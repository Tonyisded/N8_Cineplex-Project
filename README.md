# N8_Cineplex-Project

## Environment configuration

The backend loads `backend/.env` when commands run from `backend/`.
Keep an existing `.env`: it contains private local configuration and must never be committed.
For a new checkout, copy `backend/.env.example` to `backend/.env` only if `.env` does not exist.
Fill in the PostgreSQL URL, three distinct JWT secrets, Gmail sender/App Password,
public HTTPS API URL and an absolute writable avatar directory before starting.
The example lists all 13 currently supported application variables; Google sign-in is optional.
Blank secrets and example addresses are intentionally not usable credentials.
Do not rotate existing JWT secrets just to copy the template: tokens/quotes would be invalidated.

```powershell
cd backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Edit .env with your own configuration before running:
npm run build
npm start
```

`HOST=127.0.0.1`, `PORT=5000`, `TRUST_PROXY=false` and `CRAWLER_ENABLED=false`
are local defaults. Containers may need `HOST=0.0.0.0`; enable proxy trust only behind
one controlled reverse proxy. Leave the scheduled crawler disabled unless catalog writes are intended.
Use a public HTTPS URL ending in `/api/v1` for `PUBLIC_BASE_URL`, not localhost.
Only `backend/.env.example` is exempted from Git ignore; `.env` and other `.env.*` stay ignored.
Future secrets belong in private `.env`, with names/placeholders only in `.env.example`.
Never print secret values in logs or put them in source, Markdown, tests or APKs.
Payment-provider configuration is not implemented and is not invented here.

### Android configuration

Android does **not** read `.env`. Its existing public build settings are Gradle properties:
`cineplexApiUrl` (debug), `cineplexReleaseApiUrl` (release), and `googleWebClientId`.
Pass them with `-Pname=value` or configure your user-level `~/.gradle/gradle.properties`.
API URLs must end in `/api/v1/`; the Google WEB client ID must match backend `GOOGLE_CLIENT_ID`.
Backend secrets must stay on the server, never in `BuildConfig`, resources or an APK.
