"""Rule-based farmer intelligence features that build on OpenKrishi weather and crop context."""

from __future__ import annotations

from typing import Any


SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}

CROP_GUIDANCE = {
    "rice": {
        "stages": ["seedling", "transplanting", "tillering", "panicle initiation", "flowering", "grain filling", "harvest"],
        "water": "Keep soil moisture appropriate to the crop stage and avoid prolonged unnecessary standing water.",
        "fertilizer": "Use a soil-test or locally recommended nutrient plan; split nitrogen applications according to local agronomy guidance rather than applying a large dose at once.",
        "pests": ["leaf folder", "stem borer", "brown planthopper"],
    },
    "peanut": {
        "stages": ["germination", "vegetative", "flowering", "peg formation", "pod development", "maturity"],
        "water": "Maintain adequate root-zone moisture, especially around flowering and pod development, while avoiding waterlogging.",
        "fertilizer": "Use soil-test-based nutrient management and local recommendations; avoid blanket fertilizer doses.",
        "pests": ["leaf miner", "aphid", "thrips"],
    },
    "vegetables": {
        "stages": ["nursery", "transplanting", "vegetative", "flowering", "fruiting", "harvest"],
        "water": "Prefer consistent root-zone moisture and good drainage; adjust irrigation after rainfall and during hot or windy periods.",
        "fertilizer": "Use crop-specific and soil-test-based recommendations; avoid applying concentrated fertilizer to stressed plants without checking the cause.",
        "pests": ["aphid", "whitefly", "thrips", "fruit borer"],
    },
    "flowers": {
        "stages": ["nursery", "vegetative", "bud formation", "flowering", "harvest"],
        "water": "Keep moisture steady with good drainage and avoid prolonged leaf wetness when disease risk is high.",
        "fertilizer": "Follow crop- and soil-specific recommendations and avoid excessive nitrogen during flowering.",
        "pests": ["aphid", "thrips", "mites", "bud borer"],
    },
}

