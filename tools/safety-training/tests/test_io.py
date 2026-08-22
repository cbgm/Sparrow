from sparrow_safety_training.io import normalize_text, normalized_text_key


def test_normalize_text_collapses_unicode_and_whitespace() -> None:
    assert normalize_text("  Hello\u00a0  world\n") == "Hello world"
    assert normalize_text("ＡＢＣ") == "ABC"


def test_normalized_text_key_is_case_insensitive() -> None:
    assert normalized_text_key("Überweisung") == normalized_text_key("ÜBERWEISUNG")
