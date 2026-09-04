# 📽️ Video Face Detection & Intelligent Collage Generator

An Android application that scans videos to detect and track unique faces using ML Kit and MobileFaceNet, clusters appearances using an adaptive two-stage DBSCAN algorithm, and generates interactive portrait collages.


---

## 🛠️ Tech Stack & Libraries

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Asynchronous & Reactive:** Kotlin Coroutines & `StateFlow`
- **Machine Learning & Computer Vision:**
  - **Face Detection:** Google ML Kit Face Detection (Accurate Mode)
  - **Face Embeddings:** MobileFaceNet (TensorFlow Lite / LiteRT, on-device)
- **Face Clustering Engine:** Custom Two-Stage Agglomerative DBSCAN Algorithm
- **Media Storage & Export:** Android `MediaStore` API & Native Share Intent

---

## 🏗️ Architecture & Project Flow

### General Flow

`Video Selection` ➔ `Frame Extraction & Face Detection` ➔ `Feature Embedding Extraction` ➔ `Two-Stage DBSCAN Face Clustering` ➔ `Appearance Summary` ➔ `Interactive Collage Editor` ➔ `Export / Share`

### Detailed Flow

1. **Video Upload (`VideoUploadScreen`)**
   User selects a video via the system media picker. `VideoCollageViewModel` receives the URI and starts the pipeline.

2. **Frame Extraction & Face Detection (`VideoProcessor`)**
   `MediaMetadataRetriever` extracts frames every `500ms` (2 fps). ML Kit detects faces and crops each face with a generous `0.20f` margin — never a tight bounding-box crop, since tight crops hurt both embedding quality and collage image quality.

3. **Embedding Generation (`MobileFaceNetEmbedder`)**
   Each face crop is resized to 112×112 and passed through MobileFaceNet (TFLite) to produce a normalized FloatArray embedding.

4. **Face Clustering (`DbscanClusterer`)**
   - **Stage 1 — Seed:** Tight DBSCAN (`eps = 0.15`) forms high-confidence clusters, avoiding early chaining.
   - **Stage 2 — Assign:** Leftover noise points are assigned to the nearest seed centroid if within `eps = 0.35`.
   - **Stage 3 — Merge:** Clusters with close centroids are fused using an **adaptive** threshold (see below) to eliminate duplicate identities from the same person.

5. **Appearance Tracking (`AppearanceTracker`)**
   Detections for each cluster are sorted by timestamp. A gap of more than `1000ms` between detections starts a new appearance; anything closer is treated as the same continuous appearance.

6. **Appearance Summary (`AppearanceSummaryScreen`)**
   Shows each detected person with their photo and total appearance count. User taps **"Make Collage →"**.

7. **Collage Editor (`CollageEditorScreen`)**
   Auto-selects a portrait layout based on face count (1–8), lets the user switch between layout templates and swap photos between slots, then export.

---

## ⚙️ Final Parameters

| Parameter | Value | What it does |
| :--- | :--- | :--- |
| `sampleIntervalMs` | `500L` | How often a frame is pulled from the video (2 fps) |
| `gapToleranceMs` | `1000L` | Max silent gap before a new appearance is started |
| `seedEps` | `0.15f` | How close two embeddings must be to seed the same cluster |
| `assignEps` | `0.35f` | How close a leftover point must be to join an existing cluster |
| `mergeEps` | **Adaptive**, clamped `0.20f–0.38f` | How close two cluster centroids must be to be fused into one person |
| `minPoints` | `2` | Minimum detections needed to form a cluster |

**In plain terms:** `eps` is just "how far apart can two face embeddings be and still count as the same person." Too small → the same person gets split into multiple people. Too large → different people get merged into one.

---

## 🧪 How I Got Here — Experiment Log

I didn't guess these numbers — I tuned them by running Sample 1 (5 known people, ground-truth verified) over and over, watching what broke, and fixing that specific failure.

**Baseline problem:** App found 3 people instead of 5 (counts 6+4+2=12 instead of the correct 20 appearances).
**Diagnosis:** DBSCAN with `eps=0.4` was chaining — person A connects to B at distance 0.22, B connects to C at 0.15, and so on, until totally different people end up in one cluster through a chain of "close enough" hops.

**Round 1 — tightening eps (0.1 to 0.4 tested):**
Tightening `eps` stopped the chaining, but now the opposite problem showed up: at tight values (0.15–0.2) one person kept vanishing entirely — her points were too spread out to form a cluster on their own. Loosening back up brought her back but reopened the merging bug. No single eps value satisfied both needs — **conclusion: fixed single-linkage DBSCAN structurally can't solve this alone.**

