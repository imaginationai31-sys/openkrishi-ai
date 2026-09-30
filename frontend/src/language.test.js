import { describe, expect, it } from "vitest";

describe("language selection", () => {
  it("keeps the six supported farmer languages explicit", async () => {
    await import("./language.js");
    expect(window.OpenKrishiLanguage.SUPPORTED_LANGUAGES).toEqual([
      "en",
      "bn",
      "hi",
      "ta",
      "pa",
      "te",
    ]);
  });

  it("accepts supported languages", async () => {
    await import("./language.js");
    for (const language of window.OpenKrishiLanguage.SUPPORTED_LANGUAGES) {
      expect(window.OpenKrishiLanguage.isSupportedLanguage(language)).toBe(true);
    }
  });

  it("falls back to English for unsupported values", async () => {
    await import("./language.js");
    expect(window.OpenKrishiLanguage.isSupportedLanguage("fr")).toBe(false);
    expect(window.OpenKrishiLanguage.normalizeLanguage("fr")).toBe("en");
  });
});
