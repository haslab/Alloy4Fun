#!/bin/bash
# Builds the release artifacts for the production host (Linux x86_64) from the current checkout:
#   $OUT/meteor.tar.gz                 Meteor bundle (server only)
#   $OUT/alloy4fun-api-thorntail.jar   Java API (only needed when api/ changed)
# Usage: deploy/build.sh [--with-api]
# Requires the Meteor CLI; for the API a JDK 17 (Java 8 JREs cannot compile).
set -eu
cd "$(dirname "$0")/.."
OUT=${OUT:-/tmp/a4f-release}
rm -rf "$OUT"; mkdir -p "$OUT"

git diff --quiet HEAD -- meteor api || { echo "WARNING: uncommitted changes in meteor/ or api/ will be built" >&2; }
echo "commit: $(git rev-parse --short HEAD)" | tee "$OUT/COMMIT"

(cd meteor && meteor npm install && meteor build "$OUT" --architecture os.linux.x86_64 --server-only)

if [ "${1:-}" = "--with-api" ]; then
  (cd api && JAVA_HOME=${JAVA_HOME_17:-$(/usr/libexec/java_home -v 17)} mvn -q clean package -DskipTests)
  cp api/target/alloy4fun-api-thorntail.jar "$OUT/"
fi
(cd "$OUT" && shasum -a 256 * | tee SHA256SUMS)
echo "built into $OUT"
