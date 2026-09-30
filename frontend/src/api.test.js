import { describe, expect, it, vi } from "vitest";
import { buildAdvisoryRequest, postJson } from "./api.js";

describe("PWA API client", () => {
  it("builds the advisory payload without changing selected crop fields", () => {
    expect(
      buildAdvisoryRequest({
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

    await expect(postJson("/api/v1/advisory", { query: "test" }, fetchImpl)).resolves.toEqual({
      answer: "Check soil moisture.",
    });
  });

  it("surfaces sanitized API error details", async () => {
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ detail: "Invalid request." }),
    });

    await expect(postJson("/api/v1/advisory", {}, fetchImpl)).rejects.toThrow("Invalid request.");
  });
});
