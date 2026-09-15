"""Conservative normalization for multilingual farmer terminology.

Maps high-confidence farmer/ASR variants to canonical English agronomy terms.
Unknown wording is preserved and is never guessed as a pest or disease.
"""

import re


TERM_MAP: dict[str, tuple[str, ...]] = {
    "yellow leaf": (
        "yellow leaf", "yellow leaves", "yellowing", "leaves yellow",
        "হলুদ পাতা", "পাতা হলুদ", "পাতা হলুদ হওয়া", "পাতা হলুদ হওয়া",
        "হলুদ হয়ে যাচ্ছে", "হলুদ হয়ে যাচ্ছে", "হোলুদ হয়ে যাচ্ছে", "হোলুদ হয়ে যাচ্ছে",
        "হলুদ হোয়ে জাছে", "হোলুদ হোয়ে জাছে", "হলুদ হয়ে গেছে", "হলুদ হয়ে গেছে",
        "হোলুদ হয়ে গেছে", "হোলুদ হয়ে গেছে", "হলুদ হচ্ছে", "হোলুদ হচ্ছে",
        "পাতা হোলুদ", "পাতা হোলুদ হচ্ছে", "পাতা হলুদ হচ্ছে",
        "হলুদ রঙের পাতা", "হলুদ রঙের পাতাগুলো", "হলুদ রঙের পাতা দেখা যাচ্ছে",
        "पीले पत्ते", "पत्ते पीले", "पत्ते पीले हो रहे", "पत्तियां पीली", "पत्तियाँ पीली",
        "पत्ते पीले हो रहे हैं", "पत्तियां पीली हो रही हैं", "पत्तियाँ पीली हो रही हैं",
        "இலை மஞ்சள்", "மஞ்சள் இலை", "இலைகள் மஞ்சள்", "இலை மஞ்சளாகிறது", "இலைகள் மஞ்சளாகின்றன",
        "ਪੱਤੇ ਪੀਲੇ", "ਪੀਲੇ ਪੱਤੇ", "ਪੱਤੇ ਪੀਲੇ ਹੋ ਰਹੇ", "ਪੱਤੇ ਪੀਲੇ ਹੋ ਰਹੇ ਹਨ",
        "பੱத", "ఆకులు పసుపు", "పసుపు ఆకులు", "ఆకులు పసుపుగా మారుతున్నాయి", "ఆకులు పసుపు అవుతున్నాయి",
    ),
    "wilting": (
        "wilting", "wilt", "drooping", "droop", "plant wilting", "plants wilting",
        "গাছ ঢলে", "গাছ নুইয়ে", "গাছ নুইয়ে", "গাছ ঝিমিয়ে", "গাছ ঝিমিয়ে", "ঝিমিয়ে যাচ্ছে", "ঝিমিয়ে যাচ্ছে",
        "গাছ মুরঝে", "গাছ মুরঝিয়ে", "গাছ মুরঝিয়ে", "গাছ শুকিয়ে যাচ্ছে", "গাছ শুকিয়ে যাচ্ছে",
        "मुरझाना", "मुरझाए", "पौधा मुरझा", "पौधे मुरझा", "पौधा झुक रहा", "पौधे झुक रहे",
        "पौधा मुरझा रहा है", "पौधे मुरझा रहे हैं", "पौधा सूख रहा है",
        "இலை வாடல்", "வாடுதல்", "செடி வாடுகிறது", "செடிகள் வாடுகின்றன", "செடி வாடி வருகிறது",
        "பயிர் வாடுகிறது", "பயிர் வாடி வருகிறது",
        "ਪੌਦਾ ਮੁਰਝਾ", "ਮੁਰਝਾਉਣਾ", "ਪੌਦਾ ਮੁਰਝਾ ਰਿਹਾ", "ਪੌਦੇ ਮੁਰਝਾ ਰਹੇ", "ਫਸਲ ਮੁਰਝਾ ਰਹੀ",
        "ਮੁਰਝਾ ਰਿਹਾ ਹੈ", "ਮੁਰਝਾ ਰਹੇ ਹਨ",
        "మొక్క వాడిపోవడం", "వాడిపోవడం", "మొక్క వాడిపోతోంది", "మొక్కలు వాడిపోతున్నాయి",
        "పంట వాడిపోతోంది", "మొక్క వంగిపోతోంది",
    ),
    "leaf spot": (
        "leaf spot", "leaf spots", "spots", "spot", "brown spots", "black spots", "spots on leaves",
        "পাতায় দাগ", "পাতায় দাগ", "পাতায় বাদামি দাগ", "পাতায় বাদামি দাগ", "পাতায় কালো দাগ", "পাতায় কালো দাগ",
        "পাতায় দাগ হয়েছে", "পাতায় দাগ হয়েছে", "পাতায় ছোপ", "পাতায় ছোপ",
        "पत्तियों पर धब्बे", "पत्ते पर दाग", "पत्तियों पर दाग", "पत्ते पर धब्बे", "भूरे धब्बे", "काले धब्बे",
        "पत्तियों पर भूरे धब्बे", "पत्तियों पर काले धब्बे",
        "இலையில் புள்ளி", "இலை புள்ளி", "இலைகளில் புள்ளி", "இலையில் கருப்பு புள்ளி", "இலையில் பழுப்பு புள்ளி",
        "இலைகளில் கருப்பு புள்ளிகள்", "இலைகளில் பழுப்பு புள்ளிகள்",
        "ਪੱਤਿਆਂ ਤੇ ਧੱਬੇ", "ਪੱਤਿਆਂ ਉੱਤੇ ਧੱਬੇ", "ਪੱਤੇ ਤੇ ਦਾਗ", "ਪੱਤਿਆਂ ਤੇ ਦਾਗ", "ਭੂਰੇ ਧੱਬੇ", "ਕਾਲੇ ਧੱਬੇ",
        "ਪੱਤਿਆਂ ਉੱਤੇ ਭੂਰੇ ਧੱਬੇ", "ਪੱਤਿਆਂ ਉੱਤੇ ਕਾਲੇ ਧੱਬੇ",
        "ఆకులపై మచ్చలు", "ఆకులపై మచ్చ", "ఆకుల మీద మచ్చలు", "ఆకులపై నల్ల మచ్చలు", "ఆకులపై గోధుమ మచ్చలు",
        "ఆకుల మీద నల్ల మచ్చలు", "ఆకుల మీద గోధుమ మచ్చలు",
        "ఆకులపై మచ్చలు ఉన్నాయి", "ఆకులపై మచ్చ ఉన్నాయి", "ఆకుల మీద మచ్చలు ఉన్నాయి",
        "ఆకులపై నల్ల మచ్చలు ఉన్నాయి", "ఆకులపై గోధుమ మచ్చలు ఉన్నాయి",
    ),
}


def normalize_agricultural_terms(text: str, language: str) -> tuple[str, list[str]]:
    """Normalize high-confidence known terms without guessing unknown terms."""
    normalized = text.strip()
    matched: list[str] = []

    for canonical, variants in TERM_MAP.items():
        for variant in sorted(variants, key=len, reverse=True):
            if variant.lower() in normalized.lower():
                replacement = canonical
                if canonical == "leaf spot" and language == "te":
                    replacement = "leaf spots"
                normalized = re.sub(re.escape(variant), replacement, normalized, flags=re.IGNORECASE)
                if canonical not in matched:
                    matched.append(canonical)
                if canonical == "leaf spot" and "leaf spots" not in matched:
                    matched.append("leaf spots")
                if canonical == "yellow leaf" and "yellow leaves" not in matched:
                    matched.append("yellow leaves")
                break

    return normalized, matched
