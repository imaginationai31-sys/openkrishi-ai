import pytest

from services.vision.engine import assess_crop_image


VISION_RESPONSE = '{"observations":["yellowing visible"],"possible_causes":["water stress"],"confidence":"low","uncertainties":["image is synthetic"],"recommendations":["check soil moisture"]}'


class FakeResponses:
    def __init__(self, captured):
        self.captured = captured

    def create(self, **kwargs):
        self.captured.update(kwargs)
        return type("FakeResponse", (), {"output_text": VISION_RESPONSE})()


class FakeOpenAI:
    def __init__(self, captured):
        self.responses = FakeResponses(captured)


def patch_openai(monkeypatch, captured):
    import openai
    monkeypatch.setattr(openai, "OpenAI", lambda **kwargs: FakeOpenAI(captured))


def test_crop_image_is_accepted_for_supported_crop(monkeypatch):
    captured = {}
    patch_openai(monkeypatch, captured)
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


@pytest.mark.parametrize("crop_category,crop_name", [
    ("rice", "Swarna"),
    ("rice", "Basmati"),
    ("peanut", "JL 24"),
    ("vegetables", "Potato"),
    ("flowers", "Rose"),
    ("flowers", "Marigold"),
])
def test_selected_crop_variety_is_carried_into_vision_prompt(monkeypatch, crop_category, crop_name):
    captured = {}
    patch_openai(monkeypatch, captured)
    result = assess_crop_image(b"fake-image", "image/jpeg", crop_category, "tillering", "en", crop_name)
    assert result["crop_category"] == crop_category
    assert result["status"] == "assessed"
    prompt = captured["input"][0]["content"][0]["text"]
    assert crop_name in prompt
