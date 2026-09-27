# OpenKrishi AI — Firebase setup

## Firebase services
Authentication, Cloud Firestore, Cloud Storage, Cloud Messaging, App Check, Analytics, Crashlytics and Remote Config.

## Initial setup
1. Create a Firebase project named OpenKrishi AI.
2. Register the PWA as a Web app.
3. Copy the web configuration into apps/pwa/.env using .env.example.
4. Enable Anonymous Authentication for first-run access.
5. Deploy Firestore rules/indexes and Storage rules.
6. Later enable Google/Phone sign-in and Play Integrity App Check for Android.

Never put Gemini or other server credentials in Vite environment variables. The browser configuration is not a substitute for Firebase Security Rules.

## Data model
- users/{uid}
- users/{uid}/diagnoses/{diagnosisId}
- users/{uid}/preferences/{document}

## Media
Crop images and voice recordings are scoped to the authenticated UID and limited to 12 MB.

## Production
Use separate Firebase projects for development, staging and production. Do not commit google-services.json or service-account credentials.

The proposed Android application ID is ai.openkrishi.app. Confirm this before registering the Android app because Firebase treats the registered package name as permanent for that Firebase Android app.
