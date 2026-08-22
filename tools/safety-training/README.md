# Sparrow Safety Training — Balanced Automatic Pipeline (Windows)

This project builds Sparrow's four semantic Safety classifiers without manually labeling thousands of messages.

The normal workflow is now:

```text
real public SMS data
    -> normalize/deduplicate
    -> dual local teacher labeling
    -> targeted EN/DE contrast-pair generation
    -> dual-model validation of every generated message
    -> merge accepted real + generated rows
    -> Sparrow's exact MediaPipe EmbeddingGemma vectors
       (reuse old vectors; embed only new rows)
    -> near-duplicate clustering
    -> leakage-safe split with strong per-label support
    -> four logistic-regression heads
    -> evaluation
    -> quality gate
    -> ONLY if gate passes: Kotlin + parity export
```

No manual CSV labeling is required in the normal workflow.

## Learned labels

Only these semantic reasons are trained:

- `URGENT_ACTION_REQUEST`
- `CREDENTIAL_REQUEST`
- `PAYMENT_REQUEST`
- `PRIVATE_KEY_REQUEST`

Structural detections such as suspicious URLs, lookalike domains, mixed-script domains, IP-address links and URL shorteners remain deterministic in Sparrow.

## Why generated data is not trusted directly

The generator does **not** get to label its own data.

For every generated positive/negative pair:

1. the generator records the intended full four-label vector;
2. validator A independently classifies the message;
3. validator B independently classifies the message;
4. the row is accepted only when:
   - validator A and B agree;
   - both match the intended four-label vector exactly;
   - all returned confidences are at least the configured threshold.

Anything else is discarded automatically.

The default models are:

```text
generator:           qwen3:8b
validator A:         qwen3:8b
validator B:         gemma3:12b
```

`gemma3:12b` is intentionally a different model family from the generator. If your computer cannot run the 12B model, you can override it with `gemma3:4b`, but the default is the stronger independent check.

---

# COMPLETE WINDOWS / POWERSHELL MANUAL

## Step 1 — Open the correct folder

```powershell
cd C:\Users\Chris\AndroidStudioProjects\Sparrow\tools\safety-training
```

Check:

```powershell
Get-Location
```

The path must end in:

```text
Sparrow\tools\safety-training
```

Check the main files:

```powershell
Get-ChildItem
```

You should see at least:

```text
pyproject.toml
setup_windows.ps1
run_uci_training.ps1
generate_targeted.py
quality_gate.py
```

---

## Step 2 — Python 3.11

Check:

```powershell
py -3.11 --version
```

If it is missing:

```powershell
py install 3.11
```

Then verify again:

```powershell
py -3.11 --version
```

Expected:

```text
Python 3.11.x
```

---

## Step 3 — Ollama

Check:

```powershell
ollama --version
```

If missing, install Ollama for Windows, reopen PowerShell, then return to this folder.

Check the API:

```powershell
Invoke-RestMethod http://localhost:11434/api/tags
```

It must return data.

You do **not** need to pull the models manually. `run_uci_training.ps1` checks them and pulls anything missing.

Default downloads are approximately:

```text
qwen3:8b      several GB
gemma3:12b    ~8.1 GB
```

If your PC cannot run `gemma3:12b`, use the smaller override shown later.

---

## Step 4 — Set up the Python environment

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup_windows.ps1
```

This automatically:

1. checks Python 3.11;
2. creates `.venv` if needed;
3. installs/updates dependencies;
4. installs MediaPipe 1.x;
5. runs the tests.

Verify afterward:

```powershell
.\.venv\Scripts\python.exe --version
```

Expected:

```text
Python 3.11.x
```

Do not use global `python` for this project.

---

## Step 5 — Run everything

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1
```

That is the normal command. Do not manually run the individual Python scripts unless debugging.

### If your PC cannot run Gemma 3 12B

Use:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1 `
  -ValidatorModelB gemma3:4b
```

The run remains dual-model validated, but the second validator is weaker/smaller.

---

# What the 12 stages do

## 1/12 — EmbeddingGemma model + compatibility check

The pipeline keeps/downloads:

```text
models\embedding_gemma.task
models\embedding_gemma.metadata.json
```

It validates MediaPipe + model compatibility before doing expensive work.

Never delete the metadata casually. It records the exact model SHA used for classifier training.

## 2/12 — UCI SMS data

Creates/reuses:

```text
data\raw\uci_sms.jsonl
```

## 3/12 — Normalize public data

Creates:

```text
data\processed\unlabeled.jsonl
```

Exact duplicate text and empty rows are removed.

## 4/12 — Automatic public-data labeling

Creates:

```text
data\processed\public-labeled.jsonl
data\labels\review_queue.jsonl
data\labels\auto_label_report.json
data\labels\teacher_cache.jsonl
```

The review queue is excluded automatically. You do not have to open or edit it.

The teacher cache is resumable. Do not delete it unless you intentionally want to relabel everything.

## 5/12 — Targeted EN/DE contrast generation + validation

Default generation amount:

```text
200 pairs x 4 labels x 2 languages
= 1,600 pairs
= 3,200 candidate messages
```

Each pair contains:

```text
positive: target reason is truly present
negative: similar wording/topic but the dangerous request/pressure is absent
```

Examples conceptually:

```text
positive: "Schick mir den Code aus der SMS."
negative: "Schick den Code aus der SMS niemals an andere Personen."
```

and:

```text
positive: "Send me your recovery phrase."
negative: "Never send your recovery phrase to anyone."
```

Outputs:

```text
data\generated\validated.jsonl
data\generated\rejected.jsonl
data\generated\report.json
data\generated\generation_cache.jsonl
data\generated\validation_cache.jsonl
```

