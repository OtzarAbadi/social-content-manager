# SSCM Frontend

React/Vite frontend for the Social Content Manager. It provides login, Admin/Client dashboards, client and content management, comments, notifications, media previews, and monthly/weekly calendar views.

## Prerequisites

- Node.js and npm
- SSCM backend

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
