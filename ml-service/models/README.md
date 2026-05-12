Model artifacts are written here by the training pipeline:

- `ranking_model.joblib`
- `metrics.json`
- `model_metadata.json`
- `backtest_report.json`
- `feature_importance.png`

The FastAPI service falls back to deterministic scoring when `ranking_model.joblib` is not present, so the local stack remains runnable before training.
