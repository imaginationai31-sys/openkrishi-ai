"""Farmer-language understanding helpers for the voice MVP.

This layer is intentionally conservative: it extracts only high-confidence
information and asks clarifying questions when the transcript is ambiguous.
"""

from typing import Any


SYMPTOM_LABELS = {
    "yellow leaf": "yellowing",
    "wilting": "wilting",
    "leaf spot": "leaf spots",
}

QUESTIONS = {
    "bn": (
        "পাতাগুলো কি হলুদ হয়ে যাচ্ছে?",
        "পাতায় কি দাগ দেখা যাচ্ছে?",
        "গাছ কি ঢলে পড়ছে বা শুকিয়ে যাচ্ছে?",
    ),
    "hi": (
        "क्या पत्ते पीले हो रहे हैं?",
        "क्या पत्तियों पर दाग दिखाई दे रहे हैं?",
        "क्या पौधा मुरझा या झुक रहा है?",
    ),
    "ta": (
        "இலைகள் மஞ்சளாகிறதா?",
        "இலைகளில் புள்ளிகள் உள்ளதா?",
        "செடி வாடுகிறதா அல்லது சாய்கிறதா?",
    ),
    "pa": (
        "ਕੀ ਪੱਤੇ ਪੀਲੇ ਹੋ ਰਹੇ ਹਨ?",
        "ਕੀ ਪੱਤਿਆਂ ਤੇ ਧੱਬੇ ਦਿਖ ਰਹੇ ਹਨ?",
        "ਕੀ ਪੌਦਾ ਮੁਰਝਾ ਜਾਂ ਝੁਕ ਰਿਹਾ ਹੈ?",
    ),
    "te": (
        "ఆకులు పసుపు రంగులోకి మారుతున్నాయా?",
        "ఆకులపై మచ్చలు కనిపిస్తున్నాయా?",
        "మొక్క వాడిపోతుందా లేదా వాలిపోతుందా?",
    ),
}


def build_voice_understanding(
    raw_text: str,
    normalized_text: str,
    matched_terms: list[str],
    language: str,
    crop_category: str | None,
) -> dict[str, Any]:
    """Return conservative structured interpretation for a farmer transcript."""
    symptoms = [SYMPTOM_LABELS[t] for t in matched_terms if t in SYMPTOM_LABELS]
    confident = bool(symptoms)

    result: dict[str, Any] = {
        "raw_text": raw_text,
        "normalized_text": normalized_text,
        "detected_crop": crop_category,
        "possible_symptoms": symptoms,
        "interpretation_confidence": "medium" if confident else "low",
        "needs_clarification": not confident,
        "follow_up_questions": [],
    }

    if not confident:
        result["follow_up_questions"] = list(
            QUESTIONS.get(language, QUESTIONS["bn"])
        )

    return result
