"""Offline training utilities for Sparrow Safety."""

LABELS = (
    "urgent_action_request",
    "credential_request",
    "payment_request",
    "private_key_request",
)

EMBEDDING_DIMENSIONS = 128
DEFAULT_MODEL_ID = "google/embeddinggemma-300m"
DEFAULT_INPUT_MODE = "sentence_similarity"
DEFAULT_EMBEDDING_BACKEND = "mediapipe"
MEDIAPIPE_MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/text_embedder/"
    "embedding_gemma/int4int8/latest/embedding_gemma.task"
)
