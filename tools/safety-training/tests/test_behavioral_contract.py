from pathlib import Path

from sparrow_safety_training.io import read_jsonl


def test_runtime_contract_contains_reported_regressions() -> None:
    path = Path(__file__).parents[1] / "data" / "challenge" / "runtime_contract.jsonl"
    rows = list(read_jsonl(path))
    by_text = {row["text"]: row for row in rows}
    assert by_text["Your verification code is 123456."]["credential_request"] is False
    assert by_text["Handle sofort, sonst wird dein Konto gesperrt."]["urgent_action_request"] is True
    assert by_text["Überweise mir 200 Euro auf dieses Konto."]["payment_request"] is True
