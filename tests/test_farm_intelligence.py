import pytest

from services.farm.intelligence import (
    build_crop_knowledge,
    build_crop_recommendation,
    build_farm_plan,
    build_fertilizer,
    build_irrigation,
    build_pest_alerts,
)


def test_crop_recommendation():
    r = build_crop_recommendation("loam", "low", "dry", "en")
    assert r["recommended_categories"]


def test_crop_recommendation_rainy_season_and_soil():
    r = build_crop_recommendation("clay", "normal", "monsoon", "en")
    assert r["recommended_categories"] == ["rice", "vegetables"]
    assert any("Soil type provided" in note for note in r["notes"])


def test_irrigation_rain():
    r = build_irrigation("rice", "en", 30, 80, 4)
    assert r["action"] == "delay_and_recheck"


def test_irrigation_heat_and_default():
    hot = build_irrigation("peanut", "en", 35, 20)
    normal = build_irrigation("flowers", "en", 25, 20)
    assert hot["action"] == "check_moisture_more_often"
    assert normal["action"] == "check_root_zone"


def test_fertilizer_is_safe():
    r = build_fertilizer("vegetables", "flowering", "en")
    assert "soil" in " ".join(r["advice"]).lower()
    assert "dose" in r["safety"].lower()


def test_fertilizer_without_growth_stage():
    r = build_fertilizer("rice", None, "en")
    assert r["growth_stage"] is None
    assert all(not item.startswith("Growth stage:") for item in r["advice"])


def test_pest_alerts():
    r = build_pest_alerts("flowers", "en", 70, 85)
    assert r["watch_list"]
    assert len(r["alerts"]) == 2


def test_pest_alerts_without_weather_trigger():
    r = build_pest_alerts("rice", "en", 20, 50)
    assert len(r["alerts"]) == 1


def test_knowledge():
    r = build_crop_knowledge("peanut", "en")
    assert r["stages"]


def test_invalid_crop_category_is_rejected():
    with pytest.raises(ValueError, match="Unsupported crop category"):
        build_irrigation("maize", "en")
    with pytest.raises(ValueError, match="Unsupported crop category"):
        build_fertilizer("maize", None, "en")
    with pytest.raises(ValueError, match="Unsupported crop category"):
        build_pest_alerts("maize", "en")
    with pytest.raises(ValueError, match="Unsupported crop category"):
        build_crop_knowledge("maize", "en")


def test_farm_plan():
    r = build_farm_plan(
        "rice",
        "tillering",
        "en",
        {"current": {"temperature_2m": 35, "humidity": 70, "precipitation": 0}},
    )
    assert len(r["tasks"]) >= 4


def test_farm_plan_rain_takes_priority():
    r = build_farm_plan(
        "vegetables",
        "flowering",
        "en",
        {"current": {"temperature_2m": 35, "humidity": 80, "precipitation": 2}},
    )
    assert r["tasks"][0].startswith("Rain is expected")
