#!/bin/bash
# Alloy4Fun production tooling for the Meteor 3 stack (no Docker).
# Run as user svcuser (uses sudo where needed). Commands:
#   prepare   non-disruptive: stop staging, lay out /home/svcuser/a4f-prod, grant port-80 capability
#   cutover   DOWNTIME: stop old stack, final dump, restore into a NEW dbpath, start new stack
#   start     start mongod + API + app (used by @reboot)
#   stop      stop the new stack
#   status    show processes, ports, doc counts
#   rollback  stop the new stack and start the OLD stack again (old source + old data untouched)
set -eu

P=/home/svcuser/a4f-prod
OLD=/home/svcuser/old-stack
STAGE=/home/svcuser/stage
TOOL=/home/svcuser/.meteor/packages/meteor-tool/3.5.4/mt-os.linux.x86_64/dev_bundle
ROOT_URL=${ROOT_URL:-http://localhost}
MONGO_PORT=27017      # new mongod runs as svcuser, so it cannot use port 81
APP_PORT=80
API_PORT=4001
OLD_MONGO_PORT=81
OLD_DBPATH=$OLD/meteor/.meteor/local/db
SETTINGS_JSON='{ "env": { "API_URL": "http://localhost:4001", "MAX_INSTANCES": 10 }, "public": { "QUESTIONNAIRE_URL": "https://example.org/questionnaire" } }'

die() { echo "ERROR: $*" >&2; exit 1; }
listening() { ss -ltn | awk '{print $4}' | grep -qE "[:.]$1\$"; }
wait_gone() { for i in $(seq 1 40); do listening "$1" || return 0; sleep 1; done; return 1; }
counts() { mongo --quiet --port "$1" meteor --eval 'db.getMongo().setSlaveOk(); db.getCollectionNames().sort().forEach(function(c){print(c+" "+db[c].count())})' | grep -v '^system\.'; }

start_mongod() {
  $P/mongod --bind_ip 127.0.0.1 --port $MONGO_PORT --dbpath $P/db --oplogSize 8 --replSet meteor --noauth \
    --fork --logpath $P/logs/mongod.log >/dev/null
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
    >> $P/logs/api.log 2>&1 & echo $! > $P/api.pid)
}

start_app() {
  (cd $P/bundle; MONGO_URL="mongodb://127.0.0.1:$MONGO_PORT/meteor?replicaSet=meteor" \
    MONGO_OPLOG_URL="mongodb://127.0.0.1:$MONGO_PORT/local?replicaSet=meteor" \
    ROOT_URL=$ROOT_URL PORT=$APP_PORT METEOR_SETTINGS="$SETTINGS_JSON" \
    nohup $P/node main.js >> $P/logs/app.log 2>&1 & echo $! > $P/app.pid)
}

stop_new() {
  [ -f $P/app.pid ] && kill "$(cat $P/app.pid)" 2>/dev/null || true
  [ -f $P/api.pid ] && kill "$(cat $P/api.pid)" 2>/dev/null || true
  sleep 2
  mongo --quiet --port $MONGO_PORT admin --eval 'db.shutdownServer({force:true})' >/dev/null 2>&1 || true
  wait_gone $MONGO_PORT || die "new mongod still listening on $MONGO_PORT"
}

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
  mkdir -p $P/logs
  start_mongod; start_api
  # a GET on a POST-only JAX-RS path answers 405 once deployed (404/000 before)
  for i in $(seq 1 120); do
    c=$(curl -s -o /dev/null -w '%{http_code}' localhost:$API_PORT/getInstances || true)
    [ "$c" != "404" ] && [ "$c" != "000" ] && break; sleep 1
  done
  start_app
  ;;
stop)
  stop_new
  ;;
status)
  ss -ltnp 2>/dev/null | grep -E ":($APP_PORT|$API_PORT|$MONGO_PORT|$OLD_MONGO_PORT)\b" || echo "nothing listening"
  echo "app http: $(curl -s -o /dev/null -w '%{http_code}' localhost:$APP_PORT/)"
  counts $MONGO_PORT 2>/dev/null || true
  ;;
rollback)
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
  sed -n '2,9p' "$0"; exit 1
  ;;
esac
