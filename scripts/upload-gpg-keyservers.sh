#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export GNUPGHOME="${ROOT}/.gnupg"
KEY_ID="${1:-B8490A0A9D2EA45E}"

echo "Uploading GPG public key ${KEY_ID} to Sonatype-supported keyservers..."
for ks in hkps://keyserver.ubuntu.com hkps://keys.openpgp.org hkps://pgp.mit.edu; do
  echo "  → ${ks}"
  gpg --keyserver "${ks}" --send-keys "${KEY_ID}" 2>&1 || echo "    (warn: upload to ${ks} failed)"
done

FP="$(gpg --list-keys --with-colons "${KEY_ID}" 2>/dev/null | awk -F: '$1=="fpr" {print $10; exit}')"
echo ""
echo "Fingerprint: ${FP}"
echo "Verify (wait 2–10 min after first upload):"
echo "  curl -s 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x${FP}' | head -1"
echo ""
echo "Note: keys.openpgp.org requires a verified real email on the key."
echo "GitHub noreply addresses are NOT verified — use scripts/setup-gpg-maven.sh with your real email."
