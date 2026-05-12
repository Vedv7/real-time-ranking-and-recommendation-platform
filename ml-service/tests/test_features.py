from app.features import confidence_from_probability, deterministic_score
from app.schemas import RankingFeatures


def make_features(**overrides):
    values = {
        "user_total_interactions": 100,
        "user_avg_watch_time": 24.0,
        "user_like_rate": 0.2,
        "user_skip_rate": 0.1,
        "content_popularity_score": 0.7,
        "content_freshness_score": 0.8,
        "content_engagement_rate": 0.25,
        "category_match": 1,
        "creator_affinity": 0.3,
        "content_age_hours": 8,
    }
    values.update(overrides)
    return RankingFeatures(**values)


def test_deterministic_score_prefers_relevant_fresh_content():
    strong = deterministic_score(make_features(category_match=1, content_freshness_score=0.95))
    weak = deterministic_score(make_features(category_match=0, content_freshness_score=0.05, user_skip_rate=0.6))

    assert strong > weak
    assert 0.0 < weak < 1.0
    assert 0.0 < strong < 1.0


def test_confidence_increases_away_from_uncertain_probability():
    assert confidence_from_probability(0.9) > confidence_from_probability(0.55)
