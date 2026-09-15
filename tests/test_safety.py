from services.advisory.safety import enforce_advisory_safety


def test_unsafe_treatment_instruction_is_removed():
    recommendations, uncertainties, safety = enforce_advisory_safety(
        ["Spray pesticide immediately.", "Check soil moisture first."],
        [],
        confidence="high",
    )
    assert recommendations == ["Check soil moisture first."]
    assert any("unsafe treatment" in item.lower() for item in uncertainties)
    assert safety["status"] == "caution"


def test_uncertain_diagnosis_forces_low_confidence():
    recommendations, uncertainties, safety = enforce_advisory_safety(
        ["The pest may be responsible; inspect affected leaves."],
        [],
        confidence="high",
    )
    assert recommendations
    assert any("confirmation" in item.lower() for item in uncertainties)
    assert safety["status"] == "caution"


def test_safe_recommendation_is_preserved():
    recommendations, uncertainties, safety = enforce_advisory_safety(
        ["Check soil moisture and compare healthy plants."],
        [],
        confidence="low",
    )
    assert recommendations == ["Check soil moisture and compare healthy plants."]
    assert safety["status"] == "caution"
