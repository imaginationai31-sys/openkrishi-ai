const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "https://openkrishi-ai-api.onrender.com").replace(/\/$/, "");

export type VoiceAdvisoryResult = {
  transcription: { text: string; normalized_text: string; language: string; confidence: string };
  advisory: {
    answer: string;
    observations: string[];
    recommendations: string[];
    uncertainties: string[];
    confidence: string;
    language?: string;
  };
  audio?: { mime_type: string; language: string; base64: string };
};

export async function sendVoiceAdvisory(
  audio: Blob,
  language: string,
  cropCategory?: string,
  growthStage?: string,
): Promise<VoiceAdvisoryResult> {
  const form = new FormData();
  form.append("file", audio, "voice.webm");
  form.append("language", language);
  if (cropCategory) form.append("crop_category", cropCategory);
  if (growthStage?.trim()) form.append("growth_stage", growthStage.trim());

  const response = await fetch(`${API_BASE_URL}/api/v1/voice/advisory`, {
    method: "POST",
    body: form,
  });

  if (!response.ok) {
    let message = "Voice advisory failed. Please try again.";
    try {
      const payload = await response.json();
      if (typeof payload?.detail === "string") message = payload.detail;
    } catch {}
    throw new Error(message);
  }
  return response.json() as Promise<VoiceAdvisoryResult>;
}

export function playBase64Audio(base64: string, mimeType: string): HTMLAudioElement {
  const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  const blob = new Blob([bytes], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const audio = new Audio(url);
  audio.addEventListener("ended", () => URL.revokeObjectURL(url), { once: true });
  void audio.play();
  return audio;
}
