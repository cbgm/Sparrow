from __future__ import annotations

# These are deliberately stronger than the early prototype guards. With the
# targeted contrastive generator enabled, every label should have enough
# independent support to make validation/test metrics less fragile.
MIN_POSITIVES_BY_SPLIT = {
    "train": 100,
    "validation": 20,
    "test": 20,
}

MIN_NEGATIVES_BY_SPLIT = {
    "train": 100,
    "validation": 20,
    "test": 20,
}
