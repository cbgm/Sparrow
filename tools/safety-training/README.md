# Sparrow Safety Training — Balanced Automatic Pipeline (Windows)

> **v10 note:** Aggregate precision/recall alone was not sufficient for Sparrow Safety. A deployed v9 linear model could pass the held-out gate while still confusing a received verification code with a credential request and missing natural German urgency/payment requests. v10 therefore adds focused EN/DE behavioral augmentation, a tiny nonlinear MLP classifier, and a strict product behavioral contract gate before Kotlin export.


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

## v10 — Focused behavioral augmentation + tiny nonlinear MLP heads

v10 adds a focused behavioral augmentation stage for the failure modes that a single linear hyperplane underfit:

- credential request vs. credential notification/status (for example, a message that merely contains a verification code),
- coercive urgency vs. ordinary deadlines/status,
- payment request vs. completed/payment-status discussion,
- each in English and German.

It then creates:

```text
artifacts\message-safety-mlp-model.json
artifacts\evaluation.json
artifacts\behavioral-gate.json
```

EmbeddingGemma stays frozen. Each Safety reason gets a tiny one-hidden-layer MLP (`128 -> 16/32 -> 1`). The trainer balances the binary training rows, tries a small hidden-size/regularization grid, and chooses a threshold using validation constraints plus the explicit product behavioral contract. The statistical test split is still held out from model fitting and is evaluated only after selection.

The selected combination must satisfy:

```text
validation precision >= 0.80
validation recall >= 0.50
validation FPR <= 0.02
```

Among valid combinations, the trainer chooses the best validation F1. The test split is never used for fitting or threshold selection. The behavioral contract is a strict product acceptance suite, not a statistical benchmark; it prevents exporting models that repeat known intent failures even when aggregate metrics look acceptable.

## Resume from the completed v9 dataset

If the v9 public labeling, original generated dataset, and embeddings already exist, do **not** rerun them. Use:

```powershell
powershell -ExecutionPolicy Bypass -File .\resume_behavioral_retraining.ps1
```

This reuses `data\processed\labeled.jsonl`, the existing generation caches, and compatible embeddings. It generates only the six focused behavioral packs, embeds only new accepted rows, retrains the MLP heads, runs both gates, and exports only when both pass.

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
artifacts\GeneratedMessageSafetyMlpModel.kt
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
artifacts\behavioral-gate.json
data\labels\auto_label_report.json
data\generated\report.json
data\generated\behavioral-report.json
models\embedding_gemma.metadata.json
artifacts\message-safety-mlp-model.json
artifacts\GeneratedMessageSafetyMlpModel.kt
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


## v11 behavioral-labeling performance

The behavioral retraining path is optimized for local Ollama GPUs without weakening the acceptance rule:

- focused contrastive generation defaults to 16 pairs per Qwen request instead of 8; malformed large structured responses still use the existing retry + recursive split fallback,
- validator A (`qwen3:8b` by default) runs across all candidates before validator B is loaded, avoiding A/B model swapping on GPUs that cannot keep both models resident,
- generated rows that already disagree with their intended four-label vector in pass A, or fall below the minimum confidence in pass A, are rejected immediately because they cannot possibly satisfy final dual-teacher acceptance,
- validator B (`gemma3:12b` by default) only processes pass-A survivors,
- generation and validation caches remain compatible, so an interrupted v10/v11 behavioral run can be resumed without deleting cache files.

For behavioral retraining, stop an older run with Ctrl+C and rerun `resume_behavioral_retraining.ps1`; completed generation/validation cache entries are reused.

## v12 conservative model reselection

If behavioral v11 reaches the held-out gate but a head (especially `payment_request`) misses the precision floor, do **not** lower the gate and do not rerun Ollama first. v12 model selection searches several effective positive training priors (`10%`, `20%`, `35%`, `50%`) while retaining every negative example, tries stronger regularization, and requires a validation precision margin above the deployment floor. This reduces the distribution shift caused by the old hard-coded 50/50 oversampling.

After a completed v11 run has reached at least stage 7, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\resume_model_selection.ps1
```

This does not run generation, labeling, embedding, clustering, or splitting.
