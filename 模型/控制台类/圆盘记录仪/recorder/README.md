# Chart recorder prop — handoff notes for another agent

**一句话**：一个 OBJ 底座（框 + 纸 + 刻度板）+ 一支笔。模型里**没有曲线**——模组自己画。
模型只吃一个输入：读数 → 笔的 Y。
**One-liner**: base (frame + paper + scale) + pen. **The trace is not in the model — the mod draws it.**
The model takes exactly one input: the reading → `Pen.Y`.

---

## 1. Files

| file | what |
| --- | --- |
| `recorder.obj` | the whole model, 2 `o` groups (see §3). Coordinates in blocks. |
| `recorder.mtl` | one material `prop`, `Kd 1 1 1`, `map_Kd recorder.png`, `illum 2`. |
| `recorder.png` | 512×512 RGB, fully opaque, single texture (see §6). |
| `README.txt` | Chinese end-user notes (for the human; an agent can ignore it). |
| `README.md` | this file. |
| `previews/recorder_front.png`, `..._iso.png` | reference renders (static parts). |
| `previews/recorder_pos.png` | the pen at readings 10 / 35 / 60 / 85 — preview only. |

Exactly **one** model, and it contains **no curve**. Earlier revisions had an `Ink` ribbon; it was
removed on purpose because the mod renders the trace itself.

---

## 2. Coordinate system — read this before touching numbers

- Right-handed, **+Y up**, **front face points toward −Z**.
- **The OBJ stores coordinates in BLOCKS** (bbox `x 0..1.0`, `y 0..0.8125`, `z −0.1875..0`).
- Every number in this document is in **design units `u` = 1/16 block** = **Blockbench unit**:

```
obj_coordinate (blocks) = u / 16
blockbench_value        = u
```

- Bounding box: `16 × 13 × 3 u` = 1 block wide, ~0.81 high, ~0.19 deep.
- **Import scale**: import the OBJ at **×16** in Blockbench to get 16 × 13 × 3 units.
- Front view = camera on −Z looking toward +Z; **seen that way +X is on the viewer's LEFT**.

---

## 3. Mesh groups (`o` names)

| group | faces | tris | role | animated? |
| --- | --- | --- | --- | --- |
| `Body` | 84 | 168 | frame slab + inner bevel + black lip + paper + scale plate, merged | no |
| `Pen` | 23 | 44 | pen nib + rod at the paper's right edge (= the pointer) | **translate Y only** |

**Total: 212 triangles, 142 vertices, 1 material, 1 texture, 2 draw calls.**
Nothing scrolls, nothing else moves. The trace is drawn by the mod (§5).

---

## 4. The pen — the only moving part

- Translate **Y only**; never X/Z.
- Mapping (v = reading, 0 = bottom, 100 = top):

```
y = 2.836 + 0.07276 * v      # design units (u); v = 0..100
y_obj        = y / 16        # what you write into the OBJ space
y_blockbench = y             # same number, Blockbench units
```

- v = 0 → `y = 2.836`, v = 100 → `y = 10.112`.
- Default pose in the file: **v = 50 → y = 6.474** (OBJ y-coord 0.4046).
- The pen's nib apex is at `x = 4.0418 u`, `z = −2.928..−2.828` — that is the point the mod's own
  curve should meet (§5).

---

## 5. Integration contract — **one input, and the curve is the mod's job**

> **Hard rule**: the mod feeds **one scalar — the reading** — and it drives **only** `Pen`'s Y.
> The curve is **not part of the model** and must be rendered by the mod. Nothing else in the OBJ
> takes input.

```text
mod state ──(value 0..100, clamped, smoothed)──►  Pen.Y
mod renderer ──(its own trace quads)──────────►  drawn in front of the paper (see 5.1)
Body ─────────────────────────────────────────►  static
```

| contract item | value |
| --- | --- |
| input | `value` — one scalar, the reading |
| accepted range | `0.0 … 100.0`; clamp outside, hold last good value on NaN/missing |
| drives | `Pen` group, **Y only** |
| must NOT drive | `Body` (static); the model contains no trace to drive |
| update rate | once per tick, or on change only — never per frame |
| smoothing | lerp / exponential filter ~200–400 ms; no snapping |
| per-frame cost | 1 transform; the mesh and the texture never change |

```java
// value: 0..100  ->  penY in design units u (= Blockbench units), /16 for OBJ space
double v01   = Math.max(0.0, Math.min(1.0, value / 100.0));
double penYu = 2.836 + 7.276 * v01;      // 2.836 (value 0) .. 10.112 (value 100)
```

### 5.1 Where the mod should draw its own curve

Everything below in design units `u`:

