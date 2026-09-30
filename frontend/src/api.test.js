globalThis.window = globalThis;

import { beforeEach, describe, expect, it, vi } from "vitest";

describe("PWA API client", () => {
  beforeEach(async () => {
    window.OPENKRISHI_API_BASE = "https://example.test";
    await import("./api.js");
  });

  it("builds the advisory payload without changing selected crop fields", () => {
    expect(
      window.OpenKrishiApi.buildAdvisoryRequest({
        query: "  yellow leaves  ",
        language: "bn",
        cropCategory: "rice",
        cropName: "Swarna",
        growthStage: "tillering",
      }),
    ).toEqual({
      query: "yellow leaves",
      language: "bn",
      crop_category: "rice",
      crop_name: "Swarna",
      growth_stage: "tillering",
    });
  });

  it("returns JSON on successful requests", async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ answer: "Check soil moisture." }),
    });

    await expect(
      window.OpenKrishiApi.postJson("/api/v1/advisory", { query: "test" }, fetchImpl),
    ).resolves.toEqual({
      answer: "Check soil moisture.",
    });
  });

  it("surfaces sanitized API error details", async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ detail: "Invalid request." }),
    });

    await expect(window.OpenKrishiApi.postJson("/api/v1/advisory", {}, fetchImpl)).rejects.toThrow(
      "Invalid request.",
    );
  });
});
