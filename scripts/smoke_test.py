from __future__ import annotations

import json
import http.client
import sys
import time
import urllib.error
import urllib.request


BASE_URL = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080"
ML_URL = sys.argv[2].rstrip("/") if len(sys.argv) > 2 else "http://localhost:8000"


def request_json(method: str, url: str, payload: dict | None = None) -> dict:
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method=method,
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def wait_for_service(name: str, url: str, attempts: int = 60) -> None:
    for attempt in range(1, attempts + 1):
        try:
            request_json("GET", url)
            print(f"{name} is healthy")
            return
        except (ConnectionAbortedError, http.client.RemoteDisconnected, urllib.error.URLError, TimeoutError):
            print(f"waiting for {name} ({attempt}/{attempts})")
            time.sleep(2)
    raise RuntimeError(f"{name} did not become healthy: {url}")


def main() -> None:
    wait_for_service("backend", f"{BASE_URL}/actuator/health")
    wait_for_service("ml-service", f"{ML_URL}/health")

    username = f"smoke_user_{int(time.time())}"
    user = request_json(
        "POST",
        f"{BASE_URL}/api/users",
        {"username": username, "ageGroup": "25-34", "country": "US"},
    )
    content = request_json(
        "POST",
        f"{BASE_URL}/api/content",
        {
            "creatorId": user["id"],
            "title": "Smoke test ranking content",
            "category": "TECHNOLOGY",
            "tags": ["smoke-test", "ranking"],
            "durationSeconds": 60,
            "language": "en",
            "qualityScore": 0.9,
        },
    )
    request_json(
        "POST",
        f"{BASE_URL}/api/events",
        {
            "userId": user["id"],
            "contentId": content["id"],
            "interactionType": "LIKE",
            "watchTimeSeconds": 52,
            "deviceType": "smoke-test",
            "sessionId": f"smoke-{int(time.time())}",
        },
    )

    # Give the Kafka consumer a short window to update features in local Docker.
    time.sleep(3)
    feed = request_json("GET", f"{BASE_URL}/api/feed/{user['id']}?limit=5")
    diagnostics = request_json("GET", f"{BASE_URL}/api/platform/model")

    assert feed["userId"] == user["id"]
    assert "rankingPolicy" in feed
    assert "modelVersion" in feed
    assert "model_version" in diagnostics
    print("smoke test passed")


if __name__ == "__main__":
    main()
