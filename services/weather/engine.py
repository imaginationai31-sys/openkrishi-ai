from __future__ import annotations

from typing import Any

import httpx

OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
SUPPORTED_LANGUAGES = {"en", "bn", "hi", "ta", "pa", "te"}

ALERT_TEXT = {
    "en": {
        "rain": "Rain is likely. Check field drainage and avoid unnecessary field operations during heavy rain.",
        "humidity": "High humidity with wet conditions can increase disease risk. Monitor crops closely for new symptoms.",
        "heat": "High temperature can increase crop water stress. Monitor soil moisture and crop condition.",
        "wind": "Strong winds are expected. Inspect vulnerable plants and provide physical support where appropriate.",
        "normal": "No major weather-related farm alert was detected from the available forecast.",
    },
    "bn": {
        "rain": "বৃষ্টির সম্ভাবনা আছে। জমির নিকাশি ব্যবস্থা পরীক্ষা করুন এবং ভারী বৃষ্টির সময় অপ্রয়োজনীয় মাঠের কাজ এড়িয়ে চলুন।",
        "humidity": "উচ্চ আর্দ্রতা ও ভেজা পরিবেশে রোগের ঝুঁকি বাড়তে পারে। নতুন উপসর্গের জন্য ফসল নজরে রাখুন।",
        "heat": "উচ্চ তাপমাত্রায় ফসলের জল-চাপ বাড়তে পারে। মাটির আর্দ্রতা ও ফসলের অবস্থা নজরে রাখুন।",
        "wind": "জোরালো বাতাসের সম্ভাবনা আছে। দুর্বল গাছ পরীক্ষা করুন এবং প্রয়োজনে শারীরিক সহায়তা দিন।",
        "normal": "উপলব্ধ পূর্বাভাস অনুযায়ী বড় কোনো আবহাওয়া-সম্পর্কিত কৃষি সতর্কতা পাওয়া যায়নি।",
    },
    "hi": {
        "rain": "बारिश की संभावना है। खेत की जल निकासी जांचें और तेज बारिश के दौरान अनावश्यक खेत कार्य से बचें।",
        "humidity": "अधिक नमी और गीली परिस्थितियों में रोग का जोखिम बढ़ सकता है। नए लक्षणों पर फसल की निगरानी करें।",
        "heat": "अधिक तापमान से फसल में पानी का तनाव बढ़ सकता है। मिट्टी की नमी और फसल की स्थिति देखें।",
        "wind": "तेज हवा की संभावना है। कमजोर पौधों की जांच करें और जरूरत होने पर भौतिक सहारा दें।",
        "normal": "उपलब्ध पूर्वानुमान के अनुसार कोई प्रमुख मौसम-आधारित कृषि चेतावनी नहीं मिली।",
    },
    "ta": {
        "rain": "மழை பெய்ய வாய்ப்பு உள்ளது. வயலின் வடிகால் வசதியைச் சரிபார்த்து, கனமழையின் போது தேவையற்ற வயல் பணிகளைத் தவிர்க்கவும்.",
        "humidity": "அதிக ஈரப்பதமும் ஈரமான சூழலும் நோய் அபாயத்தை அதிகரிக்கலாம். புதிய அறிகுறிகளை கவனமாக கண்காணிக்கவும்.",
        "heat": "அதிக வெப்பநிலை பயிரில் நீர் அழுத்தத்தை அதிகரிக்கலாம். மண் ஈரப்பதத்தையும் பயிரின் நிலையையும் கண்காணிக்கவும்.",
        "wind": "பலத்த காற்று எதிர்பார்க்கப்படுகிறது. பாதிக்கக்கூடிய செடிகளைச் சரிபார்த்து, தேவையான இடங்களில் உடல் ஆதரவு வழங்கவும்.",
        "normal": "கிடைக்கும் வானிலை முன்னறிவிப்பின் அடிப்படையில் முக்கியமான வேளாண் எச்சரிக்கை எதுவும் இல்லை.",
    },
    "pa": {
        "rain": "ਮੀਂਹ ਪੈਣ ਦੀ ਸੰਭਾਵਨਾ ਹੈ। ਖੇਤ ਦੀ ਨਿਕਾਸੀ ਜਾਂਚੋ ਅਤੇ ਤੇਜ਼ ਮੀਂਹ ਦੌਰਾਨ ਬੇਲੋੜੇ ਖੇਤੀ ਕੰਮ ਤੋਂ ਬਚੋ।",
        "humidity": "ਜ਼ਿਆਦਾ ਨਮੀ ਅਤੇ ਗਿੱਲੀਆਂ ਹਾਲਤਾਂ ਵਿੱਚ ਬਿਮਾਰੀ ਦਾ ਖਤਰਾ ਵੱਧ ਸਕਦਾ ਹੈ। ਨਵੇਂ ਲੱਛਣਾਂ ਲਈ ਫਸਲ ਦੀ ਨਿਗਰਾਨੀ ਕਰੋ।",
        "heat": "ਜ਼ਿਆਦਾ ਤਾਪਮਾਨ ਫਸਲ ਵਿੱਚ ਪਾਣੀ ਦਾ ਤਣਾਅ ਵਧਾ ਸਕਦਾ ਹੈ। ਮਿੱਟੀ ਦੀ ਨਮੀ ਅਤੇ ਫਸਲ ਦੀ ਹਾਲਤ ਵੇਖੋ।",
        "wind": "ਤੇਜ਼ ਹਵਾਵਾਂ ਦੀ ਸੰਭਾਵਨਾ ਹੈ। ਕਮਜ਼ੋਰ ਪੌਦਿਆਂ ਦੀ ਜਾਂਚ ਕਰੋ ਅਤੇ ਲੋੜ ਪੈਣ 'ਤੇ ਭੌਤਿਕ ਸਹਾਰਾ ਦਿਓ।",
        "normal": "ਉਪਲਬਧ ਮੌਸਮੀ ਪੂਰਵ ਅਨੁਮਾਨ ਅਨੁਸਾਰ ਕੋਈ ਵੱਡੀ ਖੇਤੀਬਾੜੀ ਮੌਸਮੀ ਚੇਤਾਵਨੀ ਨਹੀਂ ਮਿਲੀ।",
    },
    "te": {
        "rain": "వర్షం పడే అవకాశం ఉంది. పొలం నీటి పారుదల పరిస్థితిని పరిశీలించి, భారీ వర్ష సమయంలో అవసరం లేని పనులను నివారించండి.",
        "humidity": "అధిక తేమ మరియు తడి పరిస్థితుల్లో వ్యాధి ప్రమాదం పెరగవచ్చు. కొత్త లక్షణాల కోసం పంటను గమనించండి.",
        "heat": "అధిక ఉష్ణోగ్రత పంటలో నీటి ఒత్తిడిని పెంచవచ్చు. నేల తేమ మరియు పంట పరిస్థితిని గమనించండి.",
        "wind": "బలమైన గాలులు వచ్చే అవకాశం ఉంది. బలహీనమైన మొక్కలను పరిశీలించి, అవసరమైతే భౌతిక మద్దతు ఇవ్వండి.",
        "normal": "అందుబాటులో ఉన్న వాతావరణ అంచనా ప్రకారం ప్రధానమైన వ్యవసాయ వాతావరణ హెచ్చరిక ఏదీ కనిపించలేదు.",
    },
}


