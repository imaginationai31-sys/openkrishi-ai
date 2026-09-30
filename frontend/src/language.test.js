import { describe, expect, it } from "vitest";
import { isSupportedLanguage, normalizeLanguage, SUPPORTED_LANGUAGES } from "./language.js";

describe("language selection", () => {
  it("keeps the six supported farmer languages explicit", () => {
    expect(SUPPORTED_LANGUAGES).toEqual(["en", "bn", "hi", "ta", "pa", "te"]);
  });

  it("accepts supported languages", () => {
    for (const language of SUPPORTED_LANGUAGES) {
      expect(isSupportedLanguage(language)).toBe(true);
    }
  });

  it("falls back to English for unsupported values", () => {
    expect(isSupportedLanguage("fr")).toBe(false);
    expect(normalizeLanguage("fr")).toBe("en");
  });
});