**Round 2 — two-stage clustering introduced:**
Split into Seed (tight eps, no chaining) → Assign (rescue lost points via nearest centroid) → Merge (fuse split fragments back together). First real success: got all 5 people for the first time, at the cost of a few duplicate splits (7 clusters instead of 5).

**Round 3 — merge threshold set too loose (`mergeEps = 0.40`):**
Over-merged everything back down to 3 people. The merge step itself was chaining now, just at the cluster level instead of the point level — each merge shifted a cluster's centroid, making it "close enough" to swallow the next one in a sequence.

**Round 4 — merge threshold tightened, sequential merge fixed (`mergeEps = 0.30`):**
Switched to always merging the single closest pair first (not cluster-ID order). Got 5 clusters — but a close look at timestamps showed 2 of them were actually different people wrongly fused (Person D and E merged into Person S). Appearance counts were now wrong even though the cluster count looked "right."

**Round 5 — asymmetric/minimum-pair gating tested (`minPairEps=0.15` + `mergeEps=0.30/0.35`):**
Fixed the wrong fusion, but reintroduced the earlier duplicate-split problem — back to 6–7 clusters. This confirmed a real trade-off: a stricter merge gate avoids wrong fusions but leaves more duplicate fragments; a looser gate reduces duplicates but risks wrong fusions.

**Round 6 — tested a fixed `mergeEps=0.22` across all 3 sample videos (not just Sample 1):**
Worked reasonably on Sample 1 (7 clusters, all 5 identities present, 2 duplicates). Failed differently on Sample 2 (5 clusters but 2 people missing/fused) and Sample 3 (6 clusters, 1 person missing). **Conclusion: a fixed constant tuned on one video does not transfer to other videos** — different lighting and face variety shift the embedding distances.

**Round 7 — adaptive per-video threshold (final approach):**
Instead of a hardcoded `mergeEps`, the app now looks at that video's own centroid-to-centroid distances at runtime, sorts them, and finds the biggest natural gap in the data — same-person distances cluster low, different-person distances cluster high, and the gap between those two groups becomes the threshold for that specific video, automatically, with no manual tuning per video. Clamped to `0.20–0.38` as a safety bound based on values observed across all 3 samples (0.253, 0.360, 0.336).

**Result across all 3 samples with the adaptive approach:**

| Sample | People found | Notes |
| :--- | :--- | :--- |
| Sample 1 | 7 clusters | All 5 identities present, 2 duplicated |
| Sample 2 | 5 clusters | 2 identities missing / fused into an oversized cluster, 2 duplicated |
| Sample 3 | 6 clusters | 1 identity missing, 2 duplicated |

---

## ⚠️ Known Limitation

The clustering is not perfect on every video. The core remaining issue is that some individuals (e.g. a more expressive speaker with more head movement) naturally produce more spread-out embeddings than others in the *same* video, so no single distance threshold — fixed or adaptive — perfectly separates "same person, different pose" from "different person" in every case. The adaptive threshold removes the need for manual per-video tuning and improves consistency over a fixed constant, but does not guarantee an exact person count.

The two most likely paths to further improve this, left as future work: a stronger/higher-dimensional face embedding model, and/or a higher frame sampling rate to give the clusterer more data points per person.

---

## 🎨 Collage Editor

| Feature | Included |
| :--- | :---: |
| Dynamic template picker (1–8 people, multiple layout styles) | ✅ |
| Tap-to-swap photos between slots | ✅ |
| Export to Gallery (1080×1920 portrait) | ✅ |
| Native Android share sheet | ✅ |

Faces are rendered from the **generously-cropped source frame**, not the tight embedding crop — matching the requirement to avoid low-resolution, tightly-cropped tiles.

---

## 🚀 Setup & Build

**Requirements:** Android Studio (recent stable), minSdk 26, Kotlin.

1. Clone the repository and open it in Android Studio.
2. The MobileFaceNet model is bundled at `app/src/main/assets/mobilefacenet.tflite` — no separate download step needed.
3. Let Gradle sync, then Build → Build APK(s), or Run on a connected device/emulator (API 26+).
4. On first launch, grant media/video access when prompted.
5. Select a video from the upload screen (dashed drop zone) to start processing.

No backend, no network calls — everything (detection, embedding, clustering, collage rendering) runs on-device.

---

## 📊 Embedding Model & Similarity Threshold

- **Embedding model:** MobileFaceNet, run on-device via TensorFlow Lite / LiteRT. Chosen for its small size and fast CPU inference.
- **Similarity metric:** Cosine distance between L2-normalized embeddings.
- **Similarity threshold:** Not a single fixed value — determined per-video at runtime via adaptive gap detection on cluster centroid distances (see Experiment Log, Round 7), clamped to `0.20–0.38` based on values observed across the 3 provided sample videos.
