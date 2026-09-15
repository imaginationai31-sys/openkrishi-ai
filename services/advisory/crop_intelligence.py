"""Structured crop and growth-stage intelligence for OpenKrishi AI.

This module stores conservative crop-stage context separately from symptom
rules so the advisory engine can evolve without embedding crop facts in the
routing layer. It intentionally avoids pesticide rates and disease claims.
"""

from typing import Any


CROP_INTELLIGENCE: dict[str, dict[str, Any]] = {
    "rice": {
        "label": "Rice",
        "stages": ("seedling", "vegetative", "tillering", "panicle initiation", "flowering", "grain filling", "maturity"),
        "stage_checks": {
            "seedling": "Check establishment, plant stand, soil moisture, and early leaf color.",
            "vegetative": "Check leaf color, plant vigor, weeds, water conditions, and visible pest damage.",
            "tillering": "Compare affected plants with healthy plants and check tiller number, leaf color, and field water conditions.",
            "panicle initiation": "Check canopy condition, water management, and whether symptoms are spreading to newer leaves.",
            "flowering": "Check flowering uniformity, canopy health, moisture conditions, and visible disease or pest signs.",
            "grain filling": "Check canopy health, grain development, water conditions, and late-season symptom spread.",
            "maturity": "Check grain maturity, lodging, remaining leaf health, and harvest readiness.",
        },
    },
    "peanut": {
        "label": "Peanut",
        "stages": ("emergence", "vegetative", "flowering", "pegging", "pod development", "maturity"),
        "stage_checks": {
            "emergence": "Check plant stand, soil crusting, moisture, and early seedling damage.",
            "vegetative": "Check canopy growth, leaf color, weeds, soil moisture, and visible leaf or insect damage.",
            "flowering": "Check flowering, canopy health, moisture conditions, and leaf symptoms.",
            "pegging": "Check soil moisture and inspect lower canopy and plant base for visible stress or damage.",
            "pod development": "Check soil moisture, canopy health, and whether symptoms are localized or spreading.",
            "maturity": "Check pod development, plant condition, and harvest readiness rather than treating leaf symptoms alone.",
        },
    },
    "vegetables": {
        "label": "Vegetables",
        "stages": ("nursery/seedling", "vegetative", "flowering", "fruiting", "harvest"),
        "stage_checks": {
            "nursery/seedling": "Check emergence, damping-off-like symptoms, moisture, light, and seedling vigor.",
            "vegetative": "Check new and older leaves, canopy growth, irrigation, weeds, and visible pests.",
            "flowering": "Check flower development, canopy health, moisture, and visible pest or disease symptoms.",
            "fruiting": "Check fruit development, leaf health, moisture, and whether symptoms are concentrated around fruit or foliage.",
            "harvest": "Check maturity, marketable quality, harvest readiness, and whether symptoms affect new growth or harvested parts.",
        },
    },
    "flowers": {
        "label": "Flowers",
        "stages": ("nursery/seedling", "vegetative", "bud development", "flowering", "post-flowering"),
        "stage_checks": {
            "nursery/seedling": "Check establishment, moisture, light, and early leaf or stem damage.",
            "vegetative": "Check new growth, leaf color, irrigation, drainage, and visible pests.",
            "bud development": "Check bud formation, new growth, moisture conditions, and whether symptoms are spreading.",
            "flowering": "Check flower quality, bud/flower damage, canopy health, and environmental stress.",
            "post-flowering": "Check remaining foliage, plant recovery, seed/fruit development where applicable, and harvest or pruning needs.",
        },
    },
}


def get_crop_profile(crop_category: str | None) -> dict[str, Any] | None:
    """Return a copy of the supported crop profile, or None for unknown crops."""
    if not crop_category or crop_category not in CROP_INTELLIGENCE:
        return None
    profile = CROP_INTELLIGENCE[crop_category]
    return {
        "label": profile["label"],
        "stages": list(profile["stages"]),
        "stage_checks": dict(profile["stage_checks"]),
    }


def get_stage_guidance(crop_category: str | None, growth_stage: str | None) -> str | None:
    """Return stage-specific observation guidance when the stage is recognized."""
    profile = get_crop_profile(crop_category)
    if not profile or not growth_stage:
        return None

    stage = growth_stage.strip().lower()
    for known_stage, guidance in profile["stage_checks"].items():
        if known_stage.lower() == stage:
            return guidance
    return None
