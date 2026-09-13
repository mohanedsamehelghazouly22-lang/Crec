# Clash Overlay Assistant — v0.3.0

Production-oriented Android foundation for an **observe-only** Clash Royale visual assistant.

## Safety boundary
The app only captures and analyzes pixels through Android MediaProjection and renders a floating overlay. It does **not** tap, swipe, place cards, inject code, modify memory, automate gameplay, or bypass game security.

## v0.3.0
- normalized capture ROIs; no device-specific hard-coded pixel coordinates
- real TensorFlow Lite adapter with float/quantized input handling
- temporal verification with high/medium confidence policies
- unique 8-card opponent deck tracking
- 4-slot opponent hand tracking with manual-correction API
- cycle engine with next-card prediction from known deck/hand state
- elixir estimator with standard 1x/2x/3x timeline and min/max bounds
- compact draggable overlay with elixir, confidence, cycle and next-card state
- foreground MediaProjection service with explicit stop action
- local card catalog schema supports localized names, aliases, heroes, champions, evolutions, variants and recognition templates
- unit coverage for deck/cycle/elixir rules

## Model contract
`app/src/main/assets/models/card_classifier.tflite` is intentionally not fabricated. The app refuses to produce card identities when a valid model is absent. Replace it together with `models/labels.txt` using the same output class order.

The recognition model is replaceable and runs on-device. A training/dataset pipeline should be supplied with properly licensed screenshots and labels; no scraped copyrighted image pack is bundled by default.

## Current game-rule references
The elixir timeline is based on Supercell's documented standard rules: 1x in the first two minutes, 2x from minute three and 3x from minute five. Modes with different rules should use `setPhaseMultiplier()`.

## Build
Open the project in Android Studio with JDK 17+ and let Gradle sync. The environment used to assemble this archive does not ship the Gradle CLI, so APK compilation must be run through Android Studio/Gradle on a normal Android build environment.

## GitHub
The intended target repository is `mohanedsamehelghazouly22-lang/Crec`. The connected GitHub integration currently rejects write operations with HTTP 403, so no remote commit is claimed until the integration is granted repository write access.

## v0.4.0 hardening
- TFLite model availability is explicit (`READY`, `MODEL_UNAVAILABLE`, `LABELS_UNAVAILABLE`, `INVALID_MODEL`).
- UINT8/INT8/FP32 model I/O paths are handled without guessing when a model is absent.
- Manual correction state is session-scoped with undo and reset.
- New-match reset clears all tracked state and corrections.
