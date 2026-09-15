"""Conservative normalization for common multilingual farmer terminology.

The normalizer maps known farmer/ASR variants to canonical agronomy terms.
Unknown wording is preserved and is never guessed as a pest or disease.
"""

import re


TERM_MAP: dict[str, tuple[str, ...]] = {
    "yellow leaves": (
        "yellow leaf", "yellow leaves", "yellowing",
        "হলুদ পাতা", "পাতা হলুদ", "পাতা হলুদ হওয়া", "পাতা হলুদ হওয়া",
        "হলুদ হয়ে যাচ্ছে", "হলুদ হয়ে যাচ্ছে", "হোলুদ হয়ে যাচ্ছে", "হোলুদ হয়ে যাচ্ছে",
        "হলুদ হয়ে গেছে", "হলুদ হয়ে গেছে", "হোলুদ হয়ে গেছে", "হোলুদ হয়ে গেছে",
        "হলুদ হচ্ছে", "হোলুদ হচ্ছে", "হলুদ হইতেছে", "হোলুদ হইতেছে",
        "पीले पत्ते", "पत्ते पीले", "இலை மஞ்சள்", "மஞ்சள் இலை",
        "ਪੱਤੇ ਪੀਲੇ", "ਪੀਲੇ ਪੱਤੇ", "ఆకులు పసుపు", "పసుపు ఆకులు",
    ),
    "wilting": (
        "wilting", "wilt", "drooping", "droop",
        "গাছ ঢলে", "গাছ নুইয়ে", "গাছ নুইয়ে", "ঝিমিয়ে", "ঝিমিয়ে",
        "মুরঝে", "মুরঝিয়ে", "মুরঝিয়ে",
        "मुरझाना", "मुरझाए", "இலை வாடல்", "வாடுதல்",
        "ਪੌਦਾ ਮੁਰਝਾ", "ਮੁਰਝਾਉਣਾ", "మొక్క వాడిపోవడం", "వాడిపోవడం",
    ),
    "leaf spot": (
        "leaf spot", "leaf spots", "spots", "spot", "brown spots", "black spots",
        "পাতায় দাগ", "পাতায় দাগ", "পাতায় বাদামি দাগ", "পাতায় বাদামি দাগ",
        "पत्तियों पर धब्बे", "இலை புள்ளி", "இலையில் புள்ளி",
        "ਪੱਤਿਆਂ ਤੇ ਧੱਬੇ", "ਆਕੁਲਪਾਈ ਮੱਚਲু", "ఆకులపై మచ్చలు", "ఆకులపై మచ్చ",
    ),
}


def normalize_agricultural_terms(text: str, language: str) -> tuple[str, list[str]]:
    """Normalize high-confidence known terms without guessing unknown terms."""
    normalized = text.strip()
    matched: list[str] = []

    for canonical, variants in TERM_MAP.items():
        for variant in sorted(variants, key=len, reverse=True):
            if variant.lower() in normalized.lower():
                normalized = re.sub(re.escape(variant), canonical, normalized, flags=re.IGNORECASE)
                if canonical not in matched:
                    matched.append(canonical)
                break

    return normalized, matched
