"""Conservative multilingual advisory engine with a small crop-knowledge layer."""

import json
import os
import logging
from typing import Any

from .knowledge import get_knowledge
from .safety import enforce_advisory_safety
from services.gemini.client import get_gemini_client, get_model, output_text

logger = logging.getLogger(__name__)

SUPPORTED_LANGUAGES = {"en", "bn", "hi", "ta", "pa", "te"}

CROP_LABELS = {
    "rice": {"en": "rice", "bn": "ধান", "hi": "धान", "ta": "நெல்", "pa": "ਝੋਨਾ", "te": "వరి"},
    "peanut": {"en": "peanut", "bn": "বাদাম", "hi": "मूंगफली", "ta": "நிலக்கடலை", "pa": "ਮੂੰਗਫਲੀ", "te": "వేరుశెనగ"},
    "vegetables": {"en": "vegetables", "bn": "সবজি", "hi": "सब्जियाँ", "ta": "காய்கறிகள்", "pa": "ਸਬਜ਼ੀਆਂ", "te": "కూరగాయలు"},
    "flowers": {"en": "flowers", "bn": "ফুল", "hi": "फूल", "ta": "மலர்கள்", "pa": "ਫੁੱਲ", "te": "పూలు"},
}

LOCALIZED = {
    "en": {
        "yellow1": "Check whether yellowing starts on older or newer leaves and whether it is uniform or patchy.",
        "yellow2": "Check soil moisture and drainage before changing irrigation or adding fertilizer.",
        "yellow3": "Inspect both sides of leaves and the plant base for insects, lesions, or other visible signs.",
        "yellow_unc": "Leaf color alone is not enough to identify the cause; growth stage, field conditions, and visible symptoms are needed.",
    },
    "bn": {
        "yellow1": "পাতা হলুদ হওয়া পুরনো পাতায় নাকি নতুন পাতায় শুরু হয়েছে এবং সমানভাবে নাকি ছোপ ছোপ হচ্ছে তা দেখুন।",
        "yellow2": "সেচের পরিমাণ পরিবর্তন বা সার দেওয়ার আগে মাটির আর্দ্রতা ও জল নিষ্কাশন পরীক্ষা করুন।",
        "yellow3": "পাতার দুই দিক এবং গাছের গোড়ায় পোকা, ক্ষত বা অন্য কোনো দৃশ্যমান লক্ষণ আছে কি না দেখুন।",
        "yellow_unc": "শুধু পাতার রং দেখে কারণ নিশ্চিত করা যায় না; বৃদ্ধির পর্যায়, জমির অবস্থা এবং দৃশ্যমান লক্ষণও দরকার।",
    },
    "hi": {
        "yellow1": "देखें कि पत्तियों का पीलापन पुरानी या नई पत्तियों से शुरू हुआ है और यह पूरे पौधे में समान है या जगह-जगह है।",
        "yellow2": "सिंचाई बदलने या खाद डालने से पहले मिट्टी की नमी और जल निकासी की जाँच करें।",
        "yellow3": "पत्तियों के दोनों तरफ और पौधे के आधार पर कीड़े, घाव या अन्य दिखाई देने वाले लक्षण देखें।",
        "yellow_unc": "केवल पत्तियों का रंग देखकर कारण की पुष्टि नहीं की जा सकती; फसल की अवस्था, खेत की स्थिति और दिखाई देने वाले लक्षण भी जरूरी हैं।",
    },
    "ta": {
        "yellow1": "இலைகள் மஞ்சளாகுவது பழைய இலைகளிலா அல்லது புதிய இலைகளிலா தொடங்குகிறது என்பதையும், அது சீராகவா அல்லது திட்டுத் திட்டாகவா உள்ளது என்பதையும் பாருங்கள்.",
        "yellow2": "நீர்ப்பாசனத்தை மாற்றுவதற்கு அல்லது உரம் இடுவதற்கு முன் மண்ணின் ஈரப்பதம் மற்றும் வடிகால் நிலையைச் சரிபார்க்கவும்.",
        "yellow3": "இலைகளின் இருபுறமும் மற்றும் செடியின் அடிப்பகுதியிலும் பூச்சிகள், காயங்கள் அல்லது பிற தெளிவான அறிகுறிகள் உள்ளதா என்று பாருங்கள்.",
        "yellow_unc": "இலையின் நிறத்தை மட்டும் வைத்து காரணத்தை உறுதி செய்ய முடியாது; வளர்ச்சி நிலை, வயல் நிலை மற்றும் தெளிவான அறிகுறிகள் தேவை.",
    },
    "pa": {
        "yellow1": "ਦੇਖੋ ਕਿ ਪੱਤਿਆਂ ਦਾ ਪੀਲਾਪਣ ਪੁਰਾਣੇ ਜਾਂ ਨਵੇਂ ਪੱਤਿਆਂ ਤੋਂ ਸ਼ੁਰੂ ਹੁੰਦਾ ਹੈ ਅਤੇ ਇਹ ਇਕਸਾਰ ਹੈ ਜਾਂ ਥਾਂ-ਥਾਂ ਹੈ।",
        "yellow2": "ਸਿੰਚਾਈ ਬਦਲਣ ਜਾਂ ਖਾਦ ਪਾਉਣ ਤੋਂ ਪਹਿਲਾਂ ਮਿੱਟੀ ਦੀ ਨਮੀ ਅਤੇ ਪਾਣੀ ਦੀ ਨਿਕਾਸੀ ਦੀ ਜਾਂਚ ਕਰੋ।",
        "yellow3": "ਪੱਤਿਆਂ ਦੇ ਦੋਵੇਂ ਪਾਸਿਆਂ ਅਤੇ ਪੌਦੇ ਦੇ ਹੇਠਲੇ ਹਿੱਸੇ ਵਿੱਚ ਕੀੜੇ, ਜ਼ਖਮ ਜਾਂ ਹੋਰ ਦਿਖਾਈ ਦੇਣ ਵਾਲੇ ਲੱਛਣ ਵੇਖੋ।",
        "yellow_unc": "ਸਿਰਫ਼ ਪੱਤਿਆਂ ਦੇ ਰੰਗ ਨਾਲ ਕਾਰਨ ਦੀ ਪੁਸ਼ਟੀ ਨਹੀਂ ਕੀਤੀ ਜਾ ਸਕਦੀ; ਫਸਲ ਦੀ ਅਵਸਥਾ, ਖੇਤ ਦੀ ਸਥਿਤੀ ਅਤੇ ਦਿਖਾਈ ਦੇਣ ਵਾਲੇ ਲੱਛਣ ਵੀ ਲੋੜੀਂਦੇ ਹਨ।",
    },
    "te": {
        "yellow1": "ఆకులు పసుపు రంగులోకి మారడం పాత ఆకులలోనా లేదా కొత్త ఆకులలోనా మొదలైందో, అలాగే అది మొత్తం సమానంగా ఉందో లేదా మచ్చలుగా ఉందో చూడండి.",
        "yellow2": "నీటి పారుదల మార్చే ముందు లేదా ఎరువు వేసే ముందు నేల తేమ మరియు నీటి పారుదల పరిస్థితిని పరిశీలించండి.",
        "yellow3": "ఆకుల రెండు వైపులా మరియు మొక్క అడుగు భాగంలో పురుగులు, గాయాలు లేదా ఇతర కనిపించే లక్షణాలు ఉన్నాయా చూడండి.",
        "yellow_unc": "ఆకుల రంగును మాత్రమే ఆధారంగా చేసుకుని కారణాన్ని నిర్ధారించలేము; పంట దశ, పొలం పరిస్థితులు మరియు కనిపించే లక్షణాలు కూడా అవసరం.",
    },
}


