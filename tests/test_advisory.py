from services.advisory.engine import generate_advisory


def test_yellow_leaves_rice():
    result = generate_advisory(
        query="My rice plants have yellow leaves. What should I do?",
        language="en",
        crop_category="rice",
    )

    assert result["language"] == "en"
    assert result["confidence"] == "low"
    assert result["observations"]
    assert any("yellow" in item.lower() for item in result["observations"])
    assert result["recommendations"]
    assert result["uncertainties"]


def test_yellow_leaves_rice_tillering():
    result = generate_advisory(
        query="My rice plants have yellow leaves. What should I do?",
        language="en",
        crop_category="rice",
        growth_stage="tillering",
    )

    assert result["recommendations"]
    assert any("tillering" in item.lower() for item in result["recommendations"])
    assert any("healthy plants" in item.lower() for item in result["recommendations"])


def test_wilting_rice():
    result = generate_advisory(
        query="My rice plants are wilting.",
        language="en",
        crop_category="rice",
    )

    assert result["observations"]
    assert any("wilting" in item.lower() or "drooping" in item.lower() for item in result["observations"])
    assert result["recommendations"]
    assert result["uncertainties"]


def test_leaf_spots_rice():
    result = generate_advisory(
        query="My rice leaves have brown spots.",
        language="en",
        crop_category="rice",
    )

    assert result["observations"]
    assert any("spot" in item.lower() for item in result["observations"])
    assert result["recommendations"]
    assert result["uncertainties"]


def test_missing_growth_stage():
    result = generate_advisory(
        query="My rice plants have yellow leaves. What should I do?",
        language="en",
        crop_category="rice",
        growth_stage=None,
    )

    assert result["uncertainties"]
    assert any("growth stage" in item.lower() for item in result["uncertainties"])
    assert not any("tillering" in item.lower() for item in result["recommendations"])
