# Home SCADA - Web Site

This repository contains the static web version of the Home SCADA app (the `web_site` folder from the Android project). It's designed to be hosted on GitHub Pages.

What this site includes
- Static HTML pages for SCADA, IPERL, Security, DVR and other pages under `pages/`.
- Client-side JavaScript that reads a public Google Sheet (gviz/csv) and updates the dashboard.
- A simple debug panel that helps fetch and map sheet data into the page.

How the site fetches data
- The site can fetch a public Google Sheet using the gviz/tq JSON wrapper and falls back to CSV export.
- Default Android sheet ID is preconfigured in the code; you can change or override it in the dashboard UI or localStorage.

Deploy to GitHub Pages
- There's a helper PowerShell script `deploy_create_github_repo.ps1` in this folder that will:
  - Create a new GitHub repo (timestamped) under your account using the GitHub REST API.
  - Initialize a fresh git repo in this folder (or use the existing one), commit only the site files, push to `main` and enable GitHub Pages for `/`.

Important: when running the script you will need to set the `GITHUB_PAT` environment variable in the PowerShell session with a Personal Access Token that has the `public_repo` scope (for a public repo) or `repo` (for a private repo).

Security note
- Do not paste your PAT into chat. Set it in your local PowerShell session before running the script:
  $env:GITHUB_PAT = 'YOUR_TOKEN'

If you'd like, I can run the deploy step for you — provide confirmation and instruct how you'd like to provide auth (PAT in session or gh CLI auth). Otherwise run the script locally (instructions are in the file `deploy_create_github_repo.ps1`).

