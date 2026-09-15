"""Conservative symptom rules enriched with structured crop-stage context."""

from typing import Any

from .crop_intelligence import get_crop_profile, get_stage_guidance


RULES: list[dict[str, Any]] = [
    {
        "keywords": ("yellow leaf", "yellow leaves", "yellowing", "yellow leafs"),
        "crops": ("rice", "peanut", "vegetables", "flowers"),
        "observation": "Leaf yellowing can have multiple causes, including nutrient stress, water stress, root problems, or pests/disease.",
        "recommendations": [
            "Check whether yellowing starts on older or newer leaves and whether it is uniform or patchy.",
            "Check soil moisture and drainage before changing irrigation or adding fertilizer.",
            "Inspect both sides of leaves and the plant base for insects, lesions, or other visible signs.",
            "Avoid applying a pesticide or large fertilizer dose until the likely cause is confirmed.",
        ],
        "uncertainty": "Leaf color alone is not enough to identify the cause; growth stage, field conditions, and visible symptoms are needed.",
    },
    {
        "keywords": ("wilting", "wilt", "drooping", "droop"),
        "crops": ("rice", "peanut", "vegetables", "flowers"),
        "observation": "Wilting or drooping can result from water stress, root damage, heat, or disease.",
        "recommendations": [
            "Check soil moisture near the root zone and look for standing water or unusually dry soil.",
            "Inspect stems and roots for damage, rot, or discoloration where practical.",
            "Compare affected plants with healthy plants in the same area to identify whether the problem is localized.",
            "Do not increase irrigation automatically if the soil is already saturated.",
        ],
        "uncertainty": "Wilting alone does not identify the underlying cause.",
    },
    {
        "keywords": ("spots", "spot", "brown spots", "black spots", "leaf spot"),
        "crops": ("rice", "peanut", "vegetables", "flowers"),
        "observation": "Leaf spots can be associated with disease, physical injury, nutrient stress, or environmental damage.",
        "recommendations": [
            "Check whether spots have a defined edge, concentric rings, a yellow halo, or visible fungal growth.",
            "Inspect several affected and unaffected leaves rather than judging from one leaf.",
            "Keep foliage reasonably dry and avoid unnecessary overhead watering while the cause is being investigated.",
            "Use a clear photo of affected leaves for more reliable visual assessment.",
        ],
        "uncertainty": "A visual description without an image and field context is not sufficient for a reliable disease identification.",
    },
]


def get_knowledge(
    query: str,
    crop_category: str | None = None,
    growth_stage: str | None = None,
) -> dict[str, Any]:
    text = query.lower()
    crop = crop_category or ""
    stage = (growth_stage or "").strip().lower()

    for rule in RULES:
        if crop in rule["crops"] and any(keyword in text for keyword in rule["keywords"]):
            recommendations = list(rule["recommendations"])
            uncertainties = [rule["uncertainty"]]

            if stage:
                recommendations.insert(
                    0,
                    f"Use the crop's current growth stage ({growth_stage}) when comparing the symptom with local agronomy guidance.",
                )

                stage_guidance = get_stage_guidance(crop, growth_stage)
                if stage_guidance:
                    recommendations.insert(1, stage_guidance)

                if crop == "rice" and "tillering" in stage and any(
                    keyword in text for keyword in rule["keywords"]
                ):
                    recommendations.insert(
                        2,
                        "At the tillering stage, compare affected plants with healthy plants and check whether yellowing is concentrated on older leaves or across the canopy.",
                    )
            else:
                uncertainties.append(
                    "Growth stage was not provided; this limits how specifically the symptom can be interpreted."
                )

            return {
                "observations": [rule["observation"]],
                "recommendations": recommendations,
                "uncertainties": uncertainties,
            }

    profile = get_crop_profile(crop)
    if profile and not stage:
        return {
            "observations": [],
            "recommendations": [],
            "uncertainties": [
                "No specific symptom rule matched this question. More crop, growth-stage, and field information is needed.",
                f"Supported {profile['label']} stages include: {', '.join(profile['stages'])}.",
            ],
        }

    return {
        "observations": [],
        "recommendations": [],
        "uncertainties": [
            "No specific symptom rule matched this question. More crop, growth-stage, and field information is needed."
        ],
    }