Accepted generated data is combined with public data into:

```text
data\processed\labeled.jsonl
```

Both generation and validation are resumable. Do not delete their cache files after an interruption.

## 6/12 — EmbeddingGemma vectors

Output:

```text
data\embeddings\labeled-128.npz
data\embeddings\metadata.json
```

The tool reuses existing vectors when:

- same exact Gemma SHA;
- same input prompt/mode;
- same 128 dimensions.

So after upgrading from the previous pipeline, your existing ~4,618 vectors are reused and only newly generated messages need Gemma inference.

Progress is printed:

```text
Embedding 1 / 2800
Embedding 50 / 2800
Embedding 100 / 2800
...
```

## 7/12 — Near-duplicate clustering

Creates:

```text
data\processed\clustered.jsonl
```

This prevents near-identical generated/public examples from leaking across train/test.

## 8/12 — Stronger leakage-safe split

Creates:

```text
data\processed\split.jsonl
```

Every label must now have at least:

```text
train       100 positives
validation   20 positives
test         20 positives
```

and matching negative support.

If this stage fails, add/diversify data. Do not weaken the guard simply to make training pass.

## 9/12 — Train four linear heads + choose thresholds from validation only

Creates:

```text
artifacts\message-safety-linear-model.json
artifacts\evaluation.json
```

EmbeddingGemma stays frozen. Only four tiny logistic-regression heads are trained.

For each label the trainer tries several logistic-regression regularization values (`C = 0.1, 0.3, 1, 3, 10`) and threshold candidates using the **validation split only**. The selected combination must satisfy the same deployment constraints used by the development gate:

```text
validation precision >= 0.80
validation recall >= 0.50
validation FPR <= 0.02
```

Among valid combinations, the trainer chooses the best validation F1. The test split is never used to choose `C` or the threshold. If validation itself cannot satisfy these constraints, training stops and tells you to add/diversify data or change the model family rather than tuning against the test set.


## Resume after a stage-11 quality-gate failure

If stages 1-8 already completed and the previous run only failed at the quality gate, v9 does **not** require Ollama generation or EmbeddingGemma to run again. Use:

```powershell
powershell -ExecutionPolicy Bypass -File .\resume_from_training.ps1
```

This reuses `split.jsonl` and the existing embeddings, retrains the linear heads with validation-only constraint selection, reevaluates the untouched test split, and exports only if the gate passes.

## 10/12 — Evaluation Markdown

Creates:

```text
artifacts\evaluation.md
```

## 11/12 — Quality gate

Creates:

```text
artifacts\quality-gate.json
```

Default development gate requires every label to have:

```text
test positives >= 20
test precision >= 0.80
test recall >= 0.50
test FPR <= 0.02
```

If any label fails, the PowerShell run stops **before Kotlin export**.

That is intentional. A weak model must not accidentally look production-ready just because training completed.

This automated gate still does not replace a future frozen human-reviewed real-world benchmark before shipping a security-sensitive warning system.

## 12/12 — Export only after gate passes

Creates:

```text
artifacts\GeneratedMessageSafetyLinearModel.kt
artifacts\embedding-parity.json
```

Do not integrate the Kotlin model into Sparrow until Android/Python embedding parity is checked.

---

# If the command is interrupted

Simply rerun:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1
```

The important caches are:

```text
data\labels\teacher_cache.jsonl
data\generated\generation_cache.jsonl
data\generated\validation_cache.jsonl
data\embeddings\labeled-128.npz
```

Public labeling, generation, validation and compatible embeddings are reused.

---

# Useful overrides

Generate more data per label/language:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1 `
  -PairsPerLabelLanguage 300
```

Use stricter teacher confidence:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1 `
  -MinConfidence 0.95
```

Use smaller independent validator:

```powershell
powershell -ExecutionPolicy Bypass -File .\run_uci_training.ps1 `
  -ValidatorModelB gemma3:4b
```

Do not lower the quality gate just to get an export.

---

# What to send back after a successful run

Send these files:

```text
artifacts\evaluation.md
artifacts\quality-gate.json
data\labels\auto_label_report.json
data\generated\report.json
models\embedding_gemma.metadata.json
artifacts\message-safety-linear-model.json
artifacts\embedding-parity.json
```

If stage 11 fails, send these instead:

```text
artifacts\evaluation.md
artifacts\quality-gate.json
data\generated\report.json
```

No manual training labels are required.

## Troubleshooting: invalid structured generation response

Local Ollama models can occasionally violate the requested JSON schema for a generation batch. The generator now recovers automatically:

1. retry the same batch with progressively lower sampling temperature;
2. if the batch still fails, split it into smaller batches;
3. continue recursively down to single-pair requests;
4. preserve all previously written generation and validation caches.

If a run was interrupted by `Ollama returned an invalid structured generation response`, do **not** delete:

```text
data\generated\generation_cache.jsonl
data\generated\validation_cache.jsonl
data\labels\teacher_cache.jsonl
```

After updating the tooling, simply rerun `run_uci_training.ps1`.

## Automatic post-clustering support refill (v8)

The balanced runner no longer aborts merely because validation/deduplication leaves a rare label below the split minimum.
After targeted generation, embedding, and near-duplicate clustering it measures the actual surviving support required by the
100/20/20 train/validation/test guard. If one or more labels are still short, it automatically increases generation only for
those labels, reuses all generation/validation/embedding caches, reclusters, and checks again. By default it adds 200 pairs
per language for each insufficient label per refill round, for up to 4 refill rounds. These can be changed with
`-RefillPairsPerLabelLanguage` and `-MaxRefillRounds`. The split requirements themselves are never lowered automatically.
