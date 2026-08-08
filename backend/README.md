# SocialContent

Backend for the SocialContent Admin/Client content-management application.

## Production deployment

Railway should use `backend` as the service root and build the included
multi-stage `Dockerfile`. The production server listens on Railway's `PORT`,
binds to `0.0.0.0`, and exposes the minimal health check at `/api/health`.

Production authentication uses an HttpOnly, Secure, `SameSite=None` cookie and
exact credentialed CORS origins. Image uploads require Cloudinary because local
`uploads` storage is not durable on Railway. See `../DEPLOYMENT.md` for all
required variable names, Vercel settings, database URL format, and limitations.

## Prerequisites

- Java 17
- MySQL 8
- Node.js and npm for the frontend

## Database

The default database name is `social_content_manager`. Create and seed it with `database/init.sql`.

The production profile reads these optional environment variables:

- `SPRING_DATASOURCE_URL` (defaults to `jdbc:mysql://localhost:3306/social_content_manager`)
- `SPRING_DATASOURCE_USERNAME` (defaults to `root`)
- `SPRING_DATASOURCE_PASSWORD` (empty by default)

Image uploads use Cloudinary when all of these variables are set:

- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

When Cloudinary is not configured, the existing local `backend/uploads` storage remains active.
Existing `/uploads/...` records continue to work. Cloudinary is currently used for images;
video uploads continue to use local storage.

Instagram publishing requires:

- `META_INSTAGRAM_USER_ID`
- `META_PAGE_ACCESS_TOKEN`
- `META_GRAPH_API_BASE_URL` (preferred; defaults to `https://graph.facebook.com/v25.0`)
- `META_GRAPH_API_BASE` (backward-compatible alias, used only when `META_GRAPH_API_BASE_URL` is absent)

The access token is used only by the backend. Do not put it in frontend environment files.
Only administrators can call `POST /api/contents/{contentId}/publish/instagram`, and the
content must be approved with a public HTTPS image URL.

Instagram Insights is available to administrators at:

- `GET /api/instagram/insights/account?since=YYYY-MM-DD&until=YYYY-MM-DD&period=day`
- `GET /api/instagram/insights/media?since=YYYY-MM-DD&until=YYYY-MM-DD&mediaType=ALL&limit=25`
- `GET /api/instagram/insights/media/{mediaId}`

The Meta app/token must include `instagram_manage_insights`. After adding this permission,
regenerate the token; an older token does not gain the permission automatically. Graph API
Explorer tokens are commonly short-lived and may expire. Never commit or expose a token.

### Meta token types and current limitation

- A **Graph API Explorer user token** is a temporary developer token representing the selected
  Meta user. It is commonly short-lived and is not a stable runtime credential.
- A **Page Access Token** represents a Facebook Page and is the credential this backend currently
  expects in `META_PAGE_ACCESS_TOKEN` for Instagram publishing and Insights.
- A **long-lived token** has an extended lifetime after Meta's supported exchange flow, but it is
  not permanent and can still expire or be invalidated.
- **Future OAuth tokens** will be issued per connected user/account, stored server-side, and
  managed with account ownership and expiry metadata.

The current Page Access Token is not refreshed or exchanged automatically. Replace it when Meta
expires or invalidates it. This change prepares the backend configuration boundary for OAuth, but
adds no OAuth flow, token persistence, database change, or migration.

Account metrics are requested independently: reach, views, profile views, accounts engaged,
total interactions, and follows/unfollows. For media, `views` is requested independently so an
unsupported metric in the common batch cannot suppress it. Images, feed videos, Reels, and
carousels request views, reach, saves, shares, total interactions, likes, and comments. Reels
additionally request average watch time and total video watch time. Meta availability varies by
media type, account, API version, time range, and permission; unavailable values are returned as
`null`, never fabricated as zero.

Common errors:

- `MISSING_PERMISSION`: add `instagram_manage_insights`, regenerate the token, and restart.
- `TOKEN_INVALID`: regenerate an expired or invalid Meta token.
- `RATE_LIMIT`: wait before manually retrying.
- `INVALID_DATE_RANGE`: use an ordered range no longer than 93 days.
- `META_TEMPORARY`: retry later; credentials are never included in the response.

Do not store real credentials in source control. Seed and newly created user passwords are BCrypt-hashed in storage.

## Run locally

Copy the committed examples to ignored local files:

```powershell
cd backend
Copy-Item .env.example .env
Copy-Item run-backend.example.ps1 run-backend.ps1
```

Fill in `backend/.env`, then run:

```powershell
.\run-backend.ps1
```

The backend loads `.env` only from the filesystem; it is not packaged into the jar. Operating
system and deployment environment variables override matching `.env` entries, and a missing
`.env` is allowed. Leave all three Cloudinary values empty to retain local image storage.
Startup diagnostics report only yes/no configuration presence.

To run without the copied helper, quote both Maven arguments in PowerShell:

```powershell
cd backend
& .\mvnw.cmd "-Dspring-boot.run.profiles=production" "spring-boot:run"
```

The frontend currently calls the backend on port `8081`.

Run the frontend on Vite's default port `5173`:

```powershell
cd ..\Frontend
npm.cmd install
npm.cmd run dev
```

Demo users created by `database/init.sql`:

- Admin: `admin` / `123456`
- Client: `client1` / `123456`
- Client: `client2` / `123456`

These credentials are for local demonstration only.

## Current main features

- Admin and Client access control
- Client and content management
- Image/video upload and preview
- Scheduling with monthly/weekly drag-and-drop calendar
- Approval, rejection, resubmission, and publishing-status workflow
- Content comments and in-app notifications
- Backend Bean Validation and BCrypt password storage

## Tests

```powershell
.\mvnw.cmd test
```

On some Windows environments the wrapper bootstrap may fail with `Cannot start maven from wrapper`. Retry the command, use `mvn test` when Maven is installed, or run `mvn.cmd test` from the Maven distribution already installed under `%USERPROFILE%\.m2\wrapper\dists`.
