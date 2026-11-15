# Numeric keypad prop — handoff notes for another agent

**一句话**：一个 1 方块见方的数字键盘模型：外壳 + 显示屏 + 10 个可按下方键 + 2 个可按下圆钮。
屏幕数字由模组画（或叠一层附带的七段数码管 OBJ）。每个键都是独立 mesh，按下 = 沿 +Z 平移。
**One-liner**: 1-block keypad. Shell + display + 10 pressable keys + 2 pressable round buttons.
Each key is its own mesh; pressing = translate +Z. The digits are the mod's business (or use the
optional 7-segment OBJ shipped alongside).

---

## 1. Files

| file | what |
| --- | --- |
| `keypad.obj` | the model: shell, display, 10 keys, 2 round buttons — 13 `o` groups (§3) |
| `keypad_digits.obj` | **optional** 7-segment digits, 28 groups `D1_A … D4_G`, same texture/material |
| `keypad.mtl` | one material `prop`, `Kd 1 1 1`, `map_Kd keypad.png`, `illum 2` |
| `keypad.png` | 512×512 RGB, opaque (§6) |
| `README.txt` | Chinese end-user notes |
| `previews/keypad_front.png`, `..._iso.png`, `..._demo.png` | reference renders |

`keypad_digits.obj` is positioned in the same world space as `keypad.obj` — import both and they line
up exactly on the screen.

---

## 2. Coordinates & units

- Right-handed, **+Y up**, **front (keys) face toward −Z**.
- **OBJ coordinates are in blocks** (bbox `x 0..1`, `y 0..1`, `z −0.1875..0`).
- All numbers below are **design units `u` = 1/16 block** = **Blockbench unit**:

```
obj_coordinate (blocks) = u / 16
blockbench_value        = u
```

- Model: **16 × 16 × 3 u** = 1 block wide, 1 block tall, 0.19 block deep.
- Front view = camera on −Z looking +Z. **Seen that way +X is on the viewer's LEFT.**
- Import at **×16** in Blockbench to get 16 × 16 × 3.

---

## 3. Groups (`o` names in keypad.obj)

| group | role | animation |
| --- | --- | --- |
| `Body` | chassis ring + dark faceplate + display housing + screen plate + both button rings | static |
| `Key1 … Key9`, `Key0` | the ten square keys (labels printed in the texture) | **+Z translate when pressed** |
| `BtnTop` | upper round button, dark cap | **+Z translate when pressed** |
| `BtnBot` | lower round button, red cap | **+Z translate when pressed** |

Total: 13 groups, ~2 100 triangles, one material, one texture.

---

## 4. Pressing

| what | raised front z | pressed front z | **delta to apply (+Z)** |
| --- | --- | --- | --- |
| any `Key*` | −2.95 | −2.45 | **+0.50 u** |
| `BtnTop` / `BtnBot` | −2.98 | −2.62 | **+0.36 u** |

```java
// press amount 0..1 -> extra Z offset in design units, then /16 for OBJ space
double dz = 0.50 * press;     // keys
double dzBtn = 0.36 * press;  // round buttons
```

- Press = move **+Z only** (into the panel). Never X/Y.
- The pressed key sinks to 0.05 u in front of the faceplate; a dark recess frame is part of the
  pressed *look*, so do **not** flatten it — the visual "sink" comes from the key's side walls
  disappearing plus the dark well around it.
- The round buttons sink into their rings (the rings are real annuli, the caps stay visible).
- Typical timing: down in ~60 ms, up in ~90 ms; release returns to 0.

---

## 5. The screen — digits are drawn by the mod

The screen is a flat dark plate. Nothing in the model changes it; draw the digits over it.

| | value (u) |
| --- | --- |
| screen rect | `x 3.20 … 12.80`, `y 12.40 … 14.30` |
| screen front plane | `z = −2.975` (draw at `z = −2.99 … −2.98`, still behind the bezel at −3.00) |
| digit cells | 4 cells, width **2.40 u**, centres at `x = 11.60, 9.20, 6.80, 4.40` (x descending = screen left→right), `y = 13.35` |
| glyph size (recommended) | `0.86 w × 1.52 h`, segment thickness `0.20` |

