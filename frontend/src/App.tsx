import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

type InteractionType = "VIEW" | "LIKE" | "CLICK" | "SKIP" | "SAVE" | "SHARE" | "WATCH_TIME";

type RankedItem = {
  contentId: number;
  title: string;
  category: string;
  predictedCtr: number;
  contentPopularityScore: number;
  userInterestMatchScore: number;
  freshnessScore: number;
  explorationScore: number;
  diversityPenalty: number;
  finalScore: number;
  rankPosition: number;
  rankingPolicy: string;
  modelVersion: string;
  explanation: string[];
};

type FeedResponse = {
  userId: number;
  generatedAt: string;
  latencyMs: number;
  candidateCount: number;
  experimentBucket: string;
  rankingPolicy: string;
  modelVersion: string;
  items: RankedItem[];
};

type ContentItem = {
  id: number;
  title: string;
  category: string;
  durationSeconds: number;
  language: string;
  qualityScore: number;
};

type ModelMetadata = {
  model_version: string;
  model_stage: string;
  using_fallback: boolean;
  loaded_at: string;
};

type FeatureStoreStatus = {
  userFeatureCount: number;
  contentFeatureCount: number;
  staleUserFeatureCount: number;
  staleContentFeatureCount: number;
  freshnessThresholdMinutes: number;
};

type EventLogItem = {
  type: InteractionType;
  contentId: number;
  title: string;
  at: string;
};

type DemoResetResponse = {
  demoUserId: number;
  usersCreated: number;
  contentCreated: number;
  eventsCreated: number;
  message: string;
};

