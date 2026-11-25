# Alloy4Fun migration safety net

Two checks that must pass before and after every migration step.

1. **DDP baseline** (`baseline/`): calls Meteor methods/publications over DDP and
   compares normalized results with `baseline/expected.json` (recorded on Meteor 2.9).
2. **Browser flows** (`tests/`): Playwright tests of the main user flows.

## Prerequisites

- Alloy Web API on port 4001 (from a separate checkout of `alloy-web-api`, run `mvn package`, then its Quarkus runner JAR).
- Alloy4Fun Stats API on port 4002 (`cd api-quarkus && mvn package`, then run its Quarkus runner JAR).
- Meteor app on port 3000 (`cd meteor && meteor run --settings settings.json --port 3000`), with `API_URL` and `STATS_API_URL` pointing to those services.
- Set `ALLOY_WEB_API_DIR` to the path of the standalone API checkout when building the combined release bundle with `deploy/build.sh --with-api`.
- `cd e2e && npm install`. Tests use the locally installed Google Chrome
  (`E2E_CHANNEL=chromium` to use Playwright's bundled browser instead).

## Commands

```
npm run baseline:compare   # compare against expected.json (do not re-record after upgrading)
npm test                   # browser flows
```

Overrides: `E2E_BASE_URL`, and for the baseline `E2E_DDP_URL`, `E2E_MONGO_URL`, `E2E_MONGO_DB` (see `baseline/run.mjs`).
`npm run baseline:record` rewrites `expected.json`; only do so on the original Meteor 2.9 app.

The comparison masks plain `YYYY-MM-DD` day buckets and sorts arrays of named entries (e.g. stats scalars), because the API does not guarantee their order (`canonicalize` in `lib/normalize.mjs`).

Known: the app logs a Bootstrap/jQuery version `pageerror`; the flows whitelist it.
