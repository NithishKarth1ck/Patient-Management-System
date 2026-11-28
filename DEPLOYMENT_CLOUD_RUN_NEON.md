# Deploy Patient Management System — Cloud Run + Neon

This guide is for the `deployment/cloud-run-neon` branch. The frontend remains on Vercel. The three Spring Boot containers run as one Cloud Run service; only the API Gateway receives public ingress.

## Current deployment layout

- Vercel: Next.js frontend
- Cloud Run service: `pms-api`, region `asia-south1`
  - `gateway`: public ingress, Cloud Run port 8080
  - `auth-service`: private sidecar, localhost port 4005
  - `patient-service`: private sidecar, localhost port 4000
- Neon: one PostgreSQL database, two schemas (`auth`, `patient`) and two database roles

The gateway's Cloud Run port is supplied by the `PORT` environment variable. Locally it defaults to 4004. Cloud Run sidecars share localhost networking. Container startup dependencies are declared in `cloud-run-service.yaml` and the sidecars expose readiness probes.

## 1. Create Neon database schemas and roles

Create a Neon project/database. In the Neon SQL Editor, using the database owner role, create two app roles and schemas. Replace the example passwords with strong unique values; do not commit them.

```sql
CREATE ROLE auth_app LOGIN PASSWORD 'REPLACE_WITH_AUTH_PASSWORD';
CREATE ROLE patient_app LOGIN PASSWORD 'REPLACE_WITH_PATIENT_PASSWORD';

CREATE SCHEMA auth AUTHORIZATION auth_app;
CREATE SCHEMA patient AUTHORIZATION patient_app;
```

Use the direct Neon connection host initially (not the pooled `-pooler` host) and require SSL. The JDBC URL format is:

```
jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
```

The Auth Service uses `DB_SCHEMA=auth`; Patient Service uses `DB_SCHEMA=patient`. Each role owns its schema, so Hibernate can create/update its own tables without owning the other service's data.

For local Docker Compose, copy `.env.example` to `.env`, set `DB_PASSWORD`, and generate/set `JWT_SECRET`. The populated `.env` is ignored by Git. If starting Auth Service directly outside Compose, set `JWT_SECRET` in the process environment first; there is intentionally no insecure default.

**Important:** Patient's existing `data.sql` targets a legacy table named `patient`, while the JPA entity maps to `patients`. SQL initialization is disabled in the deployment configuration so that this incompatible seed script is not run. The new database starts without demo patient rows; create test records through the application after deployment.

## 2. Enable Secret Manager and create secrets

Enable the Secret Manager API if it is not already enabled:

```powershell
gcloud services enable secretmanager.googleapis.com --project pms-deployment-2026
```

Create these seven secrets in Google Cloud Secret Manager in project `pms-deployment-2026`:

| Secret name | Value |
|---|---|
| `neon-jdbc-url` | Neon JDBC URL shown above |
| `auth-db-username` | `auth_app` |
| `auth-db-password` | Password chosen for `auth_app` |
| `patient-db-username` | `patient_app` |
| `patient-db-password` | Password chosen for `patient_app` |
| `jwt-secret` | Base64-encoded random secret containing at least 32 random bytes |
| `recaptcha-secret-key` | Secret key paired with the reCAPTCHA site key used by the frontend |

Generate a JWT secret locally in PowerShell (do not send it in chat or commit it):

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Copy the output directly into the `jwt-secret` secret value. Auth Service Base64-decodes this value before using it for HMAC signing; do not use an ordinary unencoded password.

The Cloud Run runtime identity needs `Secret Manager Secret Accessor` on each of these secrets. If using the project's default Compute Engine service account, find its email with:

```powershell
gcloud projects describe pms-deployment-2026 --format="value(projectNumber)"
```

Its email is `PROJECT_NUMBER-compute@developer.gserviceaccount.com`. Grant that identity `roles/secretmanager.secretAccessor` on each secret (or use a dedicated runtime service account and grant it access instead).

## 3. Build and push the three images

Prerequisites on your Windows machine:
- Docker Desktop running
- Google Cloud CLI installed and authenticated
- Access to project `pms-deployment-2026`
- Artifact Registry repository `pms-services` already created in `asia-south1`

