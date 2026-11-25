# Alloy4Fun Stats API

This Quarkus service contains Alloy4Fun's `/getStats` endpoint. The generic
Alloy endpoints (`/validate`, `/getInstances`, `/getProjection`) live in the
independent `alloy-web-api` project/repository. Meteor uses both services.

Build with Java 17 or newer:

```sh
cd api-quarkus
mvn package
```

Run the uber-JAR on port 4002:

```sh
java -Dquarkus.http.port=4002 \
  -Dalloy4fun.mongo.port=3001 \
  -Dalloy4fun.mongo.db=meteor \
  -jar target/alloy4fun-stats-api-quarkus-0.1.0-SNAPSHOT-runner.jar
```

The service reads the MongoDB connection from `alloy4fun.mongo.host`,
`alloy4fun.mongo.port`, and `alloy4fun.mongo.db` system properties, with
defaults suitable for local Meteor development. Each endpoint should pass the
API compatibility tests in the standalone Alloy Web API project and A4F's
integration tests should pass before release. The legacy Thorntail API under
`api/` is not part of the runtime stack.
