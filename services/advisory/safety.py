"""Safety helpers for agricultural advisory responses."""

from typing import Any


UNSAFE_ACTION_TERMS = (
    "pesticide",
    "insecticide",
    "fungicide",
    "herbicide",
    "chemical spray",
    "spray dose",
    "large fertilizer dose",
    "double dose",
)

CAUTION_TERMS = (
    "disease",
    "pest",
    "deficiency",
    "fungus",
    "virus",
    "bacteria",
)


def build_safety(status: str = "caution", reason: str = "") -> dict[str, Any]:
    return {"status": status, "reason": reason}


def low_confidence_safety() -> dict[str, Any]:
    return build_safety(
        "caution",
        "This MVP uses limited rule-based guidance. Confirm important farm decisions with a qualified local agronomist.",
    )


def enforce_advisory_safety(
    recommendations: list[str],
    uncertainties: list[str],
    confidence: str = "low",
    language: str = "en",
) -> tuple[list[str], list[str], dict[str, Any]]:
    """Remove unsafe treatment instructions and keep safety text in the requested language."""
    safety_text = {
        "en": {
            "blocked": "A potentially unsafe treatment instruction was removed; do not apply chemical or large-dose treatment based on this advisory alone.",
            "confirmation": "Potential disease, pest, or deficiency wording requires confirmation before action.",
            "reason": "Recommendations are limited to low-risk checks. Confirm diagnosis and treatment with a qualified local agronomist.",
        },
        "bn": {
            "blocked": "সম্ভাব্য অনিরাপদ চিকিৎসার নির্দেশনা সরিয়ে দেওয়া হয়েছে; এই পরামর্শের ভিত্তিতে একা কোনো রাসায়নিক বা বেশি মাত্রার চিকিৎসা প্রয়োগ করবেন না।",
            "confirmation": "রোগ, পোকা বা পুষ্টির ঘাটতির সম্ভাব্য উল্লেখ থাকলে পদক্ষেপ নেওয়ার আগে তা নিশ্চিত করা দরকার।",
            "reason": "পরামর্শগুলো কম ঝুঁকির পরীক্ষায় সীমাবদ্ধ। রোগ নির্ণয় ও চিকিৎসার সিদ্ধান্তের আগে যোগ্য স্থানীয় কৃষিবিদের পরামর্শ নিন।",
        },
        "hi": {
            "blocked": "संभावित असुरक्षित उपचार निर्देश हटा दिया गया है; केवल इस सलाह के आधार पर रसायन या अधिक मात्रा वाला उपचार न करें।",
            "confirmation": "रोग, कीट या पोषक तत्वों की कमी का संभावित उल्लेख है, इसलिए कार्रवाई से पहले इसकी पुष्टि जरूरी है।",
            "reason": "सलाह कम जोखिम वाली जाँच तक सीमित है। रोग की पुष्टि और उपचार के निर्णय से पहले योग्य स्थानीय कृषि विशेषज्ञ से सलाह लें।",
        },
        "ta": {
            "blocked": "சாத்தியமான பாதுகாப்பற்ற சிகிச்சை வழிமுறை நீக்கப்பட்டுள்ளது; இந்த ஆலோசனையை மட்டும் அடிப்படையாகக் கொண்டு ரசாயனங்கள் அல்லது அதிக அளவு சிகிச்சையைப் பயன்படுத்த வேண்டாம்.",
            "confirmation": "நோய், பூச்சி அல்லது ஊட்டச்சத்து குறைபாடு பற்றிய சாத்தியமான குறிப்புகள் உள்ளதால், நடவடிக்கை எடுப்பதற்கு முன் உறுதிப்படுத்த வேண்டும்.",
            "reason": "பரிந்துரைகள் குறைந்த ஆபத்துள்ள சோதனைகளுக்கு மட்டுமே வரையறுக்கப்பட்டுள்ளன. நோயறிதல் மற்றும் சிகிச்சை முடிவுக்கு முன் தகுதியான உள்ளூர் வேளாண் நிபுணரை அணுகவும்.",
        },
        "pa": {
            "blocked": "ਸੰਭਾਵੀ ਅਸੁਰੱਖਿਅਤ ਇਲਾਜ ਦੀ ਹਦਾਇਤ ਹਟਾ ਦਿੱਤੀ ਗਈ ਹੈ; ਸਿਰਫ਼ ਇਸ ਸਲਾਹ ਦੇ ਆਧਾਰ 'ਤੇ ਰਸਾਇਣ ਜਾਂ ਵੱਧ ਮਾਤਰਾ ਵਾਲਾ ਇਲਾਜ ਨਾ ਕਰੋ।",
            "confirmation": "ਬਿਮਾਰੀ, ਕੀੜੇ ਜਾਂ ਪੋਸ਼ਕ ਤੱਤਾਂ ਦੀ ਘਾਟ ਬਾਰੇ ਸੰਭਾਵੀ ਜ਼ਿਕਰ ਹੈ, ਇਸ ਲਈ ਕਾਰਵਾਈ ਤੋਂ ਪਹਿਲਾਂ ਪੁਸ਼ਟੀ ਕਰਨੀ ਲੋੜੀਂਦੀ ਹੈ।",
            "reason": "ਸਲਾਹ ਘੱਟ ਜੋਖਮ ਵਾਲੀਆਂ ਜਾਂਚਾਂ ਤੱਕ ਸੀਮਿਤ ਹੈ। ਬਿਮਾਰੀ ਦੀ ਪੁਸ਼ਟੀ ਅਤੇ ਇਲਾਜ ਦੇ ਫੈਸਲੇ ਤੋਂ ਪਹਿਲਾਂ ਯੋਗ ਸਥਾਨਕ ਖੇਤੀ ਮਾਹਿਰ ਨਾਲ ਸਲਾਹ ਕਰੋ।",
        },
        "te": {
            "blocked": "సంభావ్యంగా అసురక్షితమైన చికిత్స సూచన తొలగించబడింది; ఈ సలహాను మాత్రమే ఆధారంగా చేసుకుని రసాయనాలు లేదా అధిక మోతాదు చికిత్సను ఉపయోగించవద్దు.",
            "confirmation": "వ్యాధి, పురుగు లేదా పోషక లోపం గురించి సంభావ్య ప్రస్తావన ఉంది; చర్య తీసుకునే ముందు దాన్ని నిర్ధారించాలి.",
            "reason": "సలహాలు తక్కువ ప్రమాదం ఉన్న తనిఖీలకు మాత్రమే పరిమితం చేయబడ్డాయి. నిర్ధారణ మరియు చికిత్స నిర్ణయానికి ముందు అర్హత కలిగిన స్థానిక వ్యవసాయ నిపుణుడిని సంప్రదించండి.",
        },
    }
    table = safety_text.get(language, safety_text["en"])
    safe_recommendations: list[str] = []
    blocked = False

    for recommendation in recommendations:
        text = str(recommendation).strip()
        lowered = text.lower()
        if any(term in lowered for term in UNSAFE_ACTION_TERMS):
            blocked = True
            continue
        safe_recommendations.append(text)

    safe_uncertainties = [str(item) for item in uncertainties]
    if blocked:
        safe_uncertainties.append(table["blocked"])

    normalized_confidence = confidence if confidence in {"low", "medium", "high"} else "low"
    if normalized_confidence != "low" and any(
        term in " ".join(safe_recommendations).lower() for term in CAUTION_TERMS
    ):
        normalized_confidence = "low"
        safe_uncertainties.append(table["confirmation"])

    status = "caution"
    return safe_recommendations, safe_uncertainties, build_safety(status, table["reason"])