From the repository root, check out this branch and pull the latest changes:

```powershell
git switch deployment/cloud-run-neon
git pull
gcloud config set project pms-deployment-2026
```

Build and push all three containers:

```powershell
.\build-and-push.ps1
```

The script tags images with the checked-out Git commit, pushes them to Artifact Registry, and generates `cloud-run-service.rendered.yaml`. It does not require the Cloud Build API because Docker builds locally.

## 4. Deploy the Cloud Run service

After all seven secrets exist and the Cloud Run runtime identity can access them, run:

```powershell
.\build-and-push.ps1 -Deploy
```

This builds/pushes the images and replaces the `pms-api` service in `asia-south1`. The script also grants `allUsers` the Cloud Run Invoker role so the Vercel browser frontend can call the public Gateway. Patient endpoints remain protected by the Gateway's JWT validation. The first container is the only ingress container. Auth and Patient are sidecars; they are not exposed as independent public services.

The deployment template uses 1 vCPU and 512 MiB per container, no minimum instances, and a maximum scale of two instances. The service is publicly invokable because the browser-based Vercel frontend must call it; only the Gateway is exposed, and patient operations still require JWT validation. These are starting settings for a portfolio demo, not a guarantee of zero cost. Monitor the budget and Cloud Run usage.

If the service already exists and you only need to deploy the images currently pushed, the script still rebuilds and pushes before replacing it. Check deployment status with:

```powershell
gcloud run services describe pms-api --region asia-south1 --project pms-deployment-2026
```

## 5. Configure Vercel

In the Vercel project settings, set:

- `NEXT_PUBLIC_API_BASE_URL` = the Cloud Run service URL, with no trailing slash
- `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` = the public site key paired with the Secret Manager reCAPTCHA secret

Make sure the reCAPTCHA site key allows the deployed Vercel domain. The frontend uses these values at build time, so redeploy Vercel after changing them.

The Gateway CORS configuration currently allows:
- `https://patient-management-system-frontend-blue.vercel.app`
- `http://localhost:3000`

If your Vercel deployment uses a different production domain, update the CORS origin in `api-gateway/src/main/resources/application.properties` before deploying.

## 6. Verify the deployment

Replace `CLOUD_RUN_URL` with the service URL.

1. Open `CLOUD_RUN_URL/actuator/health`; expect a basic `UP` response.
2. Register a user through the frontend (reCAPTCHA must be configured).
3. Log in and confirm the frontend receives a JWT.
4. Create a patient, refresh the page, and confirm the patient remains.
5. Update and delete a patient.
6. Check Cloud Run logs if a container fails its startup probe or database connection.

Only `/auth/login` and `/auth/register` are exposed through the public Auth route. The Auth `/validate` endpoint is called internally by the Gateway's JWT filter. Patient API requests continue to require a Bearer token.

## What is already prepared in this branch

- Gateway uses `application.properties`, honors Cloud Run's `PORT`, and routes to localhost sidecars.
- CORS is configured for the current Vercel domain and localhost development.
- Auth no longer has a hard-coded JWT signing secret fallback.
- Auth and Patient use configurable PostgreSQL schemas and Actuator readiness probes.
- Legacy Patient SQL seed initialization is disabled to avoid creating/inserting into the wrong table.
- Spring Boot versions are aligned to the 3.5 line; Gateway uses the stable Spring Cloud 2025.0.3 release train.
- Cloud Run multi-container template, secret references, startup dependencies, and Windows build/push/deploy script are included.

## Limitations and follow-up

- The code and deployment template have been prepared, but a live deployment is not complete until Neon, Secret Manager, Artifact Registry image pushes, Cloud Run, and Vercel are configured and verified.
- No automated migration tool is configured; Hibernate `ddl-auto=update` is retained for this portfolio deployment. For production, replace it with Flyway or Liquibase migrations.
- The frontend warm-up page is not included; it is optional. Cloud Run scales to zero by default, so the first request after idle may be slower.
- Cloud Run budget alerts are notifications, not spending caps. Neon and outbound network usage can also incur costs.
