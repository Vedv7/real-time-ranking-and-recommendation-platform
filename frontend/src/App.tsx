import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

type RankedItem = {
  contentId: number;
  title: string;
  category: string;
  predictedCtr: number;
  finalScore: number;
  rankPosition: number;
  explanation: string[];
};

type FeedResponse = {
  userId: number;
  generatedAt: string;
  latencyMs: number;
  items: RankedItem[];
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

function App() {
  const [userId, setUserId] = useState("1");
  const [feed, setFeed] = useState<FeedResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadFeed() {
    setError(null);
    const response = await fetch(`${API_BASE_URL}/api/feed/${userId}?limit=10`);
    if (!response.ok) {
      setError(`Feed request failed: ${response.status}`);
      return;
    }
    setFeed(await response.json());
  }

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Real-Time Ranking Platform</p>
        <h1>Personalized Feed Debugger</h1>
        <p>
          Inspect ranked recommendations, prediction scores, freshness boosts, and explanation metadata from the Spring Boot API.
        </p>
        <div className="controls">
          <label>
            <span>User ID</span>
            <input
              value={userId}
              onChange={(event) => setUserId(event.target.value)}
              placeholder="Enter user ID"
            />
          </label>
          <button onClick={loadFeed}>Load feed</button>
        </div>
      </section>

      {error && <p className="error">{error}</p>}
      {feed && (
        <section>
          <div className="summary">
            <span>User {feed.userId}</span>
            <span>{feed.items.length} items</span>
            <span>{feed.latencyMs} ms</span>
          </div>
          <div className="grid">
            {feed.items.map((item) => (
              <article key={item.contentId} className="card">
                <div className="rank">#{item.rankPosition}</div>
                <h2>{item.title}</h2>
                <p>{item.category}</p>
                <div className="scores">
                  <span>CTR {item.predictedCtr}</span>
                  <span>Score {item.finalScore}</span>
                </div>
                <ul>
                  {item.explanation.map((reason) => (
                    <li key={reason}>{reason}</li>
                  ))}
                </ul>
              </article>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
