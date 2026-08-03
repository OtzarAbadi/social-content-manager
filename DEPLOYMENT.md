# SocialContent production deployment preparation

This repository is prepared for Vercel (frontend/PWA) and Railway
(Spring Boot backend and MySQL). These instructions do not deploy, create
accounts, or contain credentials.

## Vercel frontend

Create a Vercel project from the monorepo with:

- Root Directory: `Frontend`
- Framework preset: Vite
- Build command: `npm run build`
- Output directory: `dist`
- Production environment variable: `VITE_API_URL=https://<public-backend-host>`

`VITE_API_URL` is the backend origin, with no path required. A single trailing
`/api` is accepted and normalized, but other paths are rejected. Production
runtime validation rejects missing, local, private-network, or non-HTTPS
backend URLs. Never place database, Cloudinary, Meta, or authentication secrets
in a `VITE_*` variable.

`Frontend/vercel.json` serves existing static files first, returns 404 for
`/api` paths instead of proxying them, and falls back to `index.html` for React
routes such as `/analytics`, `/clients`, `/content`, and `/integrations`.

The generated service worker caches versioned frontend assets only. API,
authentication, profile, client, content, Insights, publishing, Meta, mutation,
and personal-data responses have no runtime cache route. Updates remain
user-controlled.

## Railway backend

Create a Railway service with `backend` as its root directory and build it from
`backend/Dockerfile`. The Dockerfile uses the Maven wrapper in a multi-stage
build and starts the Spring Boot jar as a non-root user. Configure the health
check path as:

```text
/api/health
```

The application binds to `0.0.0.0`. Railway's `PORT` takes precedence over the
local `SERVER_PORT`.

Required Railway variables:

```text
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
META_INSTAGRAM_USER_ID
META_PAGE_ACCESS_TOKEN
META_GRAPH_API_BASE_URL
FRONTEND_PUBLIC_URL
CORS_ALLOWED_ORIGINS
COOKIE_SECURE
COOKIE_SAME_SITE
```

Railway supplies `PORT`; do not hardcode it. Optional variables matching
existing publishing behavior are:

```text
SERVER_PORT
SSCM_PUBLISHING_PROVIDER
SSCM_PUBLISHING_SCHEDULING_ENABLED
SSCM_PUBLISHING_FIXED_DELAY_MS
SSCM_PUBLISHING_INITIAL_DELAY_MS
SSCM_PUBLISHING_BATCH_SIZE
```

Use these production settings:

```text
SPRING_PROFILES_ACTIVE=production
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
```

Set `FRONTEND_PUBLIC_URL` and `CORS_ALLOWED_ORIGINS` to the exact public HTTPS
frontend origin. `CORS_ALLOWED_ORIGINS` supports a comma-separated allowlist
when more than one exact frontend origin is intentional. Do not use `*` with
credentialed requests.

### Railway MySQL

`SPRING_DATASOURCE_URL` accepts JDBC format:

```text
jdbc:mysql://<host>:<port>/<database>?useSSL=true
```

The backend also normalizes a `mysql://<host>:<port>/<database>` scheme to
JDBC. Keep the username and password in the separate datasource variables; do
not embed credentials in the URL. Local development retains the existing local
MySQL URL and credentials from the ignored `backend/.env`.

Flyway runs the versioned scripts in `backend/src/main/resources/db/migration`
before the application accepts traffic. Existing databases are baselined at
version `0`, and migrations must remain idempotent so older Railway databases
can be upgraded safely. Hibernate schema creation remains disabled.

## Authentication and cross-domain cookies

Login now sets the existing `token` as an HttpOnly cookie. Production cookies
are `Secure` and `SameSite=None`; the production response does not serialize
the token to JavaScript. Local HTTP development uses `Secure=false` and
`SameSite=Lax`.

Vercel and Railway default domains are cross-site. Although `SameSite=None`
allows cross-site credential requests, browser third-party-cookie policies can
still block them. For reliable production authentication, use custom frontend
and backend subdomains under the same registrable domain where possible. CORS
still requires the exact frontend origin, and frontend requests retain
credentials.

## Uploads and publishing

Production image uploads require Cloudinary and return public HTTPS URLs.
`backend/uploads` is a development-only fallback and Railway's filesystem is
not durable across deployments. Do not rely on `/uploads` for production
media. Instagram publishing already rejects localhost, private-network, and
non-HTTPS image URLs.

The pilot Meta Page Access Token remains server-only in Railway variables.
There is no automatic token refresh and no Meta OAuth change in this phase.
All public users continue to interact with the same pilot Instagram connection
until multi-client OAuth is implemented.

Direct Instagram publishing and Insights remain unchanged. The generic
scheduled-publishing provider also retains its existing configured behavior;
review `SSCM_PUBLISHING_PROVIDER` before enabling production scheduling.

## Local development

Backend:

```powershell
cd backend
.\run-backend.ps1
```

Frontend:

```powershell
cd Frontend
npm.cmd install
npm.cmd run dev
```

The ignored `backend/.env` and `backend/run-backend.ps1` remain on the local
computer across normal `git pull` operations.
