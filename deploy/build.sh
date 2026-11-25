#!/bin/bash
# Builds the release artifacts for the production host (Linux x86_64) from the current checkout:
#   $OUT/meteor.tar.gz                 Meteor bundle (server only)
#   $OUT/alloy-web-api.jar             Generic Alloy API
#   $OUT/alloy4fun-stats-api.jar       A4F-specific stats API
# Usage: ALLOY_WEB_API_DIR=/path/to/alloy-web-api deploy/build.sh [--with-api]
# Requires the Meteor CLI and JDK 17.
set -eu
cd "$(dirname "$0")/.."
OUT=${OUT:-/tmp/a4f-release}
rm -rf "$OUT"; mkdir -p "$OUT"

git diff --quiet HEAD -- meteor api api-quarkus || { echo "WARNING: uncommitted changes in meteor/, api/, or api-quarkus/ will be built" >&2; }
echo "commit: $(git rev-parse --short HEAD)" | tee "$OUT/COMMIT"

(cd meteor && meteor npm install && meteor build "$OUT" --architecture os.linux.x86_64 --server-only)

if [ "${1:-}" = "--with-api" ]; then
  : "${ALLOY_WEB_API_DIR:?Set ALLOY_WEB_API_DIR to a clean checkout of the standalone alloy-web-api project}"
  [ -d "$ALLOY_WEB_API_DIR" ] || { echo "Alloy Web API project not found: $ALLOY_WEB_API_DIR" >&2; exit 1; }
  (cd "$ALLOY_WEB_API_DIR" && JAVA_HOME=${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17)} mvn -q clean package -DskipTests -Dquarkus.package.jar.type=uber-jar)
  (cd api-quarkus && JAVA_HOME=${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17)} mvn -q clean package -DskipTests -Dquarkus.package.jar.type=uber-jar)
  cp "$ALLOY_WEB_API_DIR"/target/alloy-web-api-*-runner.jar "$OUT/alloy-web-api.jar"
  cp api-quarkus/target/alloy4fun-stats-api-quarkus-*-runner.jar "$OUT/alloy4fun-stats-api.jar"
fi
(cd "$OUT" && shasum -a 256 * | tee SHA256SUMS)
echo "built into $OUT"
