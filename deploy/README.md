# Releasing Alloy4Fun (no Docker)

Production runs three processes as user `mcunha` from `/home/mcunha/a4f-prod`, started at boot by root's crontab
(`@reboot sudo -u mcunha /home/mcunha/a4f-prod/a4f.sh start`):

| Process | What | Port |
|---|---|---|
| MongoDB 4.0.2 | single-node replica set `meteor`, data in `a4f-prod/db` | 27017 (localhost) |
| Java API | Thorntail jar, Java 11 | 4001 |
| Meteor app | built bundle on Meteor's Node 24 | 80 |

Releases live in `a4f-prod/releases/<timestamp>/bundle`; `a4f-prod/bundle` is a symlink to the active one.
A code release never touches the database files.

## 1. Develop and validate locally (every change)

1. Branch from `study`, commit with explicit `git add <paths>` (this repo's folders contain many untracked data exports that must never be committed).
2. Start the stack (the API only runs on Java 8; macOS `java_home -v 8` does not see the Oracle plugin JRE, so use its full path):
   ```
   cd api && "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java" \
     -Dthorntail.http.port=4001 -Djava.net.preferIPv4Stack=true -jar target/alloy4fun-api-thorntail.jar
   cd meteor && meteor npm install && meteor run --settings settings.json --port 3000
   ```
   `settings.json` needs `env.API_URL=http://localhost:4001`. Keep local edits of that file out of commits.
3. Automated gates, all must pass (see [e2e/README.md](../e2e/README.md)):
   ```
   cd e2e && npm install
   npm run baseline:compare           # 33 DDP scenarios
   npx playwright test                # 11 browser flows
   cd ../meteor && TEST_CLIENT=0 npm test    # server unit tests
   ```
   Do not re-record `e2e/baseline/expected.json` to make a failure go away. Re-record only for an intended behaviour change, in the same PR, explained.
4. Manual check of what you touched (editor, share model/instance, secrets, stats, study timeout/questionnaire message). When the change affects data handling, also test against a copy of production data (section 4).
5. Open a PR to `study`, with the results of the gates. Merge after review.

## 2. Build the release

From the commit you will deploy (clean checkout of `study`):
```
deploy/build.sh              # Meteor bundle only
deploy/build.sh --with-api   # also rebuild the Java API (JDK 17); only when api/ changed
```
This writes `/tmp/a4f-release/meteor.tar.gz` (+ jar, `COMMIT`, `SHA256SUMS`). Build on your machine: the production host has 2 GB RAM and
should not run `meteor build`. Note that if `meteor/.meteor/release` changes (new Meteor version), the bundle needs that version's Node:
`a4f.sh deploy` refuses to continue if `a4f-prod/node` differs from the bundle's Node version.
To update it: copy `<meteor-tool>/dev_bundle/bin/node` to `a4f-prod/node` and run `sudo setcap cap_net_bind_service=+ep a4f-prod/node`.

## 3. Deploy

```
scp /tmp/a4f-release/{meteor.tar.gz,alloy4fun-api-thorntail.jar} mcunha@<host>:/home/mcunha/
ssh mcunha@<host>
/home/mcunha/a4f-prod/a4f.sh deploy /home/mcunha/meteor.tar.gz [/home/mcunha/alloy4fun-api-thorntail.jar]
```
`deploy` does, in order: unpack and `npm install` the new bundle (errors are shown), check the Node version, **dump the database**
to `a4f-prod/backup/pre-<timestamp>.gz`, stop the app, switch the symlink, optionally swap the jar (restarting the API),
start the app and wait for HTTP 200. If the app is not healthy it reverts by itself. Expect about a minute of downtime.
It keeps the last 3 releases and dumps.

Afterwards, in a browser: open a real link, a stats page, run a model, and watch `tail -f /home/mcunha/a4f-prod/logs/app.log`
for `Exception` lines. Do not run the Playwright suite against production (it writes data).

Schedule releases outside study sessions. Open sessions in the browser reconnect on their own, but running models are interrupted.

## 4. Rollback

Code only (data is kept, this is the normal case):
```
/home/mcunha/a4f-prod/a4f.sh release-rollback
```
Switches back to the previous bundle (and previous jar if the last deploy replaced it).
Only one step back is kept. Older releases are in `a4f-prod/releases/`; to go further, `ln -sfn <release>/bundle a4f-prod/bundle` and restart.

Data (only if a release corrupted data): stop the stack (`a4f.sh stop`), then restore a pre-deploy dump into an empty directory:
move `a4f-prod/db` aside, `a4f.sh start`, `mongorestore --port 27017 --gzip --archive=a4f-prod/backup/pre-<ts>.gz --drop`.
Anything written after the dump is lost.

## 5. Testing against a copy of production data (optional, for risky changes)

1. `mongodump --port 27017 --db meteor --forceTableScan --gzip --archive=~/meteor-$(date +%F).gz` (read-only; the production tools are old, `--forceTableScan` is required).
2. Copy the dump and the release files to a scratch directory and use `deploy/stage.sh` (ports 3200 / 3301 / 4101, never touches 80 / 27017 / 4001).
   It needs the 4.0.2 `mongod` copied next to it, and `npm install` with `--cache <dir>` because `~/.npm` may contain root-owned files.
3. Reach it with `ssh -L 3200:localhost:3200 mcunha@<host>`. Production data contains study participants' work:
   keep dumps off shared machines and delete copies when done.

## Where each setting lives

Settings reach three different programs through three different mechanisms. In production all of them are set in `a4f-prod/a4f.sh`
(the top variables and the `start` function); `settings.json` in the repo is only for local development and is not used on the server.

| Setting | Read by | Mechanism | Local dev | Production (`a4f.sh`) |
|---|---|---|---|---|
| `API_URL`, `MAX_INSTANCES` | Meteor server | `env.*` in the Meteor settings JSON | `meteor/settings.json` | `SETTINGS_JSON` -> `METEOR_SETTINGS` |
| `QUESTIONNAIRE_URL` | Meteor client | `public.*` in the same JSON (sent to browsers) | optional in `settings.json` (has a default) | `SETTINGS_JSON` |
| `MONGO_URL`, `MONGO_OPLOG_URL`, `PORT`, `ROOT_URL` | Meteor server | environment variables | set by `meteor run` (Mongo on port+1) | `start` |
| Mongo host/port/db, stats max age, HTTP port | Java API | `-Dalloy4fun.*` / `-Dthorntail.http.port` JVM flags | defaults (localhost:3001, db `meteor`, 1 day, port from the command) | `start` |

So: to change the study questionnaire or the number of instances, edit `SETTINGS_JSON` in `a4f.sh`; to change ports or Mongo location,
edit the variables at the top of `a4f.sh`. Then `a4f.sh stop && a4f.sh start`.

## Facts and gotchas

- MongoDB 4.0.2 binary: `a4f-prod/mongod`, taken from the Meteor 1.8.0_1 tool. Do not start a newer `mongod` on these data files (it would upgrade them irreversibly); `/usr/bin/mongod` is 3.6.8 and too old.
- API options (defaults in parentheses): `-Dalloy4fun.mongo.host` (localhost), `.mongo.port` (3001; production uses 27017), `.mongo.db` (meteor), `.stats.maxage` ms (1 day; production 600000).
- Settings in `a4f.sh` (`SETTINGS_JSON`): `env.API_URL`, `env.MAX_INSTANCES`, `public.QUESTIONNAIRE_URL`. Change them there; they are passed as `METEOR_SETTINGS`.
- Crash recovery: nothing restarts the stack after a crash, only a reboot does (`a4f.sh start`). Check `a4f.sh status`.
- Retired setup (Meteor 1.8 `meteor run` as root with its own Mongo on :81) is kept in `~/A4FStudy/Alloy4Fun`
  as a last-resort rollback of the 2026-11-06 migration (`a4f.sh rollback`, loses data written since).
- Logs: `a4f-prod/logs/{app,api,mongod,boot}.log`. They are not rotated.
