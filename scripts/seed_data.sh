#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python - "$BASE_URL" "$ROOT_DIR" <<'PY'
import json
import sys
import time
import urllib.request

base_url = sys.argv[1].rstrip("/")
root = sys.argv[2]


def post(path: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


for name, path in [
    ("users", "/api/users"),
    ("content", "/api/content"),
    ("events", "/api/events"),
]:
    with open(f"{root}/data/sample_{name}.json", encoding="utf-8") as handle:
        records = json.load(handle)
    for record in records:
        try:
            created = post(path, record)
            print(f"created {name}: {created.get('id', created)}")
            time.sleep(0.15)
        except Exception as exc:
            print(f"skipped {name}: {exc}")

print("seed complete")
PY
