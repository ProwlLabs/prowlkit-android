#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="${ROOT}/publish.properties"
NAMESPACE="${1:-io.github.prowllabs}"

if [[ ! -f "${PROPS}" ]]; then
  echo "Missing publish.properties"
  exit 1
fi

get_prop() {
  grep "^${1}=" "${PROPS}" | cut -d= -f2- | head -1
}

USERNAME="$(get_prop MAVEN_CENTRAL_USERNAME)"
PASSWORD="$(get_prop MAVEN_CENTRAL_PASSWORD)"

if [[ -z "${USERNAME}" || -z "${PASSWORD}" ]]; then
  echo "Set MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD in publish.properties"
  exit 1
fi

TOKEN="$(printf '%s:%s' "${USERNAME}" "${PASSWORD}" | base64 | tr -d '\n')"
AUTH="Authorization: Bearer ${TOKEN}"
API="https://ossrh-staging-api.central.sonatype.com/manual"

echo "Searching staging repositories for namespace ${NAMESPACE}..."
SEARCH="$(curl -sS -H "${AUTH}" \
  "${API}/search/repositories?ip=any&profile_id=${NAMESPACE}")"
echo "${SEARCH}" | python3 -m json.tool 2>/dev/null || echo "${SEARCH}"

echo ""
echo "Uploading staging repository to Central Portal..."
UPLOAD="$(curl -sS -w "\nHTTP_STATUS:%{http_code}" -X POST \
  -H "${AUTH}" \
  "${API}/upload/defaultRepository/${NAMESPACE}?publishing_type=user_managed")"
BODY="${UPLOAD%HTTP_STATUS:*}"
STATUS="${UPLOAD##*HTTP_STATUS:}"
echo "${BODY}" | python3 -m json.tool 2>/dev/null || echo "${BODY}"
echo "HTTP ${STATUS}"

if [[ "${STATUS}" != "200" && "${STATUS}" != "201" && "${STATUS}" != "204" ]]; then
  echo "Upload request failed. If IP mismatch, re-run ./gradlew publishAllToMavenCentral then this script immediately."
  exit 1
fi

echo ""
echo "Done. Refresh https://central.sonatype.com/publishing/deployments"
echo "Then Close → Release the deployment."
