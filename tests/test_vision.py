import pytest

from services.vision.engine import assess_crop_image


def test_crop_image_is_accepted_for_supported_crop():
    result = assess_crop_image(b"fake-image", "image/jpeg", "rice", "tillering")
    assert result["status"] == "ready_for_visual_model"
    assert result["crop_category"] == "rice"
    assert result["growth_stage"] == "tillering"
    assert result["confidence"] == "low"
    assert result["safety"] == "caution"


def test_supported_crop_scope_excludes_tomato_and_chilli():
    with pytest.raises(ValueError):
        assess_crop_image(b"fake-image", "image/jpeg", "tomato")
    with pytest.raises(ValueError):
        assess_crop_image(b"fake-image", "image/jpeg", "chilli")


def test_invalid_image_type_is_rejected():
    with pytest.raises(ValueError, match="Unsupported image type"):
        assess_crop_image(b"fake-image", "application/pdf", "rice")


def test_empty_image_is_rejected():
    with pytest.raises(ValueError, match="Image is empty"):
        assess_crop_image(b"", "image/jpeg", "rice")


def test_large_image_is_rejected():
    with pytest.raises(ValueError, match="10 MB"):
        assess_crop_image(b"x" * (10 * 1024 * 1024 + 1), "image/jpeg", "rice")
