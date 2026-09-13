"""Conservative MVP advisory engine with a small crop-knowledge layer."""

from typing import Any

from .knowledge import get_knowledge
from .safety import low_confidence_safety

SUPPORTED_LANGUAGES = {"en", "bn", "hi", "ta", "pa", "te"}
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

    knowledge = get_knowledge(query, crop_category)

    if crop_label:
        answer = {
            "en": f"I understand your question about {crop_label}. I found a possible symptom match and can provide conservative checks based on the information supplied.",
            "bn": f"{crop_label} নিয়ে আপনার প্রশ্নটি বুঝেছি। দেওয়া তথ্যের ভিত্তিতে একটি সম্ভাব্য উপসর্গের মিল পেয়েছি এবং নিরাপদ কিছু পরীক্ষা করার পরামর্শ দিতে পারি।",
            "hi": f"मैंने {crop_label} से जुड़ा आपका सवाल समझ लिया है। दी गई जानकारी के आधार पर एक संभावित लक्षण मिला है और मैं कुछ सुरक्षित जांच सुझा सकता हूँ।",
            "ta": f"{crop_label} பற்றிய உங்கள் கேள்வியை புரிந்துகொண்டேன். கொடுக்கப்பட்ட தகவலின் அடிப்படையில் ஒரு சாத்தியமான அறிகுறி பொருத்தம் உள்ளது; சில பாதுகாப்பான சோதனைகளை பரிந்துரைக்கலாம்.",
            "pa": f"ਮੈਂ {crop_label} ਬਾਰੇ ਤੁਹਾਡਾ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਦਿੱਤੀ ਜਾਣਕਾਰੀ ਦੇ ਆਧਾਰ ਤੇ ਇੱਕ ਸੰਭਾਵੀ ਲੱਛਣ ਮਿਲਦਾ ਹੈ ਅਤੇ ਮੈਂ ਕੁਝ ਸੁਰੱਖਿਅਤ ਜਾਂਚਾਂ ਸੁਝਾ ਸਕਦਾ ਹਾਂ।",
            "te": f"{crop_label} గురించి మీ ప్రశ్నను అర్థం చేసుకున్నాను. ఇచ్చిన సమాచారంలో ఒక సంభావ్య లక్షణం కనిపిస్తోంది; కొన్ని సురక్షితమైన తనిఖీలను సూచించగలను.",
        }[language]
    else:
        answer = {
            "en": "I understand your agricultural question. Please provide the crop type and growth stage for more specific guidance.",
            "bn": "আপনার কৃষি প্রশ্নটি বুঝেছি। আরও নির্দিষ্ট পরামর্শের জন্য ফসলের ধরন ও বৃদ্ধি-পর্যায় জানান।",
            "hi": "मैंने आपका कृषि सवाल समझ लिया है। अधिक विशिष्ट सलाह के लिए फसल का प्रकार और वर्तमान अवस्था बताएं।",
            "ta": "உங்கள் விவசாயக் கேள்வியை புரிந்துகொண்டேன். மேலும் குறிப்பிட்ட ஆலோசனைக்கு பயிர் வகை மற்றும் தற்போதைய வளர்ச்சி நிலையை தெரிவிக்கவும்.",
            "pa": "ਮੈਂ ਤੁਹਾਡਾ ਖੇਤੀਬਾੜੀ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਹੋਰ ਖਾਸ ਸਲਾਹ ਲਈ ਫਸਲ ਦੀ ਕਿਸਮ ਅਤੇ ਮੌਜੂਦਾ ਅਵਸਥਾ ਦੱਸੋ।",
            "te": "మీ వ్యవసాయ ప్రశ్నను అర్థం చేసుకున్నాను. మరింత నిర్దిష్టమైన సలహా కోసం పంట రకం మరియు ప్రస్తుత దశను ఇవ్వండి.",
        }[language]

    return {
        "answer": answer,
        "language": language,
        "confidence": "low",
        "safety": low_confidence_safety(),
        "observations": knowledge["observations"],
        "recommendations": knowledge["recommendations"],
        "uncertainties": knowledge["uncertainties"],
        "source_references": [],
    }
