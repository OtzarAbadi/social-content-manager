# SSCM Frontend

React/Vite frontend for the Social Content Manager. It provides login, Admin/Client dashboards, client and content management, comments, notifications, media previews, and monthly/weekly calendar views.

## Prerequisites

- Node.js and npm
- SSCM backend running at `http://localhost:8081`

## Run locally

```powershell
npm.cmd install
npm.cmd run dev
```

Vite serves the frontend on `http://localhost:5173` by default.

## Build

```powershell
npm.cmd run build
```

The production files are written to `dist/`.

Authentication uses the existing backend token response and browser cookie behavior. Do not place database credentials or other secrets in frontend files.
