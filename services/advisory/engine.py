"""Deterministic MVP advisory engine.

This is intentionally conservative. It provides basic crop-aware guidance
without pretending to diagnose diseases or prescribe chemical treatments.
"""

from typing import Any

from .safety import low_confidence_safety

SUPPORTED_LANGUAGES = {"bn", "hi", "ta", "pa", "te"}
CROP_LABELS = {
    "rice": "Rice",
    "peanut": "Peanut",
    "vegetables": "Vegetables",
    "flowers": "Flowers",
}


def generate_advisory(query: str, language: str, crop_category: str | None = None) -> dict[str, Any]:
    crop_label = CROP_LABELS.get(crop_category or "")

    if language not in SUPPORTED_LANGUAGES:
        language = "en"

    # Keep the first MVP conservative: no fabricated diagnosis, pesticide dose,
    # or crop-specific claim that requires expert/validated evidence.
    if crop_label:
        answer = {
            "bn": f"{crop_label} নিয়ে আপনার প্রশ্নটি বুঝেছি। এই MVP-তে নিরাপদ পরামর্শের জন্য প্রশ্নটি, ফসলের বর্তমান বৃদ্ধি-পর্যায় এবং প্রয়োজন হলে ছবি/আবহাওয়ার তথ্য আরও দরকার।",
            "hi": f"मैंने {crop_label} से जुड़ा आपका सवाल समझ लिया है। इस MVP में सुरक्षित सलाह के लिए सवाल के साथ फसल की वर्तमान अवस्था और जरूरत होने पर फोटो/मौसम की जानकारी चाहिए।",
            "ta": f"{crop_label} பற்றிய உங்கள் கேள்வியை புரிந்துகொண்டேன். இந்த MVP-யில் பாதுகாப்பான ஆலோசனைக்கு கேள்வியுடன் பயிரின் தற்போதைய வளர்ச்சி நிலையும், தேவையானால் படம்/வானிலை தகவலும் தேவை.",
            "pa": f"ਮੈਂ {crop_label} ਬਾਰੇ ਤੁਹਾਡਾ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਇਸ MVP ਵਿੱਚ ਸੁਰੱਖਿਅਤ ਸਲਾਹ ਲਈ ਸਵਾਲ ਦੇ ਨਾਲ ਫਸਲ ਦੀ ਮੌਜੂਦਾ ਅਵਸਥਾ ਅਤੇ ਲੋੜ ਪੈਣ ਤੇ ਤਸਵੀਰ/ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਚਾਹੀਦੀ ਹੈ।",
            "te": f"{crop_label} గురించి మీ ప్రశ్నను అర్థం చేసుకున్నాను. ఈ MVPలో సురక్షితమైన సలహా కోసం ప్రశ్నతో పాటు పంట ప్రస్తుత దశ, అవసరమైతే ఫోటో/వాతావరణ సమాచారం అవసరం.",
        }[language]
    else:
        answer = {
            "bn": "আপনার কৃষি প্রশ্নটি বুঝেছি। নিরাপদ পরামর্শের জন্য ফসলের ধরন, বর্তমান বৃদ্ধি-পর্যায় এবং প্রয়োজন হলে স্থান/আবহাওয়ার তথ্য জানা দরকার।",
            "hi": "मैंने आपका कृषि सवाल समझ लिया है। सुरक्षित सलाह के लिए फसल का प्रकार, वर्तमान अवस्था और जरूरत होने पर स्थान/मौसम की जानकारी चाहिए।",
            "ta": "உங்கள் விவசாயக் கேள்வியை புரிந்துகொண்டேன். பாதுகாப்பான ஆலோசனைக்கு பயிர் வகை, தற்போதைய வளர்ச்சி நிலை மற்றும் தேவையானால் இடம்/வானிலை தகவல் தேவை.",
            "pa": "ਮੈਂ ਤੁਹਾਡਾ ਖੇਤੀਬਾੜੀ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਸੁਰੱਖਿਅਤ ਸਲਾਹ ਲਈ ਫਸਲ ਦੀ ਕਿਸਮ, ਮੌਜੂਦਾ ਅਵਸਥਾ ਅਤੇ ਲੋੜ ਪੈਣ ਤੇ ਸਥਾਨ/ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਚਾਹੀਦੀ ਹੈ।",
            "te": "మీ వ్యవసాయ ప్రశ్నను అర్థం చేసుకున్నాను. సురక్షితమైన సలహా కోసం పంట రకం, ప్రస్తుత దశ, అవసరమైతే ప్రాంతం/వాతావరణ సమాచారం అవసరం.",
        }[language]

    return {
        "answer": answer,
        "language": language,
        "confidence": "low",
        "safety": low_confidence_safety(),
        "observations": [],
        "recommendations": [],
        "uncertainties": [
            "The MVP does not yet use validated agronomy knowledge, live weather, or image evidence."
        ],
        "source_references": [],
    }
