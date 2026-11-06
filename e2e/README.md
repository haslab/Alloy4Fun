# Alloy4Fun migration safety net

Two checks that must pass before and after every migration step.

1. **DDP baseline** (`baseline/`): calls Meteor methods/publications over DDP and
   compares normalized results with `baseline/expected.json` (recorded on Meteor 2.9).
2. **Browser flows** (`tests/`): Playwright tests of the main user flows.

## Prerequisites

- Alloy API on port 4001 (`java -Dthorntail.http.port=4001 -Djava.net.preferIPv4Stack=true -jar api/target/alloy4fun-api-thorntail.jar`, Java 8).
- Meteor app on port 3000 (`cd meteor && meteor run --settings settings.json --port 3000`), with `API_URL` pointing to the API.
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
