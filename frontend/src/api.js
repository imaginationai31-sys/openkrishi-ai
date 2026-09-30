const DEFAULT_API_BASE = "https://openkrishi-ai-api.onrender.com";

function getApiBase() {
  return window.OPENKRISHI_API_BASE || DEFAULT_API_BASE;
}

function buildAdvisoryRequest({ query, language, cropCategory, cropName, growthStage }) {
  return {
    query: query.trim(),
    language,
    crop_category: cropCategory || null,
    crop_name: cropName || null,
    growth_stage: growthStage || null,
  };
}

async function postJson(path, body, fetchImpl = fetch) {
  const response = await fetchImpl(`${getApiBase()}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const detail = typeof data.detail === "string" ? data.detail : "Request failed.";
    throw new Error(detail);
  }
  return data;
}

window.OpenKrishiApi = { getApiBase, buildAdvisoryRequest, postJson };