### 5.1 Or use `keypad_digits.obj`

Shipped with the model so it can show numbers without a custom renderer:

- 4 digits × 7 segments = **28 groups**, named `D<n>_<SEG>` with `<SEG> ∈ {A,B,C,D,E,F,G}`
  (A top, B upper-right, C lower-right, D bottom, E lower-left, F upper-left, G middle).
- Show a digit by keeping only the segments listed below visible (scale the rest to 0 / hide them):

```
0: ABCDEF   1: BC      2: ABGED   3: ABGCD   4: FGBC
5: AFGCD    6: AFGECD  7: ABC     8: ABCDEFG 9: ABCDFG
```

- Digit 1 (`D1_*`) is the **leftmost** on screen (largest x). Its cell centre is `x = 11.60`.
- **The shipped file lights all 28 segments** (it will read `8888`). Hide / zero-scale the
  segments you do not need — the table above is the mapping. To bake one fixed number instead, run
  `build_digits("1234")` in the generator.
- Segment boxes sit at `z = −3.000 … −2.988` (horizontal segments 0.004 further back to avoid
  coplanar fighting).

---

## 6. Texture layout (512×512, opaque, one material)

| region | rect (px) | content |
| --- | --- | --- |
| `k0 … k6` | `(4|72|140|208|276|344|412, 4, 64, 64)` | key faces 0-6 (tan + dark numeral) |
| `k7 … k9` | `(4|72|140, 72, 64, 64)` | key faces 7-9 |
| `screen` | `(4, 140, 448, 96)` | dark glass with an orange rim |
| swatches | small 10×10 blocks after those | all flat colours (chassis, face, key sides, button caps, LED orange) |

- Key faces are whole-face UVs; everything else samples a 4×4 patch inside its swatch.
- Do not remap UVs; recolour the swatches or a tile instead.

---

## 7. Importing

**Blockbench**: `File → Import → OBJ` → `keypad.obj` (12 keys/buttons become separate meshes).
Optionally import `keypad_digits.obj` on top. **Import at ×16**.

**Animation**: give each `Key*` / `Btn*` its own bone (or animate the meshes) and keyframe Z.
`Body` never moves.

---

## 8. Rebuilding

```bash
python build_keypad.py        # numpy + Pillow; rebuilds obj/png/previews/zip
```

Knobs: `KEY_COLS`, `KEY_ROWS`, `KEY_UP/KEY_DN`, `BTN_*`, `DIG_*`, `SEG`, the PAL palette,
`build(pressed=("Key3","BtnBot"))` for previews, `build_digits("1234")` to bake any number.

---

## 9. Constraints & gotchas

1. **Press direction is +Z** (into the panel) — moving −Z pops the key out of the shell.
2. Do not move keys in X/Y: the dark gap grid only looks right at the modelled positions.
3. `Key0` sits in the bottom row, middle column — it has no neighbours left/right (that is intended,
   it matches the reference keypad).
4. The digits are **not** in `keypad.obj`; if you see an empty screen that is correct.
5. Keep the screen plate where it is (`z = −2.975`); drawing digits in front of the bezel (`z < −3.0`)
   will look like they float above the panel.
6. One material only: no emissive/transparent variants. The LED orange is just a bright colour.

---

## 10. QA checklist

- Front view: keys read 1…9 then 0 under the middle column; labels upright (not mirrored).
- Press `Key5` → it sinks 0.50 u, dark recess visible, label still readable.
- Press `BtnBot` → red cap sinks into its cream ring; press `BtnTop` → dark cap sinks.
- Digits at `z ≈ −2.99` are fully inside the screen rect and in front of the glass.
- Bounding box stays `1.0 × 1.0 × 0.1875` blocks (= 16 × 16 × 3 u).
