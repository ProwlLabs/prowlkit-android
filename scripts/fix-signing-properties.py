#!/usr/bin/env python3
"""Write SIGNING_KEY (single-line escaped) and SIGNING_PASSWORD into publish.properties."""
from __future__ import annotations

import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PROPS = ROOT / "publish.properties"
GNUPGHOME = ROOT / ".gnupg"
EMAIL = "elmysufiandy@gmail.com"
GPG = "/opt/homebrew/bin/gpg"


def main() -> None:
    env = {"GNUPGHOME": str(GNUPGHOME)}

    list_proc = subprocess.run(
        [GPG, "--list-secret-keys", "--keyid-format=long", EMAIL],
        capture_output=True,
        text=True,
        env=env,
        check=True,
    )
    key_id = None
    for line in list_proc.stdout.splitlines():
        if line.startswith("sec"):
            key_id = line.split("/")[1].split()[0]
            break
    if not key_id:
        sys.exit(f"No secret key found for {EMAIL}. Run setup-gpg-maven.sh first.")

    if not PROPS.exists():
        sys.exit(f"Missing {PROPS}")

    text = PROPS.read_text()
    pass_match = re.search(r"^SIGNING_PASSWORD=(.+)$", text, re.M)
    if not pass_match:
        sys.exit("SIGNING_PASSWORD missing in publish.properties")
    passphrase = pass_match.group(1).strip()

    export_proc = subprocess.run(
        [
            GPG,
            "--batch",
            "--pinentry-mode",
            "loopback",
            "--passphrase",
            passphrase,
            "--armor",
            "--export-secret-keys",
            key_id,
        ],
        capture_output=True,
        text=True,
        env=env,
        check=True,
    )
    escaped = export_proc.stdout.replace("\\", "\\\\").replace("\n", "\\n")

    # Strip any multiline SIGNING_KEY block and rewrite as single property line.
    text = re.sub(r"^SIGNING_KEY=.*?(?=^[A-Z_]+=|\Z)", "", text, flags=re.M | re.S)
    if not text.endswith("\n"):
        text += "\n"
    if "GPG_KEY_ID=" not in text:
        text += f"GPG_KEY_ID={key_id}\n"
    else:
        text = re.sub(r"^GPG_KEY_ID=.*$", f"GPG_KEY_ID={key_id}", text, flags=re.M)

    if "SIGNING_KEY=" in text:
        text = re.sub(r"^SIGNING_KEY=.*$", f"SIGNING_KEY={escaped}", text, flags=re.M)
    else:
        text += f"SIGNING_KEY={escaped}\n"

    PROPS.write_text(text)
    print(f"Fixed publish.properties for key {key_id} ({EMAIL})")


if __name__ == "__main__":
    main()
