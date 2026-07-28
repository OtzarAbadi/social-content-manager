# SocialContent Frontend

React/Vite frontend for the Social Content Manager. It provides login, Admin/Client dashboards, client and content management, comments, notifications, media previews, and monthly/weekly calendar views.

## Prerequisites

- Node.js and npm
- SocialContent backend

## Run locally

```powershell
npm.cmd install
npm.cmd run dev
```

Vite serves the frontend on `http://localhost:5173` by default.

The checked-in `.env` configures the backend origin:

- Opening `http://localhost:5173` uses `http://localhost:8081/api`.
- Opening `http://192.168.1.139:5173` from a device on the same Wi-Fi uses
  `http://192.168.1.139:8081/api`.

`VITE_API_URL` is an optional, highest-priority override. To use a different
backend address, copy `.env.example` to `.env` and set:

```dotenv
VITE_API_URL=http://localhost:8081
```

Restart the Vite development server after adding or changing the override.

## Build

```powershell
npm.cmd run build
```

The production files are written to `dist/`.

Authentication uses the existing backend token response and browser cookie behavior. Do not place database credentials or other secrets in frontend files.

## Progressive Web App

The production build is installable as **SocialContent**.
Supporting browsers show an in-app installation prompt. On iPhone/iPad, Safari
users receive instructions to use **Share > Add to Home Screen**. Dismissing the
prompt is remembered on that device.

Service-worker updates are never applied silently. When a new build is ready,
the app shows `גרסה חדשה זמינה`; select `עדכון עכשיו` to activate and reload it.

Only generated frontend assets (HTML, JavaScript, CSS, fonts and public images)
are precached. API, login, profiles, clients, content, Insights, Meta publishing,
mutations and other personal data are not cached. SocialContent does not provide a full
offline workflow: analytics and publishing require a network connection, and
the UI reports when the device is offline.

The service worker supplies `index.html` as the navigation fallback for React
routes. The production web server must also route unknown non-API paths such as
`/content/123` to `index.html`, because a first visit may happen before the
service worker is installed. Never rewrite `/api/*` to the frontend.

For production, set `VITE_API_URL` to the public HTTPS backend origin before
building:

```dotenv
VITE_API_URL=https://api.example.com
```

Localhost or LAN HTTP origins remain suitable for development only. No local
machine IP address is embedded in the production source.

## Vercel

Use `Frontend` as the Vercel Root Directory, `npm run build` as the build
command, and `dist` as the output directory. `vercel.json` preserves React deep
links without proxying `/api`. See the repository-level `DEPLOYMENT.md` for the
full production checklist.
