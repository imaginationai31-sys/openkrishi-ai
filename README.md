# OpenKrishi AI

**Vernacular AI Agronomy & Voice Advisory for Indian Farmers**

OpenKrishi AI is an open-source, multilingual agricultural assistance platform designed to help Indian farmers understand crop problems, receive practical agronomy guidance, check weather conditions, and interact with AI using regional Indian languages.

The project is being developed as a **PWA-first application**, so farmers can use OpenKrishi AI directly from a mobile browser and install it on supported Android devices without requiring Android Studio.

## 🌾 Core Features

- **Multilingual agricultural advisory** for Indian farmers
- **Crop problem diagnosis** from farmer-described symptoms
- **Crop image analysis** using camera or gallery photos
- **Possible-cause analysis** with cautious, non-definitive recommendations
- **Weather information and agricultural weather guidance**
- **Voice advisory** and voice-oriented interaction
- **Regional-language responses** without unnecessary mixed-language output
- **Crop categories and crop selection**
- Mobile-first, installable **Progressive Web App (PWA)**
- Backend API architecture that can be used by future mobile clients

## 🌱 Crop Categories

OpenKrishi AI is organized around four main agricultural categories:

1. **Cereal**
   - Rice
   - Wheat
   - Maize
   - Other supported cereal crops

2. **Pulse & Oilseed**
   - Peanut
   - Other supported pulse and oilseed crops

3. **Vegetable**
   - Tomato
   - Chilli
   - Other supported vegetable crops

4. **Flower**
   - Common flower crops
   - Other supported flower crops

The crop catalog will continue to expand as the advisory engine and regional agriculture coverage improve.

## 🗣️ Language Support

The application is designed for multilingual use, with support being developed for:

- English
- Bengali
- Hindi
- Tamil
- Punjabi
- Telugu

The goal is to return the user-facing advisory in the selected/requested language rather than mixing languages unnecessarily.

## 🏗️ Current Architecture

OpenKrishi AI currently follows a PWA + API architecture:

```text
Farmer
  │
  ▼
OpenKrishi AI PWA
  │
  ├── Camera / Gallery
  ├── Crop & Language Selection
  ├── Advisory
  ├── Vision
  ├── Voice
  └── Weather
  │
  ▼
OpenKrishi AI API
  │
  ├── Advisory
  ├── Vision Assessment
  ├── Voice Transcription
  ├── Voice Advisory
  └── Weather
```

### Frontend

The current primary client is a mobile-first PWA hosted through Hatchable.

### Backend

The existing production API is deployed on Render:

- API: `https://openkrishi-ai-api.onrender.com/`
- Interactive API documentation: `https://openkrishi-ai-api.onrender.com/docs`
- OpenAPI specification: `https://openkrishi-ai-api.onrender.com/openapi.json`

### API Endpoints

The backend currently exposes:

- `GET /api/v1/health`
- `POST /api/v1/advisory`
- `POST /api/v1/voice/transcribe`
- `POST /api/v1/voice/advisory`
- `POST /api/v1/voice/vision-advisory`
- `POST /api/v1/vision/assess`
- `GET /api/v1/weather`

## 📱 PWA-First Development

Android Studio is **not required for the current OpenKrishi AI development path**.

The PWA approach allows the project to be developed and tested through:

- Desktop browsers
- Android mobile browsers
- Camera and gallery access
- Installable PWA experience
- Existing Render API infrastructure

A native Android client may be developed later when the web/PWA product and API are stable.

## 🔐 Security

- [Threat Model](docs/THREAT_MODEL.md)
- [Security Policy](SECURITY.md)
- Secrets are loaded server-side from environment configuration; production Render secrets use `sync: false`.

## 🔐 Safety Principles

OpenKrishi AI is intended to provide **informational agricultural guidance**, not replace qualified agricultural experts or local agricultural authorities.

The advisory system should:

- Avoid claiming a disease is confirmed from symptoms alone.
- Clearly distinguish observations from possible causes.
- Request a clearer image when image evidence is insufficient.
- Avoid unsafe or unsupported pesticide/fertilizer dosage instructions.
- Encourage local expert/agriculture-department confirmation for serious or uncertain cases.
- Protect farmer data and avoid unnecessary collection of personal information.

## 💰 Access & Product Direction

The product direction is to keep the core OpenKrishi AI service **free for farmers**.

Future sustainability options may include non-intrusive advertising and B2B/enterprise partnerships, while keeping essential farmer advisory functionality accessible.

## 🚀 Development Status

OpenKrishi AI is under active development.

### Current priority

1. Stabilize the PWA frontend.
2. Connect all PWA features to the existing Render API.
3. Fix and test the advisory API.
4. Validate crop vision.
5. Validate voice input/output.
6. Validate weather integration.
7. Complete multilingual consistency.
8. Improve mobile UX and PWA installation.
9. Prepare the public release.

## 🧪 Testing

The API can be tested through the interactive FastAPI documentation:

`https://openkrishi-ai-api.onrender.com/docs`

The PWA should be tested on both desktop and Android mobile browsers, especially for:

- Camera permissions
- Gallery image selection
- Microphone permissions
- Language selection
- Slow/mobile network conditions
- Advisory response handling
- Image-analysis failures
- Voice failures
- Weather/location permissions

## 🤝 Open Source

OpenKrishi AI is intended to remain an open-source project focused on practical, multilingual AI assistance for agriculture.

Contributions, testing feedback, agricultural knowledge, language improvements, and technical improvements are welcome.

## 📄 License

See the repository license file for the current licensing terms.

---

**OpenKrishi AI**  
*Vernacular AI Agronomy & Voice Advisory for Indian Farmers*