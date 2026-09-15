from services.advisory.normalizer import normalize_agricultural_terms


def test_bengali_yellow_leaf_term_is_normalized():
    text, matched = normalize_agricultural_terms("আমার ধানের পাতা হলুদ", "bn")
    assert "yellow leaf" in text
    assert "yellow leaf" in matched


def test_bengali_voice_yellow_leaf_phrase_is_normalized():
    text, matched = normalize_agricultural_terms("আমার ধান গাছে হলুদ রঙের পাতা দেখা যাচ্ছে।", "bn")
    assert "yellow leaf" in text
    assert "yellow leaf" in matched
    assert "yellow leaves" in matched


def test_hindi_wilting_term_is_normalized():
    text, matched = normalize_agricultural_terms("पौधे मुरझाए हैं", "hi")
    assert "wilting" in text
    assert "wilting" in matched


def test_tamil_leaf_spot_term_is_normalized():
    text, matched = normalize_agricultural_terms("இலையில் புள்ளி உள்ளது", "ta")
    assert "leaf spot" in text
    assert "leaf spot" in matched


def test_punjabi_yellow_leaf_term_is_normalized():
    text, matched = normalize_agricultural_terms("ਪੱਤੇ ਪੀਲੇ ਹੋ ਰਹੇ ਹਨ", "pa")
    assert "yellow leaf" in text
    assert "yellow leaf" in matched


def test_telugu_leaf_spot_term_is_normalized():
    text, matched = normalize_agricultural_terms("ఆకులపై మచ్చలు ఉన్నాయి", "te")
    assert "leaf spots" in text
    assert "leaf spots" in matched


def test_unknown_term_is_not_guessed():
    original = "আমার ধানে অচেনা পোকা আছে"
    text, matched = normalize_agricultural_terms(original, "bn")
    assert text == original
    assert matched == []
