# Dataset and model provenance

`source_manifest.json` is the canonical source list for this training project.

Rules:

1. Do not commit downloaded corpora into the Sparrow repository.
2. Preserve every row's `source` and `source_id` through normalization and training.
3. Keep source-specific attribution when publishing a derived dataset or model.
4. A wrapper dataset's license does not erase upstream attribution or license obligations.
5. Gated Hugging Face resources must only be downloaded after accepting their access terms.
6. Re-check source cards/licenses before a public release; this manifest records the state checked on 2026-08-20, not a permanent legal guarantee.
