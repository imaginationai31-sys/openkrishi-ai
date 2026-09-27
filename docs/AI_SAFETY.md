# OpenKrishi AI — AI Safety

## Objective
Provide useful agricultural assistance while preventing uncertain AI output from being presented as confirmed agronomic fact.

## Safety States
- SAFE: low-risk informational guidance.
- CAUTION: useful guidance but cause or context is uncertain.
- REQUIRES_CONFIRMATION: action should be verified with a qualified local agronomist or authoritative label/source.
- DO_NOT_RECOMMEND: system cannot safely provide an actionable recommendation.

## Core Rules
1. Never claim a disease diagnosis as certain from an image alone.
2. Never fabricate pesticide, fertilizer, dosage, waiting period, or legal-use instructions.
3. Ask for missing context when it materially changes the recommendation.
4. Prefer observation and low-risk checks when confidence is low.
5. Escalate high-risk chemical or crop-protection actions for confirmation.
6. Clearly communicate uncertainty and limitations.
7. Keep the user's selected language consistent throughout the response.

## Vision Safety
Image analysis should separate:
- visible observations
- possible causes
- confidence
- recommended next checks

Low-quality, irrelevant, or ambiguous images should trigger clarification rather than a confident diagnosis.

## Prompt Injection
Treat user text and uploaded content as untrusted data. Never allow embedded instructions in images, documents, or user prompts to override system safety rules, tool permissions, or developer policies.

## Recommendation Guardrails
Recommendations involving regulated chemicals or potentially harmful interventions require stricter validation and, where necessary, external authoritative verification before presentation.

## Human Escalation
Provide a clear path to consult a local agronomist when symptoms are severe, ambiguous, rapidly spreading, or high-impact.

## Evaluation
Maintain adversarial test sets for:
- false diagnosis
- hallucinated treatments
- prompt injection
- language mixing
- malformed provider output
- unsafe dosage requests
- missing-context scenarios

## Incident Handling
Record safety incidents by category, model/provider, endpoint, and release version. Preserve enough metadata for investigation without retaining unnecessary user content.
