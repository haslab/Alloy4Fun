# Production deployment (no Docker)

Used for the 2026-11-06 move from Meteor 1.8 (`meteor run`, bundled Mongo 4.0.2 on :81) to the Meteor 3.5.4 bundle.

- `stage.sh`: staging on other ports (3200/3301/4101) with a restored dump.
- `a4f.sh`: `prepare | cutover | start | stop | status | rollback`. Everything lives in `/home/svcuser/a4f-prod`.

## Layout
- Mongo: the Meteor 1.8 tool's `mongod` 4.0.2 (do not use a newer one on old data files), single-node replica set `meteor`, port 27017, new dbpath `a4f-prod/db`.
- API: `-Dalloy4fun.mongo.port=27017 -Dalloy4fun.stats.maxage=600000` (defaults: localhost:3001, db meteor, 1 day).
- App: built with `meteor build --architecture os.linux.x86_64 --server-only`, run with Meteor's Node 24 (`cap_net_bind_service` for port 80), `MONGO_URL=...?replicaSet=meteor`.
- Settings: `env.API_URL`, `env.MAX_INSTANCES`, `public.QUESTIONNAIRE_URL`.
- Boot: root crontab `@reboot sudo -u svcuser .../a4f.sh start`.

## Gotchas
- Old `mongodump` needs `--forceTableScan` against Mongo 4.0.
- `~/.npm` may contain root-owned files: use `npm install --omit=dev --cache <dir>`.
- Never run two `mongod` on the same dbpath. Rolling back loses data written after the cutover.
