# OpenKrishi AI — PWA Architecture

## Objective
Make the PWA the primary user client while keeping the backend client-agnostic so Android can be added later without a second backend.

## Stack Direction
Use a modern TypeScript web application with:
- responsive mobile-first UI
- installable PWA
- service worker
- camera and gallery access
- microphone access where supported
- accessible controls
- API client layer
- local state/cache layer

## Application Areas
1. Home/dashboard
2. Crop selection
3. Camera/gallery diagnosis
4. Advisory result
5. Voice advisory
6. Weather
7. History
8. Profile/settings
9. Feedback

## Backend Integration
The PWA communicates only with the public API. Provider keys, database credentials, Firebase server credentials, and privileged storage credentials never ship to the browser.

## Media
For crop photos, use camera/gallery input and upload securely. The normal diagnosis flow does not need a generic file picker.

## Offline/Weak Network
Cache app shell and safe static resources. Queue only operations that are safe to retry. Never pretend an offline AI request succeeded.

## Language
The selected language is persisted locally and sent explicitly with API requests. User-facing AI output must remain in the requested language.

## Security
Use HTTPS, strict origin configuration, secure token handling, CSP, safe DOM rendering, dependency controls, and no secrets in frontend source.

## Performance
Optimize for low-bandwidth mobile networks using compressed images, lazy loading, code splitting, bounded uploads, and clear progress states.

## Accessibility
Support readable typography, keyboard navigation, semantic controls, sufficient contrast, screen-reader labels, and voice-friendly interactions.
