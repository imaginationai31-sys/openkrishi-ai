"""Conservative normalization for common multilingual farmer terminology.

The normalizer only maps known words/phrases to canonical English agronomy
terms. It does not infer an unknown pest or disease from an uncertain ASR
transcript.
"""

import re


TERM_MAP: dict[str, tuple[str, ...]] = {
    "yellow leaves": ("yellow leaf", "yellow leaves", "yellowing"),
    "wilting": ("wilting", "wilt", "drooping", "droop"),
    "leaf spots": ("leaf spot", "spots", "spot", "brown spots", "black spots"),
    "yellow leaf": ("হলুদ পাতা", "পাতা হলুদ", "পাতা হলুদ হওয়া", "पीले पत्ते", "पत्ते पीले", "இலை மஞ்சள்", "மஞ்சள் இலை", "ਪੱਤੇ ਪੀਲੇ", "ਪੀਲੇ ਪੱਤੇ", "ఆకులు పసుపు", "పసుపు ఆకులు"),
    "wilting": ("গাছ ঢলে", "গাছ নুইয়ে", "ঝিমিয়ে", "मुरझाना", "मुरझाए", "இலை வாடல்", "வாடுதல்", "ਪੌਦਾ ਮੁਰਝਾ", "ਮੁਰਝਾਉਣਾ", "మొక్క వాడిపోవడం", "వాడిపోవడం"),
    "leaf spot": ("পাতায় দাগ", "পাতায় দাগ", "पत्तियों पर धब्बे", "இலை புள்ளி", "இலையில் புள்ளி", "ਪੱਤਿਆਂ ਤੇ ਧੱਬੇ", "ఆకులపై మచ్చలు", "ఆకులపై మచ్చ"),
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
