#!/bin/bash
# Staging of the Meteor 3 build on the production host, on separate ports.
# Never touches ports 80/81/4001 nor the production source tree or dbpath.
# Usage: ./stage.sh setup | up | status | down
set -eu

S=$(cd "$(dirname "$0")" && pwd)   # staging dir = where this script lives
H=$(dirname "$S")
DUMP=${DUMP:-$(ls -t $H/meteor-*.gz | head -1)}
MONGO_PORT=3301
API_PORT=4101
APP_PORT=3200
METEOR_TOOL=$(ls -d $H/.meteor/packages/meteor-tool/3.5.4/mt-os.linux.x86_64/dev_bundle)
NODE=$METEOR_TOOL/bin/node
NPM=$METEOR_TOOL/bin/npm

case "${1:-}" in
setup)
  mkdir -p $S/db $S/logs
  [ -x $S/mongod ] || { echo "copy the 4.0.2 mongod first (see instructions)"; exit 1; }
  $S/mongod --version | head -1
  rm -rf $S/bundle && tar -xzf $S/meteor.tar.gz -C $S
  (cd $S/bundle/programs/server && PATH=$METEOR_TOOL/bin:$PATH $NPM install --omit=dev 2>&1 | tail -3)
  cat > $S/settings.json <<EOF
{ "env": { "API_URL": "http://localhost:$API_PORT", "MAX_INSTANCES": 10 },
  "public": { "QUESTIONNAIRE_URL": "https://example.org/questionnaire" } }
EOF
  ;;
up)
  # same flags as production's mongod: single-node replica set named "meteor"
  $S/mongod --bind_ip 127.0.0.1 --port $MONGO_PORT --dbpath $S/db --oplogSize 8 --replSet meteor --noauth \
    --fork --logpath $S/logs/mongod.log
  sleep 3
  mongo --quiet --port $MONGO_PORT --eval "rs.initiate({_id:'meteor',members:[{_id:0,host:'127.0.0.1:$MONGO_PORT'}]})" | head -3
  sleep 5
  if [ "$(mongo --quiet --port $MONGO_PORT meteor --eval 'db.Model.count()')" = "0" ]; then
    mongorestore --port $MONGO_PORT --gzip --archive="$DUMP" --nsInclude='meteor.*' --drop
  fi
  (cd $S; nohup java -Dalloy4fun.mongo.port=$MONGO_PORT -Dalloy4fun.stats.maxage=600000 \
    -Dthorntail.http.port=$API_PORT -Djava.net.preferIPv4Stack=true -jar alloy4fun-api-thorntail.jar \
    > $S/logs/api.log 2>&1 & echo $! > $S/api.pid)
  (cd $S/bundle; MONGO_URL="mongodb://127.0.0.1:$MONGO_PORT/meteor?replicaSet=meteor" \
    MONGO_OPLOG_URL="mongodb://127.0.0.1:$MONGO_PORT/local?replicaSet=meteor" \
    ROOT_URL=http://localhost:$APP_PORT PORT=$APP_PORT METEOR_SETTINGS="$(cat $S/settings.json)" \
    nohup $NODE main.js > $S/logs/app.log 2>&1 & echo $! > $S/app.pid)
  sleep 15; $0 status
  ;;
status)
  echo "app http: $(curl -s -o /dev/null -w '%{http_code}' localhost:$APP_PORT/)"
  echo "api: $(tail -1 $S/logs/api.log | cut -c1-120)"
  echo "app log:"; tail -5 $S/logs/app.log
  mongo --quiet --port $MONGO_PORT meteor --eval 'print("Model docs: "+db.Model.count())'
  ;;
down)
  kill $(cat $S/app.pid) $(cat $S/api.pid) 2>/dev/null || true
  mongo --quiet --port $MONGO_PORT admin --eval 'db.shutdownServer()' >/dev/null 2>&1 || true
  ;;
*) echo "usage: $0 setup|up|status|down"; exit 1;;
esac