def _as_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _max(values: list[Any]) -> float:
    return max((_as_float(v) for v in values), default=0.0)


def _sum(values: list[Any]) -> float:
    return sum(_as_float(v) for v in values)


def _build_alert(hourly: dict[str, Any], language: str) -> tuple[str, list[str]]:
    text = ALERT_TEXT.get(language, ALERT_TEXT["en"])
    rain_probability = _max(hourly.get("precipitation_probability", []))
    rain = _sum(hourly.get("rain", [])) + _sum(hourly.get("showers", []))
    humidity = _max(hourly.get("relative_humidity_2m", []))
    temperature = _max(hourly.get("temperature_2m", []))
    wind = _max(hourly.get("wind_speed_10m", []))

    alerts: list[str] = []
    if rain_probability >= 60 or rain >= 10:
        alerts.append(text["rain"])
    if humidity >= 85 and rain_probability >= 50:
        alerts.append(text["humidity"])
    if temperature >= 36:
        alerts.append(text["heat"])
    if wind >= 35:
        alerts.append(text["wind"])
    if not alerts:
        alerts.append(text["normal"])

    return alerts[0], alerts


def _daily_summary(data: dict[str, Any]) -> list[dict[str, Any]]:
    daily = data.get("daily", {})
    times = daily.get("time", [])
    rows: list[dict[str, Any]] = []
    for i, day in enumerate(times):
        rows.append(
            {
                "date": day,
                "temperature_max_c": daily.get("temperature_2m_max", [None] * len(times))[i],
                "temperature_min_c": daily.get("temperature_2m_min", [None] * len(times))[i],
                "precipitation_probability_max_pct": daily.get("precipitation_probability_max", [None] * len(times))[i],
                "precipitation_sum_mm": daily.get("precipitation_sum", [None] * len(times))[i],
                "wind_speed_max_kmh": daily.get("wind_speed_10m_max", [None] * len(times))[i],
                "et0_mm": daily.get("et0_fao_evapotranspiration", [None] * len(times))[i],
            }
        )
    return rows


async def get_weather(
    latitude: float,
    longitude: float,
    language: str = "en",
    forecast_days: int = 7,
) -> dict[str, Any]:
    if language not in SUPPORTED_LANGUAGES:
        raise ValueError(f"Unsupported language: {language}")
    if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
        raise ValueError("Invalid latitude or longitude")
    if not (1 <= forecast_days <= 7):
        raise ValueError("forecast_days must be between 1 and 7")

    params = {
        "latitude": latitude,
        "longitude": longitude,
        "timezone": "auto",
        "forecast_days": forecast_days,
        "current": "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code",
        "hourly": "temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,rain,showers,wind_speed_10m,et0_fao_evapotranspiration",
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,et0_fao_evapotranspiration",
    }

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(OPEN_METEO_URL, params=params)
            response.raise_for_status()
            data = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise RuntimeError("Weather provider is temporarily unavailable.") from exc

    hourly = data.get("hourly", {})
    alert, alerts = _build_alert(hourly, language)
    return {
        "location": {
            "latitude": data.get("latitude", latitude),
            "longitude": data.get("longitude", longitude),
            "timezone": data.get("timezone"),
            "elevation_m": data.get("elevation"),
        },
        "current": data.get("current", {}),
        "forecast": _daily_summary(data),
        "farm_alert": alert,
        "farm_alerts": alerts,
        "language": language,
        "source": "Open-Meteo",
        "safety": {
            "status": "caution",
            "reason": "Weather information supports planning and monitoring. It does not by itself justify pesticide, fertilizer, or other chemical treatment decisions.",
        },
    }