LOCALIZED = {
    "en": {"today": "Today's farm plan", "check": "Check the crop and soil before changing irrigation or fertilizer.", "rain": "Rain is expected; review irrigation before watering.", "heat": "Hot or dry conditions may increase crop water demand.", "wind": "Wind is elevated; check plant stress and irrigation losses.", "monitor": "Monitor affected plants and compare them with healthy plants."},
    "bn": {"today": "আজকের কৃষি পরিকল্পনা", "check": "সেচ বা সার পরিবর্তনের আগে ফসল ও মাটির অবস্থা পরীক্ষা করুন।", "rain": "বৃষ্টির সম্ভাবনা আছে; সেচ দেওয়ার আগে পরিকল্পনা পর্যালোচনা করুন।", "heat": "গরম বা শুষ্ক আবহাওয়ায় ফসলের পানির চাহিদা বাড়তে পারে।", "wind": "বাতাস বেশি; গাছের চাপ ও সেচের পানির ক্ষতি পরীক্ষা করুন।", "monitor": "আক্রান্ত গাছ পর্যবেক্ষণ করুন এবং সুস্থ গাছের সঙ্গে তুলনা করুন।"},
    "hi": {"today": "आज की खेती की योजना", "check": "सिंचाई या उर्वरक बदलने से पहले फसल और मिट्टी की स्थिति जांचें।", "rain": "बारिश की संभावना है; सिंचाई से पहले योजना की समीक्षा करें।", "heat": "गर्म या शुष्क मौसम में फसल की पानी की जरूरत बढ़ सकती है।", "wind": "हवा तेज है; पौधों के तनाव और सिंचाई में पानी की हानि देखें।", "monitor": "प्रभावित पौधों की निगरानी करें और स्वस्थ पौधों से तुलना करें।"},
    "ta": {"today": "இன்றைய பண்ணைத் திட்டம்", "check": "நீர்ப்பாசனம் அல்லது உரத்தை மாற்றுவதற்கு முன் பயிர் மற்றும் மண் நிலையைச் சரிபார்க்கவும்.", "rain": "மழை வாய்ப்பு உள்ளது; நீர்ப்பாசனத்திற்கு முன் திட்டத்தை மறுபரிசீலனை செய்யவும்.", "heat": "வெப்பமான அல்லது வறண்ட நிலையில் பயிரின் நீர் தேவை அதிகரிக்கலாம்.", "wind": "காற்று அதிகமாக உள்ளது; பயிர் அழுத்தம் மற்றும் நீர்ப்பாசன இழப்பை கவனிக்கவும்.", "monitor": "பாதிக்கப்பட்ட செடிகளை கண்காணித்து ஆரோக்கியமான செடிகளுடன் ஒப்பிடவும்."},
    "pa": {"today": "ਅੱਜ ਦੀ ਖੇਤੀ ਯੋਜਨਾ", "check": "ਸਿੰਚਾਈ ਜਾਂ ਖਾਦ ਬਦਲਣ ਤੋਂ ਪਹਿਲਾਂ ਫਸਲ ਅਤੇ ਮਿੱਟੀ ਦੀ ਹਾਲਤ ਜਾਂਚੋ।", "rain": "ਮੀਂਹ ਦੀ ਸੰਭਾਵਨਾ ਹੈ; ਸਿੰਚਾਈ ਤੋਂ ਪਹਿਲਾਂ ਯੋਜਨਾ ਦੀ ਸਮੀਖਿਆ ਕਰੋ।", "heat": "ਗਰਮ ਜਾਂ ਸੁੱਕੇ ਮੌਸਮ ਵਿੱਚ ਫਸਲ ਦੀ ਪਾਣੀ ਦੀ ਲੋੜ ਵੱਧ ਸਕਦੀ ਹੈ।", "wind": "ਹਵਾ ਤੇਜ਼ ਹੈ; ਪੌਦਿਆਂ ਦੇ ਤਣਾਅ ਅਤੇ ਸਿੰਚਾਈ ਦੇ ਪਾਣੀ ਦੇ ਨੁਕਸਾਨ ਨੂੰ ਦੇਖੋ।", "monitor": "ਪ੍ਰਭਾਵਿਤ ਪੌਦਿਆਂ ਦੀ ਨਿਗਰਾਨੀ ਕਰੋ ਅਤੇ ਸਿਹਤਮੰਦ ਪੌਦਿਆਂ ਨਾਲ ਤੁਲਨਾ ਕਰੋ।"},
    "te": {"today": "ఈరోజు వ్యవసాయ ప్రణాళిక", "check": "నీటి పారుదల లేదా ఎరువును మార్చే ముందు పంట మరియు నేల పరిస్థితిని తనిఖీ చేయండి.", "rain": "వర్షం వచ్చే అవకాశం ఉంది; నీరు పెట్టే ముందు ప్రణాళికను సమీక్షించండి.", "heat": "వేడి లేదా పొడి వాతావరణంలో పంటకు నీటి అవసరం పెరగవచ్చు.", "wind": "గాలి ఎక్కువగా ఉంది; మొక్కల ఒత్తిడి మరియు నీటి నష్టాన్ని గమనించండి.", "monitor": "బాధిత మొక్కలను గమనించి ఆరోగ్యకరమైన మొక్కలతో పోల్చండి."},
}

def _lang(language: str) -> dict[str, str]:
    return LOCALIZED.get(language, LOCALIZED["en"])

def _validate(crop_category: str) -> None:
    if crop_category not in SUPPORTED_CROPS:
        raise ValueError("Unsupported crop category.")

def build_irrigation(crop_category: str, language: str, temperature_c: float | None = None, rain_probability: float | None = None, et0: float | None = None) -> dict[str, Any]:
    _validate(crop_category)
    x = _lang(language)
    advice = [CROP_GUIDANCE[crop_category]["water"], x["check"]]
    if rain_probability is not None and rain_probability >= 60:
        advice.insert(0, x["rain"])
        action = "delay_and_recheck"
    elif temperature_c is not None and temperature_c >= 34:
        advice.insert(0, x["heat"])
        action = "check_moisture_more_often"
    else:
        action = "check_root_zone"
    return {"crop_category": crop_category, "action": action, "advice": advice, "et0_fao": et0, "safety": "Do not use weather alone to decide irrigation; check soil moisture and crop stage."}

