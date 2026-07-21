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

Do not store real credentials in source control. Seed and newly created user passwords are BCrypt-hashed in storage.

## Run locally

The frontend currently calls the backend on port `8081`:

```powershell
$env:SERVER_PORT=8081
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
