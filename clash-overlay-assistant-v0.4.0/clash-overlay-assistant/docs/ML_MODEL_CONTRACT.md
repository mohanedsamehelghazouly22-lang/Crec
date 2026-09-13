# Card recognition model contract

The Android runtime expects `app/src/main/assets/models/card_classifier.tflite`.

## Input
- Tensor shape: `[1, height, width, 3]`
- RGB order
- FLOAT32: values normalized to `[0,1]`
- UINT8/INT8: tensor quantization parameters are honored

## Output
- One classification tensor whose final dimension equals `models/labels.txt` line count.
- The runtime selects the highest output score and clamps the reported confidence to `[0,1]`.
- Temporal verification is applied after classification.

## Dataset layout
Use only screenshots/recordings you are permitted to use. Recommended structure:

```
dataset/
  train/<card-id>/*.jpg
  val/<card-id>/*.jpg
  test/<card-id>/*.jpg
metadata.jsonl
```

Vary resolution, aspect ratio, device DPI, compression, brightness, motion blur, overlays, card level visuals, evolution state, hero/champion forms and partial occlusion.

Do not train on labels inferred by the same model being evaluated.