def build_fertilizer(crop_category: str, growth_stage: str | None, language: str) -> dict[str, Any]:
    _validate(crop_category)
    stage = (growth_stage or "").strip()
    advice = [CROP_GUIDANCE[crop_category]["fertilizer"], _lang(language)["check"]]
    if stage:
        advice.insert(0, f"Growth stage: {stage}. Use stage-specific local agronomy guidance.")
    return {"crop_category": crop_category, "growth_stage": stage or None, "advice": advice, "safety": "No pesticide rate or blanket fertilizer dose is provided. Confirm recommendations with local agronomy guidance or a soil test."}

def build_pest_alerts(crop_category: str, language: str, rain_probability: float | None = None, humidity: float | None = None) -> dict[str, Any]:
    _validate(crop_category)
    alerts = []
    if humidity is not None and humidity >= 80:
        alerts.append(_lang(language)["monitor"] + " High humidity can increase the risk of some fungal and insect problems.")
    if rain_probability is not None and rain_probability >= 60:
        alerts.append(_lang(language)["rain"])
    if not alerts:
        alerts.append(_lang(language)["monitor"])
    return {"crop_category": crop_category, "watch_list": CROP_GUIDANCE[crop_category]["pests"], "alerts": alerts, "safety": "These are monitoring prompts, not confirmed pest or disease diagnoses."}

def build_crop_recommendation(soil_type: str | None, water_availability: str | None, season: str | None, language: str) -> dict[str, Any]:
    water = (water_availability or "").lower()
    season_text = (season or "").lower()
    notes = []
    if water in {"low", "limited"}:
        candidates = ["peanut", "vegetables"]
        notes.append("Prioritize crops that can be managed with available water and confirm local suitability.")
    elif "rain" in season_text or "monsoon" in season_text:
        candidates = ["rice", "vegetables"]
        notes.append("Rainfall pattern and drainage should be checked before selecting a crop.")
    else:
        candidates = ["rice", "peanut", "vegetables", "flowers"]
        notes.append("Confirm soil, water, market access and local seasonal recommendations before planting.")
    if soil_type:
        notes.append(f"Soil type provided: {soil_type}. A soil test is recommended before final selection.")
    return {"recommended_categories": candidates, "notes": notes, "language": language, "safety": "Crop selection is a planning aid, not a guaranteed yield or income prediction."}

def build_crop_knowledge(crop_category: str, language: str) -> dict[str, Any]:
    _validate(crop_category)
    profile = CROP_GUIDANCE[crop_category]
    return {"crop_category": crop_category, "stages": profile["stages"], "water": profile["water"], "fertilizer": profile["fertilizer"], "common_watch_list": profile["pests"], "language": language}

def build_farm_plan(crop_category: str, growth_stage: str | None, language: str, weather: dict[str, Any] | None = None) -> dict[str, Any]:
    _validate(crop_category)
    x = _lang(language)
    current = (weather or {}).get("current", {})
    rain = current.get("precipitation")
    humidity = current.get("relative_humidity_2m")
    temp = current.get("temperature_2m")
    tasks = [x["check"], CROP_GUIDANCE[crop_category]["water"], CROP_GUIDANCE[crop_category]["fertilizer"], x["monitor"]]
    if isinstance(rain, (int, float)) and rain > 0:
        tasks.insert(0, x["rain"])
    elif isinstance(temp, (int, float)) and temp >= 34:
        tasks.insert(0, x["heat"])
    return {"title": x["today"], "crop_category": crop_category, "growth_stage": growth_stage, "tasks": tasks[:6], "weather_snapshot": {"temperature_c": temp, "humidity_pct": humidity, "rain_mm": rain}, "safety": "Use this as a daily checklist; verify field conditions before taking treatment actions."}
