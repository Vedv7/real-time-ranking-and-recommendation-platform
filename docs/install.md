# Install Requirements

## Required To Run The Full Stack

- Docker Desktop
- Git

With Docker installed:

```powershell
docker compose up --build
```

## Required For Backend Development Without Docker

- Java 17 JDK
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+
- Kafka or Redpanda

Recommended Windows installs:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install Docker.DockerDesktop
```

Restart Cursor after installing command-line tools so the terminal picks up updated `PATH`.

## Required For ML Development

- Python 3.11+

```powershell
cd ml-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python training/generate_synthetic_data.py
python training/train_ranker.py
python training/backtest_policies.py
```

## Optional Load Testing

- k6

```powershell
winget install k6.k6
k6 run load-tests/k6-feed-ranking.js
```

## Optional Frontend Development

- Node.js 20+

```powershell
cd frontend
npm install
npm run dev
```
