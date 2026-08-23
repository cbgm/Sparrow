from pathlib import Path

import numpy as np

from sparrow_safety_training.clustering import assign_near_duplicate_clusters
from sparrow_safety_training.io import read_jsonl, write_jsonl


def test_near_duplicates_share_cluster(tmp_path: Path) -> None:
    dataset = tmp_path / "data.jsonl"
    embeddings = tmp_path / "embeddings.npz"
    output = tmp_path / "clustered.jsonl"
    write_jsonl(
        dataset,
        [
            {"id": "a", "text": "one"},
            {"id": "b", "text": "one variant"},
            {"id": "c", "text": "different"},
        ],
    )
    matrix = np.asarray([[1.0, 0.0], [0.99999, 0.001], [0.0, 1.0]], dtype=np.float32)
    matrix /= np.linalg.norm(matrix, axis=1, keepdims=True)
    np.savez_compressed(embeddings, ids=np.asarray(["a", "b", "c"]), embeddings=matrix)

    assign_near_duplicate_clusters(dataset, embeddings, output, similarity_threshold=0.99, neighbors=3)
    rows = {row["id"]: row for row in read_jsonl(output)}
    assert rows["a"]["cluster_id"] == rows["b"]["cluster_id"]
    assert rows["a"]["cluster_id"] != rows["c"]["cluster_id"]
