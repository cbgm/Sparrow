from sparrow_safety_training.behavioral_augmentation import FOCUS_PACKS


def test_behavioral_focuses_cover_reported_failure_modes_in_both_languages() -> None:
    focuses = {(item.target_label, item.language) for item in FOCUS_PACKS}
    assert ("credential_request", "en") in focuses
    assert ("credential_request", "de") in focuses
    assert ("urgent_action_request", "en") in focuses
    assert ("urgent_action_request", "de") in focuses
    assert ("payment_request", "en") in focuses
    assert ("payment_request", "de") in focuses
