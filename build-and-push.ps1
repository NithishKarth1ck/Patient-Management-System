param(
    [string]$ProjectId = "pms-deployment-2026",
    [string]$Region = "asia-south1",
    [string]$Repository = "pms-services",
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE"
    }
}

$Tag = (git rev-parse --short HEAD).Trim()
if (-not $Tag) { throw "Run this script from a Git repository with a commit checked out." }

$Registry = "$Region-docker.pkg.dev/$ProjectId/$Repository"

Write-Host "Using project: $ProjectId"
Write-Host "Using image tag: $Tag"
Write-Host "Configuring Docker authentication..."
Invoke-Checked "gcloud" @("auth", "configure-docker", "$Region-docker.pkg.dev", "--quiet")

$Images = @(
    @{ Name = "api-gateway"; Context = "./api-gateway" },
    @{ Name = "auth-service"; Context = "./auth-service" },
    @{ Name = "patient-service"; Context = "./patient-service" }
)

foreach ($Item in $Images) {
    $Image = "$Registry/$($Item.Name):$Tag"
    Write-Host "Building $Image"
    Invoke-Checked "docker" @("build", "--pull", "-t", $Image, $Item.Context)
    Write-Host "Pushing $Image"
    Invoke-Checked "docker" @("push", $Image)
}

$Rendered = (Get-Content "./cloud-run-service.yaml" -Raw).Replace("PROJECT_ID", $ProjectId).Replace("IMAGE_TAG", $Tag)
Set-Content -Path "./cloud-run-service.rendered.yaml" -Value $Rendered -Encoding utf8

Write-Host ""
Write-Host "Images pushed successfully:"
foreach ($Item in $Images) {
    Write-Host "  $Registry/$($Item.Name):$Tag"
}
Write-Host "Rendered service file: cloud-run-service.rendered.yaml"

if ($Deploy) {
    Write-Host "Deploying Cloud Run service..."
    Invoke-Checked "gcloud" @("run", "services", "replace", "./cloud-run-service.rendered.yaml", "--region", $Region, "--project", $ProjectId)
    Invoke-Checked "gcloud" @("run", "services", "describe", "pms-api", "--region", $Region, "--project", $ProjectId, "--format", "value(status.url)")
}
else {
    Write-Host "Images are pushed. After creating all Secret Manager secrets, run:"
    Write-Host "  .\build-and-push.ps1 -ProjectId $ProjectId -Region $Region -Deploy"
}