type RecommendationLog = {
  id: number;
  userId: number;
  contentId: number;
  title: string;
  predictedCtr: number;
  finalScore: number;
  rankPosition: number;
  modelVersion: string;
  rankingPolicy: string;
  experimentBucket: string;
  latencyMs: number;
  createdAt: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const ACTIONS: Array<{ type: InteractionType; label: string; tone: "positive" | "negative" | "neutral" }> = [
  { type: "LIKE", label: "Like", tone: "positive" },
  { type: "SAVE", label: "Save", tone: "positive" },
  { type: "SHARE", label: "Share", tone: "positive" },
  { type: "SKIP", label: "Skip", tone: "negative" },
  { type: "VIEW", label: "View", tone: "neutral" },
];

function App() {
  const [userId, setUserId] = useState("1");
  const [feed, setFeed] = useState<FeedResponse | null>(null);
  const [content, setContent] = useState<ContentItem[]>([]);
  const [model, setModel] = useState<ModelMetadata | null>(null);
  const [featureStore, setFeatureStore] = useState<FeatureStoreStatus | null>(null);
  const [eventTrail, setEventTrail] = useState<EventLogItem[]>([]);
  const [recommendationLogs, setRecommendationLogs] = useState<RecommendationLog[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isResettingDemo, setIsResettingDemo] = useState(false);

  const topCategory = useMemo(() => {
    if (!feed?.items.length) {
      return "Learning";
    }
    const counts = feed.items.reduce<Record<string, number>>((acc, item) => {
      acc[item.category] = (acc[item.category] ?? 0) + 1;
      return acc;
    }, {});
    return Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0] ?? "Learning";
  }, [feed]);

  useEffect(() => {
    void refreshPlatform();
    void loadContent();
    void loadFeed();
  }, []);

  async function loadFeed(userOverride?: string) {
    const targetUserId = userOverride ?? userId;
    if (!targetUserId) {
      setError("Enter a user ID first.");
      return;
    }
    setError(null);
    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/feed/${targetUserId}?limit=10`);
      if (!response.ok) {
        setError(`Feed request failed: ${response.status}. Seed data first or create a user/content/event.`);
        return;
      }
      setFeed(await response.json());
      window.setTimeout(() => void loadRecommendationLogs(targetUserId), 150);
    } catch {
      setError("Could not reach backend. Start Docker Compose and try again.");
    } finally {
      setIsLoading(false);
    }
  }

  async function loadContent() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/content`);
      if (response.ok) {
        setContent(await response.json());
      }
    } catch {
      // Content is supporting context; feed loading handles user-visible errors.
    }
  }

  async function refreshPlatform() {
    try {
      const [modelResponse, featureResponse] = await Promise.all([
        fetch(`${API_BASE_URL}/api/platform/model`),
        fetch(`${API_BASE_URL}/api/platform/feature-store`),
      ]);
      if (modelResponse.ok) {
        setModel(await modelResponse.json());
      }
      if (featureResponse.ok) {
        setFeatureStore(await featureResponse.json());
      }
    } catch {
      // Diagnostics are optional for the demo surface.
    }
  }

  async function loadRecommendationLogs(userOverride?: string) {
    const targetUserId = userOverride ?? userId;
    if (!targetUserId) {
      return;
    }
    try {
      const response = await fetch(`${API_BASE_URL}/api/platform/recommendation-logs/${targetUserId}`);
      if (response.ok) {
        setRecommendationLogs(await response.json());
      }
    } catch {
      // Recommendation logs are supporting evidence; feed loading owns visible errors.
    }
  }

  async function recordInteraction(item: RankedItem, type: InteractionType) {
    setError(null);
    try {
      const response = await fetch(`${API_BASE_URL}/api/events`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: Number(userId),
          contentId: item.contentId,
          interactionType: type,
          watchTimeSeconds: type === "SKIP" ? 3 : 45,
          deviceType: "rankstream-demo",
          sessionId: `demo-${Date.now()}`,
        }),
      });
      if (!response.ok) {
        setError(`Event failed: ${response.status}`);
        return;
      }
      setEventTrail((events) => [
        { type, contentId: item.contentId, title: item.title, at: new Date().toLocaleTimeString() },
        ...events,
      ].slice(0, 8));
      window.setTimeout(() => {
        void loadFeed();
        void refreshPlatform();
      }, 900);
    } catch {
      setError("Could not post event. Check that the backend is running.");
    }
  }

  async function createDemoUser() {
    const username = `demo_user_${Date.now()}`;
    setError(null);
    try {
      const response = await fetch(`${API_BASE_URL}/api/users`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, ageGroup: "25-34", country: "US" }),
      });
      if (!response.ok) {
        setError(`Create user failed: ${response.status}`);
        return;
      }
      const user = await response.json();
      setUserId(String(user.id));
      setEventTrail([]);
      setRecommendationLogs([]);
      window.setTimeout(() => void loadFeed(String(user.id)), 100);
    } catch {
      setError("Could not create demo user. Check that the backend is running.");
    }
  }

  async function resetDemoData() {
    setError(null);
    setIsResettingDemo(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/platform/demo/reset`, { method: "POST" });
      if (!response.ok) {
        setError(`Demo reset failed: ${response.status}`);
        return;
      }
      const reset: DemoResetResponse = await response.json();
      setUserId(String(reset.demoUserId));
      setEventTrail([
        {
          type: "VIEW",
          contentId: 0,
          title: `${reset.contentCreated} fresh demo items seeded`,
          at: new Date().toLocaleTimeString(),
        },
      ]);
      setRecommendationLogs([]);
      await Promise.all([loadContent(), refreshPlatform(), loadFeed(String(reset.demoUserId))]);
    } catch {
      setError("Could not reset demo data. Check that the backend is running.");
    } finally {
      setIsResettingDemo(false);
    }
  }

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">RankStream Demo Control Room</p>
          <h1>Watch a real-time feed ranker learn from user behavior.</h1>
          <p>
            Like, save, share, view, or skip content. RankStream sends events to Kafka, updates online features,
            calls the ML inference service, applies an experiment policy, and returns a new explainable feed.
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
            <button onClick={() => void loadFeed()} disabled={isLoading}>{isLoading ? "Ranking..." : "Refresh feed"}</button>
            <button className="secondary" onClick={createDemoUser}>New demo user</button>
            <button className="secondary danger" onClick={() => void resetDemoData()} disabled={isResettingDemo}>
              {isResettingDemo ? "Resetting..." : "Reset demo data"}
            </button>
          </div>
        </div>
        <div className="hero-card">
          <span className="live-dot" />
          <p>Current ranking policy</p>
          <strong>{feed?.rankingPolicy ?? "Waiting for feed"}</strong>
          <small>{feed?.experimentBucket ?? "Run the feed to see experiment assignment"}</small>
        </div>
      </section>

      {error && <p className="error">{error}</p>}

      <section className="stats-grid">
        <Metric label="Ranked user" value={feed ? `#${feed.userId}` : "Not loaded"} />
        <Metric label="Candidates scored" value={feed?.candidateCount ?? "-"} />
        <Metric label="Latency" value={feed ? `${feed.latencyMs} ms` : "-"} />
        <Metric label="Top category" value={topCategory} />
        <Metric label="Model" value={feed?.modelVersion ?? model?.model_version ?? "-"} />
      </section>

      <section className="workspace">
        <aside className="sidebar">
          <Panel title="Demo script">
            <ol className="steps">
              <li>Click Reset demo data for a clean storyline.</li>
              <li>Like or save tech/education content.</li>
              <li>Skip unrelated content.</li>
              <li>Refresh and watch ranking metadata change.</li>
            </ol>
          </Panel>

          <Panel title="Platform health">
            <div className="kv"><span>ML stage</span><strong>{model?.model_stage ?? "unknown"}</strong></div>
            <div className="kv"><span>Fallback model</span><strong>{model?.using_fallback ? "yes" : "no"}</strong></div>
            <div className="kv"><span>User features</span><strong>{featureStore?.userFeatureCount ?? "-"}</strong></div>
            <div className="kv"><span>Content features</span><strong>{featureStore?.contentFeatureCount ?? "-"}</strong></div>
            <button className="wide secondary" onClick={refreshPlatform}>Refresh diagnostics</button>
          </Panel>

          <Panel title="Recent events">
            {eventTrail.length === 0 ? (
              <p className="muted">No demo events yet. Click an action on a feed card.</p>
            ) : (
              <div className="event-list">
                {eventTrail.map((event) => (
                  <div className="event" key={`${event.at}-${event.contentId}-${event.type}`}>
                    <strong>{event.type}</strong>
                    <span>{event.title}</span>
                    <small>{event.at}</small>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Catalog snapshot">
            <p className="muted">{content.length} content items available through the backend.</p>
          </Panel>

          <Panel title="Decision log">
            {recommendationLogs.length === 0 ? (
              <p className="muted">Load a feed to write recommendation logs.</p>
            ) : (
              <div className="log-list">
                {recommendationLogs.slice(0, 5).map((log) => (
                  <div className="log-row" key={log.id}>
                    <strong>#{log.rankPosition} {log.title}</strong>
                    <span>{log.rankingPolicy} · {log.finalScore.toFixed(3)} final · {log.latencyMs} ms</span>
                    <small>{new Date(log.createdAt).toLocaleTimeString()}</small>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        </aside>

        <section className="feed-stage">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Personalized feed</p>
              <h2>Ranked recommendations</h2>
            </div>
            {feed && <span className="timestamp">Generated {new Date(feed.generatedAt).toLocaleTimeString()}</span>}
          </div>

          {!feed && (
            <div className="empty-state">
              <h2>No feed loaded yet</h2>
              <p>Start Docker Compose, seed demo data, then load a user feed to see RankStream in action.</p>
              <button onClick={() => void loadFeed()}>Load feed</button>
            </div>
          )}

          {feed && feed.items.length === 0 && (
            <div className="empty-state">
              <h2>No candidates left for this user</h2>
              <p>Create more content or use a new demo user to reset seen-content filtering.</p>
              <button onClick={createDemoUser}>Create new demo user</button>
            </div>
          )}

          {feed && feed.items.length > 0 && (
            <div className="feed-list">
              {feed.items.map((item) => (
                <article key={item.contentId} className="feed-card">
                  <div className="card-topline">
                    <span className="rank">#{item.rankPosition}</span>
                    <span className="category">{item.category}</span>
                  </div>
                  <h3>{item.title}</h3>
                  <p className="policy">{item.rankingPolicy} · {item.modelVersion}</p>

                  <div className="score-row">
                    <Score label="Final" value={item.finalScore} strong />
                    <Score label="CTR" value={item.predictedCtr} />
                    <Score label="Fresh" value={item.freshnessScore} />
                    <Score label="Popular" value={item.contentPopularityScore} />
                    <Score label="Explore" value={item.explorationScore} />
                  </div>

                  <div className="explain-box">
                    <span>Why ranked here</span>
                    <ul>
                      {item.explanation.map((reason) => (
                        <li key={reason}>{reason}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="actions">
                    {ACTIONS.map((action) => (
                      <button
                        key={action.type}
                        className={`action ${action.tone}`}
                        onClick={() => void recordInteraction(item, action.type)}
                      >
                        {action.label}
                      </button>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function Score({ label, value, strong = false }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={strong ? "score strong-score" : "score"}>
      <div>
        <span>{label}</span>
        <strong>{value.toFixed(3)}</strong>
      </div>
      <meter className="bar" min={0} max={1} value={Math.max(0.04, Math.min(1, value))} />
    </div>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
