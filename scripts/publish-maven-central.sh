#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export GNUPGHOME="${ROOT}/.gnupg"

if [[ ! -f "${ROOT}/publish.properties" ]]; then
  echo "Missing publish.properties — copy publish.properties.example and fill credentials."
  exit 1
fi

if ! grep -q '^MAVEN_CENTRAL_USERNAME=.\+' "${ROOT}/publish.properties"; then
  echo "Set MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD in publish.properties"
  exit 1
fi

cd "${ROOT}"

KEY_ID="$(gpg --list-secret-keys --keyid-format=long 2>/dev/null | awk '/^sec/ {print $2}' | cut -d'/' -f2 | head -1 || true)"
if [[ -n "${KEY_ID}" ]]; then
  "${ROOT}/scripts/upload-gpg-keyservers.sh" "${KEY_ID}"
  echo ""
  echo "Waiting 30s for keyserver propagation..."
  sleep 30
fi

./gradlew publishAllToMavenCentral --no-daemon "$@"
"${ROOT}/scripts/upload-staging-to-portal.sh"

echo ""
echo "Open https://central.sonatype.com/publishing/deployments"
echo "Drop any FAILED deployment first, then Close → Release the new one."
