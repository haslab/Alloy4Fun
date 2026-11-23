# Alloy4Fun Quarkus API

This is the Quarkus API used by the Meteor 3 application. The existing
Thorntail API under `api/` is retained as a compatibility reference during the
2.0 migration.

Build with Java 17 or newer:

```sh
cd api-quarkus
mvn package
```

Run the uber-JAR on port 4101:

```sh
java -Dquarkus.http.port=4101 \
  -Dalloy4fun.mongo.port=3001 \
  -Dalloy4fun.mongo.db=meteor \
  -jar target/alloy4fun-api-quarkus-0.1.0-SNAPSHOT-runner.jar
```

The API reads the MongoDB connection from `alloy4fun.mongo.host`,
`alloy4fun.mongo.port`, and `alloy4fun.mongo.db` system properties, with
defaults suitable for local Meteor development. Each endpoint should pass the
API compatibility baseline in `api/e2e/` before release.
