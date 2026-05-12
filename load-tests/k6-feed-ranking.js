import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 10 },
    { duration: "1m", target: 25 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<500", "p(99)<1000"],
    http_req_failed: ["rate<0.02"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const EVENTS = ["VIEW", "LIKE", "CLICK", "SKIP", "SAVE", "SHARE", "WATCH_TIME"];

export default function () {
  const userId = 1 + Math.floor(Math.random() * 3);
  const contentId = 1 + Math.floor(Math.random() * 5);
  const interactionType = EVENTS[Math.floor(Math.random() * EVENTS.length)];

  const eventResponse = http.post(
    `${BASE_URL}/api/events`,
    JSON.stringify({
      userId,
      contentId,
      interactionType,
      watchTimeSeconds: Math.floor(Math.random() * 90),
      deviceType: "k6",
      sessionId: `load-test-${__VU}-${__ITER}`,
    }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(eventResponse, {
    "event accepted": (response) => response.status === 201,
  });

  const feedResponse = http.get(`${BASE_URL}/api/feed/${userId}?limit=10`);
  check(feedResponse, {
    "feed returned": (response) => response.status === 200,
    "feed has ranked items": (response) => response.json("items").length >= 0,
  });

  sleep(1);
}
