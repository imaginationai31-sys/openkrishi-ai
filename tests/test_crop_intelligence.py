from services.advisory.crop_intelligence import (
    get_crop_profile,
    get_stage_guidance,
)


def test_all_supported_crop_profiles_exist():
    for crop in ("rice", "peanut", "vegetables", "flowers"):
        profile = get_crop_profile(crop)
        assert profile is not None
        assert profile["label"]
        assert profile["stages"]
        assert profile["stage_checks"]


def test_rice_tillering_guidance():
    guidance = get_stage_guidance("rice", "tillering")
    assert guidance is not None
    assert "healthy plants" in guidance.lower()


def test_unknown_crop_returns_none():
    assert get_crop_profile("tomato") is None
    assert get_stage_guidance("tomato", "vegetative") is None


def test_unknown_stage_returns_none():
    assert get_stage_guidance("rice", "unknown stage") is None
