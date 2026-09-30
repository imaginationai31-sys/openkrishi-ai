# OpenKrishi AI Roadmap

## Near-term quality and release work

- Complete the endpoint test matrix and reach 80%+ backend coverage.
- Add Firebase Emulator tests for Firestore and Storage rules.
- Finish frontend modularization and remove remaining large inline scripts.
- Add dependency and license reporting.
- Complete Render and PWA production smoke tests.
- Publish the v0.1.0 release.

## Product roadmap

### Phase 1 — Foundation
- API foundation
- Crop schemas
- Agricultural knowledge base
- Safety framework
- Documentation and tests

### Phase 2 — Voice
- Saaras speech-to-text
- Bulbul text-to-speech
- Bengali, Hindi, Tamil, Punjabi, Telugu and English support
- Agricultural vocabulary handling

### Phase 3 — Crop intelligence
- Rice intelligence
- Peanut intelligence
- Expandable vegetable registry
- Expandable flower registry

### Phase 4 — Vision
- Crop image upload
- Image preprocessing
- Model inference
- Confidence scoring
- Disease and stress assessment

### Phase 5 — Weather intelligence
- Weather provider integration
- Location-aware conditions
- Weather risk engine
- Crop-specific alerts

### Phase 6 — Unified advisory
- Combine crop, stage, image, weather, farmer question, agricultural knowledge, confidence and safety controls.

### Phase 7 — Field validation
- Evaluate accuracy, usability, language quality, safety, trust, and low-connectivity performance.

## Good first issues

- Add a regression test for every remaining farm route.
- Add Firebase Emulator tests for security rules.
- Add a frontend test for camera permission denial.
- Add a frontend test for network timeout handling.
- Split the largest backend router into focused service modules.
- Add provider adapter contract tests.
- Add an accessibility smoke test for keyboard navigation.
- Add a privacy and retention review for provider data handling.
