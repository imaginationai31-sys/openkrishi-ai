from services.farm.intelligence import build_crop_recommendation, build_farm_plan, build_irrigation, build_fertilizer, build_pest_alerts, build_crop_knowledge

def test_crop_recommendation():
    r=build_crop_recommendation("loam","low","dry","en")
    assert r["recommended_categories"]

def test_irrigation_rain():
    r=build_irrigation("rice","en",30,80,4)
    assert r["action"]=="delay_and_recheck"

def test_fertilizer_is_safe():
    r=build_fertilizer("vegetables","flowering","en")
    assert "soil" in " ".join(r["advice"]).lower()
    assert "dose" in r["safety"].lower()

def test_pest_alerts():
    r=build_pest_alerts("flowers","en",70,85)
    assert r["watch_list"]
    assert r["alerts"]

def test_knowledge():
    r=build_crop_knowledge("peanut","en")
    assert "stages" in r and r["stages"]

def test_farm_plan():
    r=build_farm_plan("rice","tillering","en",{"current":{"temperature_2m":35,"humidity":70,"precipitation":0}})
    assert len(r["tasks"]) >= 4
