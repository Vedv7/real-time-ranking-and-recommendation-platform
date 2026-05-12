# Ranking Experiments

The backend assigns users to deterministic ranking buckets in `feed-ranking-v2`.

## Policies

- `CONTROL`: baseline model score plus freshness, popularity, and category match.
- `FRESHNESS_BOOST`: adds extra weight for recent content.
- `DIVERSITY_BOOST`: penalizes repeated exposure to the same category.
- `EXPLORATION_BOOST`: boosts novel categories so the system can learn new interests.

## Logged Metadata

Each recommendation log stores:

- `modelVersion`
- `rankingPolicy`
- `experimentBucket`
- `featureSnapshotJson`
- `latencyMs`

This makes ranking decisions auditable and supports offline analysis.

## Offline Backtesting

Run:

```bash
cd ml-service
python training/generate_synthetic_data.py
python training/backtest_policies.py
```

The report is written to `ml-service/models/backtest_report.json` with `precision_at_10`, `ndcg_at_10`, high-skip exposure, and evaluated user count.
