# Social Content Manager

Backend for the SSCM Admin/Client content-management application.

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
- `META_GRAPH_API_BASE_URL` (optional; defaults to `https://graph.facebook.com/v25.0`)

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

Account metrics are requested independently: reach, views, profile views, accounts engaged,
total interactions, and follows/unfollows. Media metrics are also isolated so an unsupported
metric does not fail the page. Images, videos, Reels, and carousels request reach, views,
saves, shares, total interactions, likes, and comments. Videos and Reels additionally request
average watch time, total video watch time, follows, and profile activity. Meta availability
varies by media type, account, API version, time range, and permission; unavailable values are
returned as `null`, never fabricated as zero.

Common errors:

- `MISSING_PERMISSION`: add `instagram_manage_insights`, regenerate the token, and restart.
- `TOKEN_INVALID`: regenerate an expired or invalid Meta token.
- `RATE_LIMIT`: wait before manually retrying.
- `INVALID_DATE_RANGE`: use an ordered range no longer than 93 days.
- `META_TEMPORARY`: retry later; credentials are never included in the response.

Do not store real credentials in source control. Seed and newly created user passwords are BCrypt-hashed in storage.

## Run locally

The frontend currently calls the backend on port `8081`:

```powershell
$env:CLOUDINARY_CLOUD_NAME='your-cloud-name'
$env:CLOUDINARY_API_KEY='your-api-key'
$env:CLOUDINARY_API_SECRET='your-api-secret'
$env:META_INSTAGRAM_USER_ID='your-instagram-user-id'
$env:META_PAGE_ACCESS_TOKEN='your-page-access-token'
$env:META_GRAPH_API_BASE_URL='https://graph.facebook.com/v25.0'
$env:SERVER_PORT=8081
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=production
```

For local-only uploads without Cloudinary, remove the Cloudinary variables from the current
PowerShell session before starting:

```powershell
Remove-Item Env:CLOUDINARY_CLOUD_NAME -ErrorAction SilentlyContinue
Remove-Item Env:CLOUDINARY_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:CLOUDINARY_API_SECRET -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=production
```

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
