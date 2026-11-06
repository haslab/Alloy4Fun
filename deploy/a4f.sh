#!/bin/bash
# Alloy4Fun production tooling for the Meteor 3 stack (no Docker).
# Run as the unprivileged service user, from the install directory (uses sudo where needed). Commands:
#   prepare   non-disruptive: stop staging, lay out the install directory, grant port-80 capability
#   cutover   DOWNTIME: stop old stack, final dump, restore into a NEW dbpath, start new stack
#   start     start mongod + API + app (used by @reboot)
#   stop      stop the new stack
#   status    show processes, ports, doc counts
#   rollback  stop the new stack and start the OLD stack again (old source + old data untouched)
#   deploy <meteor.tar.gz> [api.jar]   routine release: dump DB, swap code, health-check, auto-revert
#   release-rollback                   go back to the previous release (code only, data is kept)
#   watchdog  restart whichever of mongod/API/app is down (run every minute from cron); no-op after 'stop'
set -eu

P=${A4F_HOME:-$(cd "$(dirname "$0")" && pwd)}   # install dir = where this script lives
H=$(dirname "$P")                                  # service user's home
OLD=${A4F_OLD:-$H/old-stack}              # retired Meteor 1.8 checkout (rollback only)
STAGE=$H/stage
TOOL=$H/.meteor/packages/meteor-tool/3.5.4/mt-os.linux.x86_64/dev_bundle
ROOT_URL=${ROOT_URL:-http://localhost}
MONGO_PORT=27017      # new mongod runs as an unprivileged user, so it cannot use port 81
APP_PORT=80
API_PORT=4001
OLD_MONGO_PORT=81
OLD_DBPATH=$OLD/meteor/.meteor/local/db
SETTINGS_FILE=$P/settings.json   # Meteor settings for production; template: deploy/settings.production.json

die() { echo "ERROR: $*" >&2; exit 1; }
listening() { ss -ltn | awk '{print $4}' | grep -qE "[:.]$1\$"; }
wait_gone() { for i in $(seq 1 40); do listening "$1" || return 0; sleep 1; done; return 1; }
counts() { mongo --quiet --port "$1" meteor --eval 'db.getMongo().setSlaveOk(); db.getCollectionNames().sort().forEach(function(c){print(c+" "+db[c].count())})' | grep -v '^system\.'; }

start_mongod() {
  $P/mongod --bind_ip 127.0.0.1 --port $MONGO_PORT --dbpath $P/db --oplogSize 8 --replSet meteor --noauth \
    --fork --logpath $P/logs/mongod.log >/dev/null 9>&-
  for i in $(seq 1 30); do mongo --quiet --port $MONGO_PORT --eval 'db.version()' >/dev/null 2>&1 && break; sleep 1; done
  mongo --quiet --port $MONGO_PORT --eval "if(!rs.status().ok){printjson(rs.initiate({_id:'meteor',members:[{_id:0,host:'127.0.0.1:$MONGO_PORT'}]}))}" >/dev/null
  for i in $(seq 1 30); do
    [ "$(mongo --quiet --port $MONGO_PORT --eval 'db.isMaster().ismaster')" = "true" ] && return 0; sleep 1
  done
  die "mongod did not become primary (see $P/logs/mongod.log)"
}

start_api() {
  (cd $P; nohup java -Dalloy4fun.mongo.port=$MONGO_PORT -Dalloy4fun.stats.maxage=600000 \
    -Dthorntail.http.port=$API_PORT -Djava.net.preferIPv4Stack=true -jar alloy4fun-api-thorntail.jar \
    >> $P/logs/api.log 2>&1 9>&- & echo $! > $P/api.pid)
}

start_app() {
  [ -f "$SETTINGS_FILE" ] || die "missing $SETTINGS_FILE (copy deploy/settings.production.json there)"
  (cd $P/bundle; MONGO_URL="mongodb://127.0.0.1:$MONGO_PORT/meteor?replicaSet=meteor" \
    MONGO_OPLOG_URL="mongodb://127.0.0.1:$MONGO_PORT/local?replicaSet=meteor" \
    ROOT_URL=$ROOT_URL PORT=$APP_PORT METEOR_SETTINGS="$(cat $SETTINGS_FILE)" \
    nohup $P/node main.js >> $P/logs/app.log 2>&1 9>&- & echo $! > $P/app.pid)
}

stop_new() {
  [ -f $P/app.pid ] && kill "$(cat $P/app.pid)" 2>/dev/null || true
  [ -f $P/api.pid ] && kill "$(cat $P/api.pid)" 2>/dev/null || true
  sleep 2
  mongo --quiet --port $MONGO_PORT admin --eval 'db.shutdownServer({force:true})' >/dev/null 2>&1 || true
  wait_gone $MONGO_PORT || die "new mongod still listening on $MONGO_PORT"
}

stop_app() {
  [ -f $P/app.pid ] && kill "$(cat $P/app.pid)" 2>/dev/null || true
  wait_gone $APP_PORT || die "app still listening on $APP_PORT"
}
stop_api() {
  [ -f $P/api.pid ] && kill "$(cat $P/api.pid)" 2>/dev/null || true
  wait_gone $API_PORT || die "API still listening on $API_PORT"
}
wait_api() {
  # a GET on a POST-only JAX-RS path answers 405 once deployed (404/000 before)
  for i in $(seq 1 120); do
    c=$(curl -s -o /dev/null -w '%{http_code}' localhost:$API_PORT/getInstances || true)
    [ "$c" != "404" ] && [ "$c" != "000" ] && return 0; sleep 1
  done
  return 1
}
app_healthy() {
  for i in $(seq 1 60); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' localhost:$APP_PORT/ || true)" = "200" ] && return 0; sleep 1
  done
  return 1
}
prune() {
  # keep the 3 newest releases and pre-deploy dumps, never the current/previous release
  keep="$(readlink $P/bundle 2>/dev/null || true) $(cat $P/previous-bundle 2>/dev/null || true)"
  for d in $(ls -dt $P/releases/*/ 2>/dev/null | tail -n +4); do
    case " $keep " in *" ${d%/}/bundle "*) ;; *) rm -rf "$d";; esac
  done
  ls -t $P/backup/pre-*.gz 2>/dev/null | tail -n +4 | xargs -r rm -f
}

# One mutating command at a time; the watchdog just skips its run if another command holds the lock.
case "${1:-}" in
status|"") ;;
*) mkdir -p $P; exec 9>$P/lock
   if [ "$1" = watchdog ]; then flock -n 9 || exit 0; else flock 9; fi ;;
esac

case "${1:-}" in
prepare)
  [ -x $STAGE/stage.sh ] && (cd $STAGE && ./stage.sh down) || true
  listening $MONGO_PORT && die "port $MONGO_PORT already in use"
  mkdir -p $P/db $P/logs $P/backup
  [ -d $P/bundle ] || cp -a $STAGE/bundle $P/bundle
  cp -n $STAGE/mongod $P/mongod
  cp -n $STAGE/alloy4fun-api-thorntail.jar $P/
  cp -f $TOOL/bin/node $P/node
  sudo setcap 'cap_net_bind_service=+ep' $P/node
  getcap $P/node
  $P/mongod --version | head -1; $P/node -v; ls $P/bundle/programs/server/node_modules/@meteorjs
  df -h $P | tail -1
  echo "prepare OK"
  ;;
cutover)
  [ -x $P/node ] && [ -x $P/mongod ] || die "run prepare first"
  [ -z "$(ls -A $P/db)" ] || die "$P/db is not empty (previous attempt?). Move it away first"
  echo "This stops the CURRENT production stack (downtime). ROOT_URL=$ROOT_URL"
  read -r -p "Type YES to continue: " a; [ "$a" = "YES" ] || exit 1

  echo "== stopping old Java API and Meteor"
  sudo pkill -TERM -f '[m]eteor run --allow-superuser' || true
  sudo pkill -TERM -f '[a]lloy4fun-api-thorntail.jar' || true
  wait_gone 80 || die "port 80 still in use"
  wait_gone 4001 || die "port 4001 still in use"
  if listening $OLD_MONGO_PORT; then
    mongo --quiet --port $OLD_MONGO_PORT admin --eval 'db.shutdownServer({force:true})' >/dev/null 2>&1 || true
    wait_gone $OLD_MONGO_PORT || die "old mongod still on $OLD_MONGO_PORT"
  fi

  echo "== opening old data read-only-by-use with the 4.0.2 mongod to take the final dump"
  sudo $P/mongod --bind_ip 127.0.0.1 --port $OLD_MONGO_PORT --dbpath $OLD_DBPATH --oplogSize 8 --replSet meteor --noauth \
    --fork --logpath $P/logs/old-mongod.log >/dev/null
  for i in $(seq 1 60); do mongo --quiet --port $OLD_MONGO_PORT --eval 'db.version()' >/dev/null 2>&1 && break; sleep 1; done
  counts $OLD_MONGO_PORT | tee $P/backup/counts-old.txt
  DUMP=$P/backup/final-$(date +%F-%H%M).gz
  mongodump --port $OLD_MONGO_PORT --db meteor --forceTableScan --gzip --archive=$DUMP
  mongo --quiet --port $OLD_MONGO_PORT admin --eval 'db.shutdownServer({force:true})' >/dev/null 2>&1 || true
  wait_gone $OLD_MONGO_PORT || die "old mongod did not stop"

  echo "== new mongod + restore"
  start_mongod
  mongorestore --port $MONGO_PORT --gzip --archive=$DUMP --nsInclude='meteor.*' --drop
  counts $MONGO_PORT > $P/backup/counts-new.txt
  diff $P/backup/counts-old.txt $P/backup/counts-new.txt || die "document counts differ after restore! Run: $0 rollback"

  echo "== starting API and app"
  start_api; start_app
  sleep 20
  code=$(curl -s -o /dev/null -w '%{http_code}' localhost:$APP_PORT/)
  echo "app http: $code"
  [ "$code" = "200" ] || die "app not healthy (tail $P/logs/app.log). Run: $0 rollback"
  echo "CUTOVER DONE. Now update the crontab (see instructions) and smoke-test with a browser."
  ;;
start)
  mkdir -p $P/logs; rm -f $P/stopped
  start_mongod; start_api
  wait_api || echo "WARNING: API did not answer in time" >&2
  start_app
  ;;
deploy)
  B=${2:?usage: a4f.sh deploy <meteor.tar.gz> [alloy4fun-api-thorntail.jar]}; J=${3:-}
  [ -f "$B" ] || die "$B not found"
  [ -z "$J" ] || [ -f "$J" ] || die "$J not found"
  listening $MONGO_PORT || die "mongod is not running ($0 start first)"
  stamp=$(date +%Y%m%d-%H%M%S); R=$P/releases/$stamp
  mkdir -p $R $P/backup
  tar -xzf "$B" -C $R
  want=$(cat $R/bundle/.node_version.txt 2>/dev/null | head -1); have=$($P/node -v)
  [ -z "$want" ] || [ "$want" = "$have" ] || die "bundle needs Node $want but $P/node is $have: replace $P/node (copy from the Meteor tool, then setcap cap_net_bind_service)"
  (cd $R/bundle/programs/server && PATH=$TOOL/bin:$PATH npm install --omit=dev --cache $P/npm-cache --no-audit --no-fund)
  [ -d $R/bundle/programs/server/node_modules/@meteorjs ] || die "npm install did not produce node_modules"

  # first deploy: turn the plain bundle directory into a symlinked release
  if [ ! -L $P/bundle ]; then
    mkdir -p $P/releases/initial && mv $P/bundle $P/releases/initial/bundle
    ln -sfn $P/releases/initial/bundle $P/bundle
  fi
  echo "== database backup"
  mongodump --port $MONGO_PORT --db meteor --forceTableScan --gzip --archive=$P/backup/pre-$stamp.gz

  readlink $P/bundle > $P/previous-bundle
  rm -f $P/previous-jar
  stop_app
  ln -sfn $R/bundle $P/bundle
  if [ -n "$J" ]; then
    stop_api
    cp $P/alloy4fun-api-thorntail.jar $P/alloy4fun-api-thorntail.jar.prev
    cp "$J" $P/alloy4fun-api-thorntail.jar
    echo 1 > $P/previous-jar
    start_api
    wait_api || echo "WARNING: API did not answer in time" >&2
  fi
  start_app
  if app_healthy; then
    echo "DEPLOY OK: $stamp (previous: $(cat $P/previous-bundle)). Now smoke-test in the browser."
    prune
  else
    echo "app not healthy (see $P/logs/app.log): reverting" >&2
    "$0" release-rollback
    exit 1
  fi
  ;;
release-rollback)
  [ -f $P/previous-bundle ] || die "no previous release recorded"
  stop_app
  ln -sfn "$(cat $P/previous-bundle)" $P/bundle
  if [ -f $P/previous-jar ] && [ -f $P/alloy4fun-api-thorntail.jar.prev ]; then
    stop_api
    cp $P/alloy4fun-api-thorntail.jar.prev $P/alloy4fun-api-thorntail.jar
    start_api; wait_api || true
  fi
  start_app
  app_healthy && echo "rolled back to $(readlink $P/bundle)" || die "app not healthy after rollback (see $P/logs/app.log)"
  ;;
stop)
  touch $P/stopped   # tells the watchdog this is intentional
  stop_new
  ;;
watchdog)
  [ -f $P/stopped ] && exit 0
  log() { echo "$(date '+%F %T') watchdog: $*" >> $P/logs/watchdog.log; }
  listening $MONGO_PORT || { log "mongod down, restarting"; start_mongod; }
  listening $API_PORT || { log "API down, restarting"; start_api; wait_api || log "API did not answer in time"; }
  listening $APP_PORT || { log "app down, restarting"; start_app; }
  ;;
status)
  ss -ltnp 2>/dev/null | grep -E ":($APP_PORT|$API_PORT|$MONGO_PORT|$OLD_MONGO_PORT)\b" || echo "nothing listening"
  echo "app http: $(curl -s -o /dev/null -w '%{http_code}' localhost:$APP_PORT/)"
  counts $MONGO_PORT 2>/dev/null || true
  ;;
rollback)
  touch $P/stopped
  echo "== stopping the new stack"
  stop_new
  sudo pkill -TERM -f "[a]4f-prod/node" 2>/dev/null || true
  wait_gone 80 || die "port 80 still in use"
  echo "== starting the OLD stack exactly as the old crontab did"
  (cd $OLD/api; sudo nohup java -Dthorntail.http.port=4001 -Djava.net.preferIPv4Stack=true -jar target/alloy4fun-api-thorntail.jar >/dev/null 2>&1 &)
  (cd $OLD/meteor; sudo nohup meteor run --allow-superuser --settings settings.json --port 80 >> /var/log/old-app.log 2>&1 &)
  echo "old stack starting (Meteor takes ~1 min). Restore the old crontab lines too."
  ;;
*)
  sed -n '2,11p' "$0"; exit 1
  ;;
esac
