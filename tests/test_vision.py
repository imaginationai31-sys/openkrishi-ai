import pytest

from services.vision.engine import assess_crop_image


class FakeInteraction:
    output_text = '{"observations":["yellowing visible"],"possible_causes":["water stress"],"confidence":"low","uncertainties":["image is synthetic"],"recommendations":["check soil moisture"]}'


class FakeInteractions:
    def create(self, **kwargs):
        assert kwargs["model"]
        assert kwargs["input"][0]["type"] == "text"
        assert kwargs["input"][1]["type"] == "image"
        assert kwargs["input"][1]["mime_type"] == "image/jpeg"
        return FakeInteraction()


class FakeGeminiClient:
    interactions = FakeInteractions()


def test_crop_image_is_accepted_for_supported_crop(monkeypatch):
    monkeypatch.setattr("services.vision.engine.get_gemini_client", lambda: FakeGeminiClient())

    result = assess_crop_image(b"fake-image", "image/jpeg", "rice", "tillering")
    assert result["status"] == "assessed"
    assert result["crop_category"] == "rice"
    assert result["growth_stage"] == "tillering"
    assert result["confidence"] == "low"
    assert result["safety"] == "caution"
    assert result["observations"] == ["yellowing visible"]


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


def test_vision_route_exposes_advisory_layer():
    from services.api.routes.vision import vision_assess

    assert vision_assess.__name__ == "vision_assess"
    assert "language" in vision_assess.__annotations__
    assert "location" in vision_assess.__annotations__
