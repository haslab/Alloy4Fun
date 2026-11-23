#!/bin/bash
# Builds the release artifacts for the production host (Linux x86_64) from the current checkout:
#   $OUT/meteor.tar.gz                 Meteor bundle (server only)
#   $OUT/alloy4fun-api-quarkus.jar     Java API (only needed when api-quarkus/ changed)
# Usage: deploy/build.sh [--with-api]
# Requires the Meteor CLI and JDK 17.
set -eu
cd "$(dirname "$0")/.."
OUT=${OUT:-/tmp/a4f-release}
rm -rf "$OUT"; mkdir -p "$OUT"

git diff --quiet HEAD -- meteor api api-quarkus || { echo "WARNING: uncommitted changes in meteor/, api/, or api-quarkus/ will be built" >&2; }
echo "commit: $(git rev-parse --short HEAD)" | tee "$OUT/COMMIT"

(cd meteor && meteor npm install && meteor build "$OUT" --architecture os.linux.x86_64 --server-only)

if [ "${1:-}" = "--with-api" ]; then
  (cd api-quarkus && JAVA_HOME=${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17)} mvn -q clean package -DskipTests -Dquarkus.package.jar.type=uber-jar)
  cp api-quarkus/target/alloy4fun-api-quarkus-*-runner.jar "$OUT/alloy4fun-api-quarkus.jar"
fi
(cd "$OUT" && shasum -a 256 * | tee SHA256SUMS)
echo "built into $OUT"