def _localize_list(items: list[str], language: str) -> list[str]:
    table = LOCALIZED.get(language, LOCALIZED["en"])
    replacements = {
        "Check whether yellowing starts on older or newer leaves and whether it is uniform or patchy.": "yellow1",
        "Check soil moisture and drainage before changing irrigation or adding fertilizer.": "yellow2",
        "Inspect both sides of leaves and the plant base for insects, lesions, or other visible signs.": "yellow3",
        "Leaf color alone is not enough to identify the cause; growth stage, field conditions, and visible symptoms are needed.": "yellow_unc",
    }
    return [table.get(replacements.get(item, ""), item) for item in items]


GEMINI_ADVISORY_SCHEMA = {
    "type": "object",
    "properties": {
        "answer": {"type": "string"},
        "observations": {"type": "array", "items": {"type": "string"}},
        "recommendations": {"type": "array", "items": {"type": "string"}},
        "uncertainties": {"type": "array", "items": {"type": "string"}},
        "confidence": {"type": "string", "enum": ["low", "medium", "high"]},
    },
    "required": ["answer", "observations", "recommendations", "uncertainties", "confidence"],
}


def _generate_gemini_advisory(
    query: str,
    language: str,
    crop_category: str | None = None,
    crop_name: str | None = None,
    growth_stage: str | None = None,
    location: str | None = None,
) -> dict[str, Any]:
    """Generate the farmer-facing advisory with Gemini when GEMINI_API_KEY is configured."""
    language_names = {
        "en": "English",
        "bn": "Bengali",
        "hi": "Hindi",
        "ta": "Tamil",
        "pa": "Punjabi",
        "te": "Telugu",
    }
    language_name = language_names[language]
    crop_label = CROP_LABELS.get(crop_category or "", {}).get("en") or crop_category or "unspecified crop"
    location_text = location.strip() if location else "not provided"
    growth_text = growth_stage.strip() if growth_stage else "not provided"
    crop_text = crop_name.strip() if crop_name else "not specified"

    prompt = f"""You are OpenKrishi AI, a cautious agricultural advisory assistant for Indian farmers.

Answer the farmer's question using the supplied context. This is advisory guidance, not a definitive diagnosis.

FARMER QUERY:
{query}

CROP CATEGORY: {crop_label}
SPECIFIC CROP OR VARIETY: {crop_text}
GROWTH STAGE: {growth_text}
LOCATION: {location_text}

STRICT SAFETY RULES:
- Do not claim a disease, pest, nutrient deficiency, or other diagnosis as certain.
- Describe possible causes only when supported by the farmer's information.
- Do not prescribe pesticides, insecticides, fungicides, herbicides, chemical sprays, chemical names, application rates, or large fertilizer doses.
- Give practical low-risk checks and next steps.
- If important information is missing, state exactly what should be checked or provided.
- If growth stage is missing, explicitly mention that the growth stage is needed for more specific guidance.
- If location is provided, acknowledge it but do not invent local weather, soil, pest alerts, or government guidance.
- If location is not provided, state that local conditions cannot be considered.
- Keep confidence conservative; use low unless the information is unusually clear.
- Return ONLY JSON matching the supplied response schema.
- Every natural-language field MUST be fully written in {language_name}. Do not mix languages.
- Keep crop and variety names recognizable to farmers.

Return concise but useful observations, safe recommendations, and uncertainties."""

    from google.genai import types

    try:
        client = get_gemini_client()
        response = client.models.generate_content(
            model=get_model(),
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                temperature=0.2,
                max_output_tokens=1200,
            ),
        )
        raw = getattr(response, "text", None) or output_text(response)
        payload = json.loads(raw)
        if not isinstance(payload, dict):
            raise RuntimeError("Gemini returned an invalid advisory format.")
    except Exception as exc:
        logger.exception("Gemini advisory request failed: model=%s language=%s crop=%s error_type=%s error=%s", get_model(), language, crop_category, type(exc).__name__, exc)
        raise RuntimeError("Gemini advisory provider is temporarily unavailable.") from exc

    answer = str(payload.get("answer") or "").strip()
    observations = [str(x).strip() for x in payload.get("observations", []) if str(x).strip()][:8]
    recommendations = [str(x).strip() for x in payload.get("recommendations", []) if str(x).strip()][:8]
    uncertainties = [str(x).strip() for x in payload.get("uncertainties", []) if str(x).strip()][:8]
    confidence = payload.get("confidence") if payload.get("confidence") in {"low", "medium", "high"} else "low"

    if not answer:
        raise RuntimeError("Gemini returned an empty advisory.")

    if location:
        location_note = {
            "en": f"Use the supplied location ({location.strip()}) when checking local agricultural extension or agronomy guidance; no local conditions are assumed here.",
            "bn": f"স্থানীয় কৃষি দপ্তর বা কৃষিবিদদের পরামর্শ দেখার সময় দেওয়া অবস্থান ({location.strip()}) ব্যবহার করুন; এখানে কোনো স্থানীয় পরিস্থিতি ধরে নেওয়া হয়নি।",
            "hi": f"स्थानीय कृषि विभाग या कृषि विशेषज्ञ की सलाह देखते समय दिए गए स्थान ({location.strip()}) का उपयोग करें; यहाँ स्थानीय परिस्थितियाँ मानकर नहीं चली गई हैं।",
            "ta": f"உள்ளூர் வேளாண்மை துறை அல்லது வேளாண் நிபுணர் ஆலோசனையைப் பார்க்கும்போது வழங்கப்பட்ட இடத்தை ({location.strip()}) பயன்படுத்தவும்; இங்கு உள்ளூர் நிலைமைகள் கருதப்படவில்லை.",
            "pa": f"ਸਥਾਨਕ ਖੇਤੀਬਾੜੀ ਵਿਭਾਗ ਜਾਂ ਖੇਤੀ ਮਾਹਿਰ ਦੀ ਸਲਾਹ ਵੇਖਦੇ ਸਮੇਂ ਦਿੱਤੀ ਥਾਂ ({location.strip()}) ਦੀ ਵਰਤੋਂ ਕਰੋ; ਇੱਥੇ ਸਥਾਨਕ ਹਾਲਾਤ ਨਹੀਂ ਮੰਨੇ ਗਏ।",
            "te": f"స్థానిక వ్యవసాయ శాఖ లేదా వ్యవసాయ నిపుణుల సలహాను పరిశీలించేటప్పుడు ఇచ్చిన ప్రదేశాన్ని ({location.strip()}) ఉపయోగించండి; ఇక్కడ స్థానిక పరిస్థితులను ఊహించలేదు.",
        }[language]
        if not any(location.strip().lower() in item.lower() for item in recommendations):
            recommendations.insert(0, location_note)
        uncertainties.append({
            "en": "This advisory does not retrieve live local weather, soil, pest alerts, or region-specific agronomy data.",
            "bn": "এই পরামর্শে স্থানীয় আবহাওয়া, মাটি, পোকামাকড়ের সতর্কতা বা অঞ্চলভিত্তিক কৃষি তথ্যের লাইভ তথ্য ব্যবহার করা হয়নি।",
            "hi": "इस सलाह में स्थानीय मौसम, मिट्टी, कीट चेतावनी या क्षेत्र-विशिष्ट कृषि जानकारी का लाइव डेटा उपयोग नहीं किया गया है।",
            "ta": "இந்த ஆலோசனையில் உள்ளூர் வானிலை, மண், பூச்சி எச்சரிக்கைகள் அல்லது பகுதி சார்ந்த வேளாண்மைத் தகவலின் நேரடி தரவு பயன்படுத்தப்படவில்லை.",
            "pa": "ਇਸ ਸਲਾਹ ਵਿੱਚ ਸਥਾਨਕ ਮੌਸਮ, ਮਿੱਟੀ, ਕੀੜਿਆਂ ਦੀ ਚੇਤਾਵਨੀ ਜਾਂ ਖੇਤਰ-ਵਿਸ਼ੇਸ਼ ਖੇਤੀਬਾੜੀ ਜਾਣਕਾਰੀ ਦਾ ਲਾਈਵ ਡਾਟਾ ਨਹੀਂ ਵਰਤਿਆ ਗਿਆ।",
            "te": "ఈ సలహాలో స్థానిక వాతావరణం, నేల, పురుగు హెచ్చరికలు లేదా ప్రాంతానికి సంబంధించిన వ్యవసాయ సమాచారపు ప్రత్యక్ష డేటాను ఉపయోగించలేదు.",
        }[language])
    else:
        uncertainties.append({
            "en": "Location was not provided; local weather, soil, pest pressure, and regional agronomy guidance cannot be considered.",
            "bn": "অবস্থান দেওয়া হয়নি; স্থানীয় আবহাওয়া, মাটি, পোকার চাপ এবং অঞ্চলভিত্তিক কৃষি পরামর্শ বিবেচনা করা যাচ্ছে না।",
            "hi": "स्थान नहीं दिया गया है; इसलिए स्थानीय मौसम, मिट्टी, कीट दबाव और क्षेत्रीय कृषि सलाह पर विचार नहीं किया जा सकता।",
            "ta": "இடம் வழங்கப்படவில்லை; எனவே உள்ளூர் வானிலை, மண், பூச்சி தாக்கம் மற்றும் பகுதி சார்ந்த வேளாண்மை ஆலோசனையை கருத்தில் கொள்ள முடியாது.",
            "pa": "ਥਾਂ ਨਹੀਂ ਦਿੱਤੀ ਗਈ; ਇਸ ਲਈ ਸਥਾਨਕ ਮੌਸਮ, ਮਿੱਟੀ, ਕੀੜਿਆਂ ਦੇ ਦਬਾਅ ਅਤੇ ਖੇਤਰੀ ਖੇਤੀਬਾੜੀ ਸਲਾਹ ਨੂੰ ਧਿਆਨ ਵਿੱਚ ਨਹੀਂ ਰੱਖਿਆ ਜਾ ਸਕਦਾ।",
            "te": "ప్రదేశం ఇవ్వలేదు; కాబట్టి స్థానిక వాతావరణం, నేల, పురుగు ప్రభావం మరియు ప్రాంతీయ వ్యవసాయ సలహాను పరిగణనలోకి తీసుకోలేము.",
        }[language])

    recommendations, uncertainties, safety = enforce_advisory_safety(
        recommendations, uncertainties, confidence=confidence
    )

    return {
        "answer": answer,
        "language": language,
        "confidence": "low" if safety["status"] == "caution" else confidence,
        "safety": safety,
        "observations": observations,
        "recommendations": recommendations,
        "uncertainties": uncertainties,
        "source_references": [],
        "location": location.strip() if location else None,
        "crop_category": crop_category,
        "crop_name": crop_name,
    }