| | |
| --- | --- |
| paper plane (front face) | `z = −2.800` |
| paper rect | `x 2.22 … 13.78`, `y 2.22 … 10.78` |
| **drawable curve area** | `x 4.0118 … 13.78` (the scale plate covers the rest), `y 2.22 … 10.78` |
| suggested curve plane | `z = −2.815` (0.015 in front of the paper, where the old ribbon was) |
| right end should meet | `x = 4.0418` (pen nib apex) |
| value → y | `y = 2.836 + 0.07276 * v` (same mapping as the pen) |

- Clip the trace to the paper rect (or to the frame opening `x 1.80 … 14.20`, `y 1.80 … 11.20`),
  otherwise it will draw over the frame.
- Because the paper is static, a scrolling history has to move in the mod's own geometry/UVs — the
  model does not scroll.
- Keep the curve slightly inside `z = −2.79 … −2.83`: in front of the paper (`−2.80`), behind the
  scale plate (`−2.858`) and the pen (`−2.928 … −2.828`).

### 5.2 Integrator checklist

- [ ] `Pen` moves on Y only; X/Z untouched.
- [ ] `Body` never moves, never scales, never swaps texture.
- [ ] The trace is drawn by the mod, in the rect from §5.1, clipped to the paper.
- [ ] `value` clamped to 0…100, NaN-safe, smoothed.
- [ ] No per-frame texture swap / mesh rebuild on the model side.

---

## 6. Texture layout (512×512, opaque, one material)

| region | rect (px) | content |
| --- | --- | --- |
| `paper` | `(4, 4, 400, 300)` | cream + grid every 10 + green band 35–65 + red limit lines 87 / 14. **No trace.** |
| `scale` | `(408, 4, 64, 300)` | cream + red alarm zones (88–100, 0–12) + ticks (minor 10 / major 20) + numbers 0/20/40/60/80/100. |
| swatches | small 10×10 blocks | flat colours for every other face (frame, lip, pen…), each with 3 px bleed. |

- Whole-face UVs: `Paper` front = the entire `paper` region, `Scale` front = the entire `scale` region.
- If you recolour, edit the swatch blocks or the two big regions — **do not remap UVs**.
- No alpha anywhere; if your pipeline enables blending you will get artefacts.

---

## 7. Importing

**Blockbench**: `File → Import → OBJ`, pick `recorder.obj` (keep the `.mtl` and `.png` beside it).
Each `o` group becomes its own mesh, so 2 meshes appear. **Import at ×16** → 16 × 13 × 3 units.

**Bones (recommended)**: bone `pen` ← mesh `Pen`, key position Y. `Body` can be left as one static
mesh or parented to the root.

**Minecraft / YSM / item**: a flat 1×0.81×0.19-block plate facing −Z; rotate as needed when placing.

---

## 8. Rebuilding

```bash
python build_recorder.py          # numpy + Pillow, regenerates obj/mtl/png/previews/zip
```

> The generator is **not** inside this zip — it lives in the project folder `panel_recorder/`.

Knobs: `PAPER_W/H`, `SCALE_W/H`, `TW/TH` (texture resolution), the PAL colours, `v_at_x` is gone
(there is no trace in the model any more), and `build(v_ptr=…)` renders the pen at any reading.

---

## 9. Constraints & gotchas

1. **There is no trace in the model.** If you are looking for an `Ink` group, it was removed on
   purpose — draw the curve yourself (§5.1).
2. The scale plate (`x 2.22 … 4.0118`, `z −2.858 … −2.836`) is part of `Body` and sits 0.022 in
   front of the paper; that is the raised strip you see at the right. Don't flatten it.
3. The pen's own travel is `y = 2.836 … 10.112`; never offset it outside that.
4. Do not add a second material; the MTL is a single opaque `prop` material.
5. Body geometry is pre-merged for performance — do not split it into per-part meshes unless you
   need to hide a specific part.
6. If the mod's curve needs a different z to avoid z-fighting with its own geometry, use
   `−2.79 … −2.83` and check against the paper (`−2.80`) and the scale plate (`−2.858`).

---

## 10. QA checklist

- Front view: paper is clean (no baked trace), scale plate on the right, pen standing at the reading.
- Move `Pen` 0 → 100: it travels the full printed scale, never leaves the plate, never sinks into it.
- `Body` is identical in every frame (no drift, no scaling).
- Bounding box stays `1.0 × 0.8125 × 0.1875` blocks in the OBJ (= 16 × 13 × 3 design units).
- The mod's curve, drawn at `z = −2.815`, sits in front of the paper and behind the pen, and its right
  end meets the pen nib at `x = 4.0418`.
