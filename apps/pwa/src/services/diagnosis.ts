const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "https://openkrishi-ai-api.onrender.com").replace(/\/$/, "");

export type VisionAssessment = {
  status: string;
  image?: { content_type?: string; size_bytes?: number };
  crop_category?: string | null;
  crop_name?: string | null;
  growth_stage?: string | null;
  vision: {
    observations: string[];
    possible_causes: string[];
    recommendations: string[];
    uncertainties: string[];
    confidence: string;
    safety?: { status?: string; reasons?: string[] };
  };
  advisory: {
    answer: string;
    observations: string[];
    recommendations: string[];
    uncertainties: string[];
    confidence: string;
    safety?: { status?: string; reasons?: string[] };
  };
};

export async function assessCropImage(
  file: File,
  options: { cropCategory: string; language: string; growthStage?: string },
): Promise<VisionAssessment> {
  const form = new FormData();
  form.append("file", file);
  form.append("crop_category", options.cropCategory);
  form.append("language", options.language);
  if (options.growthStage?.trim()) form.append("growth_stage", options.growthStage.trim());

  const response = await fetch(`${API_BASE_URL}/api/v1/vision/assess`, {
    method: "POST",
    body: form,
  });

  if (!response.ok) {
    let message = "Crop assessment failed. Please try again.";
    try {
      const payload = await response.json();
      if (typeof payload?.detail === "string") message = payload.detail;
    } catch {
      // Keep the safe generic message when the server response is not JSON.
    }
    throw new Error(message);
  }

  return response.json() as Promise<VisionAssessment>;
}
