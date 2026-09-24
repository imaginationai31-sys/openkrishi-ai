const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { GoogleGenAI } = require("@google/genai");

initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";

const LANGUAGES = {
  en: "English",
  bn: "Bengali",
  hi: "Hindi",
  ta: "Tamil",
  pa: "Punjabi",
  te: "Telugu",
};

const CATEGORIES = new Set(["rice", "peanut", "vegetables", "flowers"]);

const ADVISORY_SCHEMA = {
  type: "object",
  properties: {
    answer: { type: "string" },
    observations: { type: "array", items: { type: "string" } },
    recommendations: { type: "array", items: { type: "string" } },
    uncertainties: { type: "array", items: { type: "string" } },
    confidence: { type: "string", enum: ["low", "medium", "high"] },
  },
  required: ["answer", "observations", "recommendations", "uncertainties", "confidence"],
};

function json(res, status, body) {
  res.status(status).set("Content-Type", "application/json").send(JSON.stringify(body));
}

function cors(res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Firebase-AppCheck");
}

async function requireFirebaseUser(req) {
  const header = req.get("Authorization") || "";
  if (!header.startsWith("Bearer ")) {
    const error = new Error("Missing Firebase ID token.");
    error.code = "UNAUTHENTICATED";
    throw error;
  }
  return getAuth().verifyIdToken(header.slice("Bearer ".length));
}

function validate(body) {
  if (!body || typeof body.query !== "string" || !body.query.trim() || body.query.length > 2000) {
    return "query must be a non-empty string of at most 2000 characters.";
  }
  if (!Object.prototype.hasOwnProperty.call(LANGUAGES, body.language)) {
    return "language must be one of en, bn, hi, ta, pa, te.";
  }
  if (body.crop_category != null && !CATEGORIES.has(body.crop_category)) {
    return "crop_category must be one of rice, peanut, vegetables, flowers.";
  }
  if (body.crop_name != null && String(body.crop_name).length > 100) {
    return "crop_name is too long.";
  }
  if (body.growth_stage != null && String(body.growth_stage).length > 100) {
    return "growth_stage is too long.";
  }
  if (body.location != null && String(body.location).length > 200) {
    return "location is too long.";
  }
  return null;
}

function buildPrompt(body) {
  const languageName = LANGUAGES[body.language];
  return `You are OpenKrishi AI, a cautious agricultural advisory assistant for Indian farmers.

Answer the farmer's question using the supplied context. This is advisory guidance, not a definitive diagnosis.

FARMER QUERY:
${body.query}

CROP CATEGORY: ${body.crop_category || "unspecified crop"}
SPECIFIC CROP OR VARIETY: ${body.crop_name || "not specified"}
GROWTH STAGE: ${body.growth_stage || "not provided"}
LOCATION: ${body.location || "not provided"}

STRICT SAFETY RULES:
- Do not claim a disease, pest, nutrient deficiency, or other diagnosis as certain.
- Describe possible causes only when supported by the farmer's information.
- Do not prescribe pesticides, insecticides, fungicides, herbicides, chemical sprays, chemical names, application rates, or large fertilizer doses.
- Give practical low-risk checks and next steps.
- If important information is missing, state exactly what should be checked or provided.
- If growth stage is missing, explicitly mention that the growth stage is needed for more specific guidance.
- If location is provided, acknowledge it but do not invent local weather, soil, pest alerts, or government guidance.
- If location is not provided, state that local conditions cannot be considered.
- Keep confidence conservative; use low unless the information is unusually clear.
- Return ONLY JSON matching the supplied response schema.
- Every natural-language field MUST be fully written in ${languageName}. Do not mix languages.
- Keep crop and variety names recognizable to farmers.

Return concise but useful observations, safe recommendations, and uncertainties.`;
}

exports.advisory = onRequest(
  {
    region: "asia-south1",
    timeoutSeconds: 60,
    memory: "256MiB",
    secrets: [GEMINI_API_KEY],
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") return res.status(204).send("");
    if (req.method !== "POST") return json(res, 405, { error: "Method not allowed." });

    try {
      const user = await requireFirebaseUser(req);
      const error = validate(req.body);
      if (error) return json(res, 400, { error });

      const apiKey = GEMINI_API_KEY.value();
      if (!apiKey) return json(res, 503, { error: "Gemini is not configured." });

      const ai = new GoogleGenAI({ apiKey });
      const response = await ai.models.generateContent({
        model: GEMINI_MODEL,
        contents: buildPrompt(req.body),
        config: {
          responseMimeType: "application/json",
          responseSchema: ADVISORY_SCHEMA,
          maxOutputTokens: 2200,
        },
      });

      let result;
      try {
        result = JSON.parse(response.text || "{}");
      } catch (parseError) {
        logger.error("Gemini returned invalid JSON", parseError);
        return json(res, 502, { error: "Gemini returned an invalid advisory response." });
      }

      if (!result.answer) return json(res, 502, { error: "Gemini returned an empty advisory." });

      return json(res, 200, {
        ...result,
        language: req.body.language,
        confidence: result.confidence === "high" || result.confidence === "medium" ? result.confidence : "low",
        safety: {
          status: "caution",
          reason: "AI guidance is informational and should be confirmed with local agronomy guidance when decisions could affect crop health or inputs.",
        },
        source_references: [],
        location: req.body.location ? String(req.body.location).trim() : null,
        crop_category: req.body.crop_category || null,
        crop_name: req.body.crop_name || null,
        platform: "firebase",
        model: GEMINI_MODEL,
      });
    } catch (error) {
      logger.error("Firebase advisory failed", error);
      if (error.code === "UNAUTHENTICATED") {
        return json(res, 401, { error: "Firebase authentication required." });
      }
      return json(res, 503, { error: "Gemini advisory provider is temporarily unavailable." });
    }
  },
);