def generate_advisory(
    query: str,
    language: str,
    crop_category: str | None = None,
    crop_name: str | None = None,
    growth_stage: str | None = None,
    location: str | None = None,
) -> dict[str, Any]:
    """Use Gemini for production advisories, with deterministic fallback before a key is configured."""
    if language not in SUPPORTED_LANGUAGES:
        language = "en"

    if os.getenv("GEMINI_API_KEY"):
        try:
            return _generate_gemini_advisory(
                query=query,
                language=language,
                crop_category=crop_category,
                crop_name=crop_name,
                growth_stage=growth_stage,
                location=location,
            )
        except RuntimeError:
            if os.getenv("GEMINI_FALLBACK_TO_RULES", "false").lower() != "true":
                raise

    return _generate_rule_based_advisory(
        query=query,
        language=language,
        crop_category=crop_category,
        crop_name=crop_name,
        growth_stage=growth_stage,
        location=location,
    )


def _generate_rule_based_advisory(
    query: str,
    language: str,
    crop_category: str | None = None,
    crop_name: str | None = None,
    growth_stage: str | None = None,
    location: str | None = None,
) -> dict[str, Any]:
    if language not in SUPPORTED_LANGUAGES:
        language = "en"

    crop_label = CROP_LABELS.get(crop_category or "", {}).get(language)
    crop_display = f"{crop_label} ({crop_name})" if crop_label and crop_name else crop_label
    location_value = location.strip() if location else None
    location_value = location_value or None

    knowledge_query = f"{crop_name}: {query}" if crop_name else query
    knowledge = get_knowledge(knowledge_query, crop_category, growth_stage)
    recommendations = _localize_list(list(knowledge["recommendations"]), language)
    uncertainties = _localize_list(list(knowledge["uncertainties"]), language)
    observations = _localize_list(list(knowledge["observations"]), language)

    if location_value:
        recommendations.insert(0, {
            "en": f"Use the supplied location ({location_value}) when checking local agricultural extension or agronomy guidance; no local conditions are assumed by this MVP.",
            "bn": f"স্থানীয় কৃষি দপ্তর বা কৃষিবিদদের পরামর্শ দেখার সময় দেওয়া অবস্থান ({location_value}) ব্যবহার করুন; এই MVP নিজে থেকে স্থানীয় পরিস্থিতি ধরে নিচ্ছে না।",
            "hi": f"स्थानीय कृषि विभाग या कृषि विशेषज्ञ की सलाह देखते समय दिए गए स्थान ({location_value}) का उपयोग करें; यह MVP अपने आप स्थानीय परिस्थितियाँ नहीं मानता है।",
            "ta": f"உள்ளூர் வேளாண்மை துறை அல்லது வேளாண் நிபுணர் ஆலோசனையைப் பார்க்கும்போது வழங்கப்பட்ட இடத்தை ({location_value}) பயன்படுத்தவும்; இந்த MVP உள்ளூர் நிலைமைகளை தானாகக் கருதாது.",
            "pa": f"ਸਥਾਨਕ ਖੇਤੀਬਾੜੀ ਵਿਭਾਗ ਜਾਂ ਖੇਤੀ ਮਾਹਿਰ ਦੀ ਸਲਾਹ ਵੇਖਦੇ ਸਮੇਂ ਦਿੱਤੀ ਗਈ ਥਾਂ ({location_value}) ਦੀ ਵਰਤੋਂ ਕਰੋ; ਇਹ MVP ਆਪਣੇ ਆਪ ਸਥਾਨਕ ਹਾਲਾਤ ਨਹੀਂ ਮੰਨਦਾ।",
            "te": f"స్థానిక వ్యవసాయ శాఖ లేదా వ్యవసాయ నిపుణుల సలహాను పరిశీలించేటప్పుడు ఇచ్చిన ప్రదేశాన్ని ({location_value}) ఉపయోగించండి; ఈ MVP స్థానిక పరిస్థితులను స్వయంగా పరిగణించదు.",
        }[language])
        uncertainties.append({
            "en": "Location was provided, but this MVP does not yet retrieve live local weather, soil, pest alerts, or region-specific agronomy data.",
            "bn": "অবস্থান দেওয়া হয়েছে, তবে এই MVP এখনও স্থানীয় আবহাওয়া, মাটি, পোকামাকড়ের সতর্কতা বা অঞ্চলভিত্তিক কৃষি তথ্য সংগ্রহ করে না।",
            "hi": "स्थान दिया गया है, लेकिन यह MVP अभी स्थानीय मौसम, मिट्टी, कीट चेतावनी या क्षेत्र-विशिष्ट कृषि जानकारी प्राप्त नहीं करता है।",
            "ta": "இடம் வழங்கப்பட்டுள்ளது, ஆனால் இந்த MVP இன்னும் உள்ளூர் வானிலை, மண், பூச்சி எச்சரிக்கைகள் அல்லது பகுதி சார்ந்த வேளாண்மைத் தகவலைப் பெறவில்லை.",
            "pa": "ਥਾਂ ਦਿੱਤੀ ਗਈ ਹੈ, ਪਰ ਇਹ MVP ਹਾਲੇ ਸਥਾਨਕ ਮੌਸਮ, ਮਿੱਟੀ, ਕੀੜਿਆਂ ਦੀ ਚੇਤਾਵਨੀ ਜਾਂ ਖੇਤਰ-ਵਿਸ਼ੇਸ਼ ਖੇਤੀਬਾੜੀ ਜਾਣਕਾਰੀ ਪ੍ਰਾਪਤ ਨਹੀਂ ਕਰਦਾ।",
            "te": "ప్రదేశం ఇచ్చారు, కానీ ఈ MVP ఇంకా స్థానిక వాతావరణం, నేల, పురుగు హెచ్చరికలు లేదా ప్రాంతానికి సంబంధించిన వ్యవసాయ సమాచారాన్ని పొందదు.",
        }[language])
    else:
        uncertainties.append({
            "en": "Location was not provided; local weather, soil, pest pressure, and regional agronomy guidance cannot be considered.",
            "bn": "অবস্থান দেওয়া হয়নি; স্থানীয় আবহাওয়া, মাটি, পোকার চাপ এবং অঞ্চলভিত্তিক কৃষি পরামর্শ বিবেচনা করা যাচ্ছে না।",
            "hi": "स्थान नहीं दिया गया है; इसलिए स्थानीय मौसम, मिट्टी, कीट दबाव और क्षेत्रीय कृषि सलाह पर विचार नहीं किया जा सकता।",
            "ta": "இடம் வழங்கப்படவில்லை; எனவே உள்ளூர் வானிலை, மண், பூச்சி தாக்கம் மற்றும் பகுதி சார்ந்த வேளாண்மை ஆலோசனையை கருத்தில் கொள்ள முடியாது.",
            "pa": "ਥਾਂ ਨਹੀਂ ਦਿੱਤੀ ਗਈ; ਇਸ ਲਈ ਸਥਾਨਕ ਮੌਸਮ, ਮਿੱਟੀ, ਕੀੜਿਆਂ ਦੇ ਦਬਾਅ ਅਤੇ ਖੇਤਰੀ ਖੇਤੀਬਾੜੀ ਸਲਾਹ ਨੂੰ ਧਿਆਨ ਵਿੱਚ ਨਹੀਂ ਰੱਖਿਆ ਜਾ ਸਕਦਾ।",
            "te": "ప్రదేశం ఇవ్వలేదు; కాబట్టి స్థానిక వాతావరణం, నేల, పురుగు ప్రభావం మరియు ప్రాంతీయ వ్యవసాయ సలహాను పరిగణనలోకి తీసుకోలేము.",
        }[language])

    recommendations, uncertainties, safety = enforce_advisory_safety(
        recommendations, uncertainties, confidence="low"
    )

    if crop_label:
        answer = {
            "en": f"I understand your question about {crop_display}. I found a possible symptom match and can provide conservative checks based on the information supplied.",
            "bn": f"{crop_display} নিয়ে আপনার প্রশ্নটি বুঝেছি। দেওয়া তথ্যের ভিত্তিতে একটি সম্ভাব্য উপসর্গের মিল পাওয়া গেছে। নিচে নিরাপদভাবে পরীক্ষা করার কিছু পরামর্শ দেওয়া হলো।",
            "hi": f"{crop_display} से जुड़े आपके सवाल को समझ लिया है। दी गई जानकारी के आधार पर एक संभावित लक्षण मिला है। नीचे सुरक्षित जाँच और अगले कदम दिए गए हैं।",
            "ta": f"{crop_display} பற்றிய உங்கள் கேள்வியைப் புரிந்துகொண்டேன். கொடுக்கப்பட்ட தகவலின் அடிப்படையில் ஒரு சாத்தியமான அறிகுறி காணப்படுகிறது. கீழே பாதுகாப்பான பரிசோதனைகள் மற்றும் அடுத்தடுத்த நடவடிக்கைகள் கொடுக்கப்பட்டுள்ளன.",
            "pa": f"ਮੈਂ {crop_display} ਬਾਰੇ ਤੁਹਾਡਾ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਦਿੱਤੀ ਜਾਣਕਾਰੀ ਦੇ ਆਧਾਰ 'ਤੇ ਇੱਕ ਸੰਭਾਵੀ ਲੱਛਣ ਮਿਲਦਾ ਹੈ। ਹੇਠਾਂ ਸੁਰੱਖਿਅਤ ਜਾਂਚਾਂ ਅਤੇ ਅਗਲੇ ਕਦਮ ਦਿੱਤੇ ਗਏ ਹਨ।",
            "te": f"{crop_display} గురించి మీ ప్రశ్నను అర్థం చేసుకున్నాను. ఇచ్చిన సమాచారంలో ఒక సంభావ్య లక్షణం కనిపిస్తోంది. కింద సురక్షితమైన తనిఖీలు మరియు తదుపరి చర్యలు ఇవ్వబడ్డాయి.",
        }[language]
    else:
        answer = {
            "en": "I understand your agricultural question. Please provide the crop type and growth stage for more specific guidance.",
            "bn": "আপনার কৃষি প্রশ্নটি বুঝেছি। আরও নির্দিষ্ট পরামর্শের জন্য ফসলের ধরন ও বর্তমান বৃদ্ধির পর্যায় জানান।",
            "hi": "मैंने आपका कृषि सवाल समझ लिया है। अधिक विशिष्ट सलाह के लिए फसल का प्रकार और वर्तमान वृद्धि अवस्था बताएं।",
            "ta": "உங்கள் விவசாயக் கேள்வியைப் புரிந்துகொண்டேன். மேலும் குறிப்பிட்ட ஆலோசனைக்கு பயிர் வகை மற்றும் தற்போதைய வளர்ச்சி நிலையை தெரிவிக்கவும்.",
            "pa": "ਮੈਂ ਤੁਹਾਡਾ ਖੇਤੀਬਾੜੀ ਸਵਾਲ ਸਮਝ ਲਿਆ ਹੈ। ਹੋਰ ਖਾਸ ਸਲਾਹ ਲਈ ਫਸਲ ਦੀ ਕਿਸਮ ਅਤੇ ਮੌਜੂਦਾ ਵਿਕਾਸ ਅਵਸਥਾ ਦੱਸੋ।",
            "te": "మీ వ్యవసాయ ప్రశ్నను అర్థం చేసుకున్నాను. మరింత నిర్దిష్టమైన సలహా కోసం పంట రకం మరియు ప్రస్తుత పెరుగుదల దశను ఇవ్వండి.",
        }[language]

    return {
        "answer": answer,
        "language": language,
        "confidence": "low",
        "safety": safety,
        "observations": observations,
        "recommendations": recommendations,
        "uncertainties": uncertainties,
        "source_references": [],
        "location": location_value,
        "crop_category": crop_category,
        "crop_name": crop_name,
    }
