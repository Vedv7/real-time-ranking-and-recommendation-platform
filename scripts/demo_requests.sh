#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Create user"
curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","ageGroup":"25-34","country":"US"}'
echo

echo "Create content"
curl -s -X POST "$BASE_URL/api/content" \
  -H "Content-Type: application/json" \
  -d '{"creatorId":1,"title":"Real-time feature stores explained","category":"TECHNOLOGY","tags":["features","ml"],"durationSeconds":80,"language":"en","qualityScore":0.91}'
echo

echo "Record event"
curl -s -X POST "$BASE_URL/api/events" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"contentId":1,"interactionType":"LIKE","watchTimeSeconds":68,"deviceType":"web","sessionId":"demo-session"}'
echo

echo "Fetch feed"
curl -s "$BASE_URL/api/feed/1?limit=5"
echo
