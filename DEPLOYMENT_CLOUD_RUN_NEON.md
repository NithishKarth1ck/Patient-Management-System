# Free Portfolio Deployment Plan (Cloud Run + Neon)

Target branch: `deployment/cloud-run-neon`

## Target architecture

- Next.js frontend: Vercel (existing deployment)
- One Cloud Run service with three containers in one instance:
  - API Gateway: ingress container, port 4004
  - Auth Service: sidecar, port 4005
  - Patient Service: sidecar, port 4000
- Neon PostgreSQL: one hosted Postgres database, with separate schemas for auth and patient data
- Cloud Run service scales to zero when idle. A warm-up route/page will send health probes before an interview demo.

## Why one Cloud Run service with sidecars

Only the Gateway receives public ingress. The backend containers share localhost networking and are not individually exposed as public services. This avoids bypassing the API Gateway's JWT validation by calling Patient Service directly. The three containers wake together, so the warm-up procedure is simpler.

## Planned environment variables

Gateway:
- `AUTH_SERVICE_URL=http://localhost:4005`
- `PATIENT_SERVICE_URL=http://localhost:4000`

Auth Service:
- `SPRING_DATASOURCE_URL` (Neon JDBC URL)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `RECAPTCHA_SECRET_KEY`

Patient Service:
- `SPRING_DATASOURCE_URL` (Neon JDBC URL)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- optional `MAIL_USERNAME`, `MAIL_PASSWORD`, `FAST2SMS_API_KEY`

## Database isolation

Create distinct schemas (e.g. `auth` and `patient`) and configure each service's Hibernate default schema. Prefer separate database roles with only schema-specific permissions. Keep secrets out of Git and do not commit any `.env` values.

## Interview warm-up

A dedicated frontend route (planned: `/wakeup`) will poll the Cloud Run Gateway health endpoint and show progress, elapsed time, and a ready state. After it reports ready, open the existing frontend and demonstrate login and CRUD. Health endpoints should reveal only basic status.

## Deployment sequence

1. Verify GCP account and billing setup; use Cloud Run's free tier carefully and configure budgets/alerts (alerts are not hard spending caps).
2. Create Neon project/database and separate schemas/roles.
3. Add Actuator health endpoints to Auth and Patient services.
4. Configure Cloud Run sidecar service YAML, startup probes/dependencies, resources, and secrets.
5. Build/push container images to Artifact Registry and deploy.
6. Set Vercel's `NEXT_PUBLIC_API_BASE_URL` to the Cloud Run Gateway URL.
7. Add and deploy the warm-up page.
8. Test externally: registration, login, patient CRUD, persistence, cold start and warm-up timing.

## Free-tier caveats

- Cloud Run is usage-based and free-tier limits are not a hard billing cap. Outbound network, image storage/build, and other products can incur charges.
- Neon free tier has usage and storage limits; it can suspend/scale down compute when idle.
- Portfolio/demo deployment is not production SLA.
