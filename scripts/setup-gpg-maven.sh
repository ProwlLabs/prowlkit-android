#!/usr/bin/env bash
# Create a Maven Central–compatible GPG key (primary signing key, real email).
# Usage: ./scripts/setup-gpg-maven.sh your.email@example.com
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <your-real-email>"
  echo "Example: $0 elmee@example.com"
  echo ""
  echo "Use a real inbox — keys.openpgp.org sends a verification link."
  exit 1
fi

EMAIL="$1"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export GNUPGHOME="${ROOT}/.gnupg"
mkdir -p "${GNUPGHOME}"
chmod 700 "${GNUPGHOME}"

PASSPHRASE="ProwlKit-MC-$(openssl rand -hex 8)"
BATCH="$(mktemp)"
trap 'rm -f "${BATCH}"' EXIT

cat > "${BATCH}" <<EOF
Key-Type: RSA
Key-Length: 4096
Key-Usage: sign
Name-Real: ProwlLabs
Name-Email: ${EMAIL}
Expire-Date: 0
Passphrase: ${PASSPHRASE}
EOF

echo "Generating signing key for ${EMAIL}..."
gpg --batch --generate-key "${BATCH}"

KEY_ID="$(gpg --list-secret-keys --keyid-format=long "${EMAIL}" | awk '/^sec/ {print $2}' | cut -d'/' -f2 | head -1)"
FP="$(gpg --list-keys --with-colons "${KEY_ID}" | awk -F: '$1=="fpr" {print $10; exit}')"

echo ""
echo "Key ID:        ${KEY_ID}"
echo "Fingerprint:   ${FP}"
echo "Passphrase:    ${PASSPHRASE}"
echo ""
echo "NEXT STEPS:"
echo "  1. Check ${EMAIL} for keys.openpgp.org verification — click the link."
echo "  2. Run: ./scripts/upload-gpg-keyservers.sh ${KEY_ID}"
echo "  3. Run: ./scripts/publish-maven-central.sh"
echo ""

python3 - "${ROOT}/publish.properties" "${PASSPHRASE}" "${KEY_ID}" <<'PY'
import pathlib, re, subprocess, os, sys

props_path, passphrase, key_id = sys.argv[1:4]
os.environ["GNUPGHOME"] = str(pathlib.Path(props_path).parent / ".gnupg")

proc = subprocess.run(
    ["gpg", "--batch", "--pinentry-mode", "loopback", "--passphrase", passphrase,
     "--armor", "--export-secret-keys", key_id],
    capture_output=True, text=True, check=True,
)
escaped = proc.stdout.replace("\\", "\\\\").replace("\n", "\\n")

path = pathlib.Path(props_path)
if not path.exists():
    print(f"WARN: {path} not found — save passphrase manually.")
    sys.exit(0)

text = path.read_text()
text = re.sub(r"^SIGNING_KEY=.*$", f"SIGNING_KEY={escaped}", text, flags=re.M)
text = re.sub(r"^SIGNING_PASSWORD=.*$", f"SIGNING_PASSWORD={passphrase}", text, flags=re.M)
path.write_text(text)
print(f"Updated {path} with new SIGNING_KEY and SIGNING_PASSWORD")
PY
