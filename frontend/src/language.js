const SUPPORTED_LANGUAGES = ["en", "bn", "hi", "ta", "pa", "te"];

function isSupportedLanguage(language) {
  return SUPPORTED_LANGUAGES.includes(language);
}

function normalizeLanguage(language) {
  return isSupportedLanguage(language) ? language : "en";
}

window.OpenKrishiLanguage = { SUPPORTED_LANGUAGES, isSupportedLanguage, normalizeLanguage };
