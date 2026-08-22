from __future__ import annotations

from collections import defaultdict
from pathlib import Path

import numpy as np
from sklearn.neighbors import NearestNeighbors

from .embedding import load_embeddings
from .io import read_jsonl, write_jsonl


class _UnionFind:
    def __init__(self, values: list[str]) -> None:
        self.parent = {value: value for value in values}

    def find(self, value: str) -> str:
        parent = self.parent[value]
        if parent != value:
            self.parent[value] = self.find(parent)
        return self.parent[value]

    def union(self, left: str, right: str) -> None:
        left_root = self.find(left)
        right_root = self.find(right)
        if left_root != right_root:
            self.parent[max(left_root, right_root)] = min(left_root, right_root)


def assign_near_duplicate_clusters(
    dataset_path: Path,
    embeddings_path: Path,
    output_path: Path,
    *,
    similarity_threshold: float = 0.985,
    neighbors: int = 8,
) -> dict[str, int | float]:
    rows = list(read_jsonl(dataset_path))
    ids, embeddings = load_embeddings(embeddings_path)
    by_id = {str(row["id"]): row for row in rows}
    if set(ids) != set(by_id):
        missing_dataset = set(ids) - set(by_id)
        missing_embeddings = set(by_id) - set(ids)
        raise ValueError(
            f"Dataset/embedding id mismatch: only_embeddings={len(missing_dataset)}, "
            f"only_dataset={len(missing_embeddings)}"
        )
    if not 0.9 <= similarity_threshold <= 1.0:
        raise ValueError("Near-duplicate similarity threshold must be between 0.9 and 1.0")

    order = {record_id: index for index, record_id in enumerate(ids)}
    matrix = np.vstack([embeddings[order[record_id]] for record_id in ids])
    uf = _UnionFind(ids)

    n_neighbors = min(max(2, neighbors), len(ids))
    model = NearestNeighbors(metric="cosine", algorithm="brute", n_neighbors=n_neighbors)
    model.fit(matrix)
    distances, indices = model.kneighbors(matrix, return_distance=True)

    for left_index, (row_distances, row_indices) in enumerate(zip(distances, indices, strict=True)):
        left_id = ids[left_index]
        for distance, right_index in zip(row_distances[1:], row_indices[1:], strict=True):
            similarity = 1.0 - float(distance)
            if similarity >= similarity_threshold:
                uf.union(left_id, ids[int(right_index)])

    groups: dict[str, list[str]] = defaultdict(list)
    for record_id in ids:
        groups[uf.find(record_id)].append(record_id)

    canonical_cluster = {
        record_id: f"cluster-{min(members)}"
        for members in groups.values()
        for record_id in members
    }
    for row in rows:
        row["cluster_id"] = canonical_cluster[str(row["id"])]

    write_jsonl(output_path, rows)
    clustered_records = sum(len(members) for members in groups.values() if len(members) > 1)
    return {
        "records": len(rows),
        "clusters": len(groups),
        "records_in_multi_record_clusters": clustered_records,
        "similarity_threshold": similarity_threshold,
    }
