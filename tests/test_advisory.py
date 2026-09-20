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


def test_location_is_returned_and_used():
    result = generate_advisory(
        query="My rice plants have yellow leaves. What should I do?",
        language="en",
        crop_category="rice",
        location="Kanchipuram, Tamil Nadu, India",
    )

    assert result["location"] == "Kanchipuram, Tamil Nadu, India"
    assert any("kanchipuram" in item.lower() for item in result["recommendations"])
    assert any("live local weather" in item.lower() for item in result["uncertainties"])


def test_blank_location_is_treated_as_missing():
    result = generate_advisory(
        query="My rice plants have yellow leaves. What should I do?",
        language="en",
        crop_category="rice",
        location="   ",
    )

    assert result["location"] is None
    assert any("location was not provided" in item.lower() for item in result["uncertainties"])


import pytest

@pytest.mark.parametrize("crop_category,crop_name", [
    ("rice", "Swarna"),
    ("rice", "Basmati"),
    ("peanut", "JL 24"),
    ("vegetables", "Potato"),
    ("flowers", "Rose"),
    ("flowers", "Marigold"),
])
def test_selected_crop_variety_is_carried_into_advisory(crop_category, crop_name):
    result = generate_advisory(
        query="yellow leaves",
        language="en",
        crop_category=crop_category,
        crop_name=crop_name,
    )
    assert result["crop_name"] == crop_name
    assert crop_name in result["answer"]
