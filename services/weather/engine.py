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

WEATHER_DESCRIPTION = {
    "en": {0:"Clear sky",1:"Mainly clear",2:"Partly cloudy",3:"Overcast",45:"Fog",48:"Depositing rime fog",51:"Light drizzle",53:"Moderate drizzle",55:"Dense drizzle",61:"Slight rain",63:"Moderate rain",65:"Heavy rain",71:"Slight snow",73:"Moderate snow",75:"Heavy snow",80:"Slight rain showers",81:"Moderate rain showers",82:"Violent rain showers",95:"Thunderstorm",96:"Thunderstorm with hail",99:"Thunderstorm with heavy hail"},
    "bn": {0:"আকাশ পরিষ্কার",1:"মূলত পরিষ্কার",2:"আংশিক মেঘলা",3:"মেঘাচ্ছন্ন",45:"কুয়াশা",48:"হিমকুয়াশা",51:"হালকা গুঁড়ি বৃষ্টি",53:"মাঝারি গুঁড়ি বৃষ্টি",55:"ঘন গুঁড়ি বৃষ্টি",61:"হালকা বৃষ্টি",63:"মাঝারি বৃষ্টি",65:"ভারী বৃষ্টি",71:"হালকা তুষারপাত",73:"মাঝারি তুষারপাত",75:"ভারী তুষারপাত",80:"হালকা বৃষ্টির ঝাপটা",81:"মাঝারি বৃষ্টির ঝাপটা",82:"প্রবল বৃষ্টির ঝাপটা",95:"বজ্রঝড়",96:"শিলাসহ বজ্রঝড়",99:"প্রবল শিলাসহ বজ্রঝড়"},
    "hi": {0:"आसमान साफ",1:"मुख्यतः साफ",2:"आंशिक बादल",3:"बादल छाए हुए",45:"कोहरा",48:"पाला वाला कोहरा",51:"हल्की बूंदाबांदी",53:"मध्यम बूंदाबांदी",55:"घनी बूंदाबांदी",61:"हल्की बारिश",63:"मध्यम बारिश",65:"भारी बारिश",71:"हल्की बर्फबारी",73:"मध्यम बर्फबारी",75:"भारी बर्फबारी",80:"हल्की बारिश की बौछार",81:"मध्यम बारिश की बौछार",82:"तेज बारिश की बौछार",95:"आंधी-तूफान",96:"ओलों के साथ आंधी-तूफान",99:"भारी ओलों के साथ आंधी-तूफान"},
    "ta": {0:"வானம் தெளிவு",1:"பெரும்பாலும் தெளிவு",2:"பகுதியளவு மேகமூட்டம்",3:"முழுவதும் மேகமூட்டம்",45:"மூடுபனி",48:"பனிப்படல மூடுபனி",51:"லேசான தூறல்",53:"மிதமான தூறல்",55:"கனமான தூறல்",61:"லேசான மழை",63:"மிதமான மழை",65:"கனமழை",71:"லேசான பனிப்பொழிவு",73:"மிதமான பனிப்பொழிவு",75:"கனமான பனிப்பொழிவு",80:"லேசான மழைத்தூறல்",81:"மிதமான மழைத்தூறல்",82:"பலத்த மழைத்தூறல்",95:"இடியுடன் கூடிய மழை",96:"ஆலங்கட்டியுடன் இடியுடன் கூடிய மழை",99:"கனமான ஆலங்கட்டியுடன் இடியுடன் கூடிய மழை"},
    "pa": {0:"ਸਾਫ਼ ਆਸਮਾਨ",1:"ਮੁੱਖ ਤੌਰ 'ਤੇ ਸਾਫ਼",2:"ਅੰਸ਼ਿਕ ਬੱਦਲ",3:"ਬੱਦਲ ਛਾਏ ਹੋਏ",45:"ਧੁੰਦ",48:"ਪਾਲੇ ਵਾਲੀ ਧੁੰਦ",51:"ਹਲਕੀ ਫੁਹਾਰ",53:"ਦਰਮਿਆਨੀ ਫੁਹਾਰ",55:"ਘਣੀ ਫੁਹਾਰ",61:"ਹਲਕੀ ਬਾਰਿਸ਼",63:"ਦਰਮਿਆਨੀ ਬਾਰਿਸ਼",65:"ਭਾਰੀ ਬਾਰਿਸ਼",71:"ਹਲਕੀ ਬਰਫ਼ਬਾਰੀ",73:"ਦਰਮਿਆਨੀ ਬਰਫ਼ਬਾਰੀ",75:"ਭਾਰੀ ਬਰਫ਼ਬਾਰੀ",80:"ਹਲਕੀ ਬਾਰਿਸ਼ ਦੀਆਂ ਛਿੱਟਾਂ",81:"ਦਰਮਿਆਨੀ ਬਾਰਿਸ਼ ਦੀਆਂ ਛਿੱਟਾਂ",82:"ਤੇਜ਼ ਬਾਰਿਸ਼ ਦੀਆਂ ਛਿੱਟਾਂ",95:"ਤੂਫ਼ਾਨ",96:"ਗੜਿਆਂ ਨਾਲ ਤੂਫ਼ਾਨ",99:"ਭਾਰੀ ਗੜਿਆਂ ਨਾਲ ਤੂਫ਼ਾਨ"},
    "te": {0:"ఆకాశం నిర్మలంగా ఉంది",1:"ప్రధానంగా నిర్మలంగా ఉంది",2:"పాక్షికంగా మేఘావృతం",3:"మేఘావృతం",45:"పొగమంచు",48:"మంచు పొగమంచు",51:"తేలికపాటి జల్లులు",53:"మోస్తరు జల్లులు",55:"దట్టమైన జల్లులు",61:"తేలికపాటి వర్షం",63:"మోస్తరు వర్షం",65:"భారీ వర్షం",71:"తేలికపాటి మంచు",73:"మోస్తరు మంచు",75:"భారీ మంచు",80:"తేలికపాటి వర్షపు జల్లులు",81:"మోస్తరు వర్షపు జల్లులు",82:"తీవ్ర వర్షపు జల్లులు",95:"ఉరుములతో కూడిన వర్షం",96:"వడగళ్లతో ఉరుములతో కూడిన వర్షం",99:"భారీ వడగళ్లతో ఉరుములతో కూడిన వర్షం"},
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
    current = dict(data.get("current", {}))
    code = current.get("weather_code")
    try:
        code_int = int(code)
    except (TypeError, ValueError):
        code_int = None
    current["weather_description"] = WEATHER_DESCRIPTION.get(language, WEATHER_DESCRIPTION["en"]).get(code_int, "Weather condition unavailable")

    safety_reason = {
        "en": "Weather information supports planning and monitoring. It does not by itself justify pesticide, fertilizer, or other chemical treatment decisions.",
        "bn": "আবহাওয়ার তথ্য পরিকল্পনা ও নজরদারিতে সহায়ক। শুধু আবহাওয়ার তথ্যের ভিত্তিতে কীটনাশক, সার বা অন্য কোনো রাসায়নিক ব্যবহারের সিদ্ধান্ত নেওয়া উচিত নয়।",
        "hi": "मौसम की जानकारी योजना और निगरानी में सहायक है। केवल मौसम की जानकारी के आधार पर कीटनाशक, उर्वरक या अन्य रासायनिक उपचार का निर्णय नहीं लेना चाहिए।",
        "ta": "வானிலைத் தகவல் திட்டமிடல் மற்றும் கண்காணிப்புக்கு உதவுகிறது. வானிலைத் தகவலை மட்டும் அடிப்படையாகக் கொண்டு பூச்சிக்கொல்லி, உரம் அல்லது பிற இரசாயன சிகிச்சையைத் தீர்மானிக்கக் கூடாது.",
        "pa": "ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਯੋਜਨਾ ਅਤੇ ਨਿਗਰਾਨੀ ਲਈ ਸਹਾਇਕ ਹੈ। ਸਿਰਫ਼ ਮੌਸਮ ਦੀ ਜਾਣਕਾਰੀ ਦੇ ਆਧਾਰ 'ਤੇ ਕੀਟਨਾਸ਼ਕ, ਖਾਦ ਜਾਂ ਹੋਰ ਰਸਾਇਣਕ ਇਲਾਜ ਦਾ ਫੈਸਲਾ ਨਹੀਂ ਕਰਨਾ ਚਾਹੀਦਾ।",
        "te": "వాతావరణ సమాచారం ప్రణాళిక మరియు పర్యవేక్షణకు సహాయపడుతుంది. వాతావరణ సమాచారాన్ని మాత్రమే ఆధారంగా చేసుకుని పురుగుమందులు, ఎరువులు లేదా ఇతర రసాయన చికిత్సలపై నిర్ణయం తీసుకోకూడదు.",
    }.get(language, "Weather information supports planning and monitoring.")

    return {
        "location": {
            "latitude": data.get("latitude", latitude),
            "longitude": data.get("longitude", longitude),
            "timezone": data.get("timezone"),
            "elevation_m": data.get("elevation"),
        },
        "current": current,
        "forecast": _daily_summary(data),
        "farm_alert": alert,
        "farm_alerts": alerts,
        "language": language,
        "source": "Open-Meteo",
        "safety": {
            "status": "caution",
            "reason": safety_reason,
        },
    }
