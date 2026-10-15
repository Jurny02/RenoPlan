# 03 – Calculations

All calculations are pure functions in the domain layer. Any change of input recalculates every
derived value; nothing derived is stored.

Notation: `Lᵢ` – length of wall *i*, `H` – room height, `n` – number of walls.

## Outline (90° corners)

The user walks around the room: for each wall they give its length and the direction of the turn
at its end. No angles are needed.

1. Start at point `(0, 0)` heading along +X.
2. For each wall, move `Lᵢ` in the current heading to get the next vertex.
3. Turn: `RIGHT` = rotate heading 90° clockwise, `LEFT` = 90° counter-clockwise.

### Validation

The outline is closed when both hold (tolerance: 1 mm):

- the last vertex equals the start point `(0, 0)`;
- the net rotation is a full turn: `|#RIGHT − #LEFT| = 4`.

Also: `n ≥ 4` and every `Lᵢ > 0`.

### Auto-closing the last wall

The editor may compute the last wall's length: after walls `1..n−1`, the remaining vector to the
start point must be parallel to the last heading. Its length becomes `Lₙ`. If it is not parallel
(or not positive), the outline cannot be closed with one wall and the user is shown an error.

### Example: kitchen with a chimney in a corner

```
3.20 R  2.50 R  2.80 R  0.40 L  0.40 R  2.10 R
```

A 3.20 × 2.50 m room with a 0.40 × 0.40 m chimney box in one corner:
floor area = 8.00 − 0.16 = **7.84 m²**, perimeter = **11.40 m** (unchanged by the box, as expected
for a rectilinear notch).

## Floor and ceiling

Floor area from the vertices `(xᵢ, yᵢ)` using the shoelace formula:

```
A_floor = ½ · | Σ (xᵢ · yᵢ₊₁ − xᵢ₊₁ · yᵢ) |
```

`A_ceiling = A_floor` (flat ceiling assumption).

## Perimeter and volume

```
P = Σ Lᵢ
V = A_floor · H
```

## Openings

```
A_opening = width · height        (Dimensions)
A_opening = given value           (Area)
```

Validation: `width ≤ L` of its wall, `height ≤ H`, total opening area on a wall `≤` gross wall area.

## Walls

```
A_wall_gross(i) = Lᵢ · H
A_wall_net(i)   = A_wall_gross(i) − Σ A_opening on wall i
A_walls_net     = Σ A_wall_net(i)
```

Window reveals and niche depths are ignored.

## Mouldings

```
L_skirting = P − Σ width of openings with startsAtFloor = true
L_crown    = P
```

## Rounding

Calculations use exact millimetre integers and `Double` for areas. Rounding happens only for display:
areas to 0.01 m², lengths to 0.01 m, money to 0.01 PLN.

## Tests

Every formula above has unit tests in `commonTest`, including at least:
rectangle, L-shape, chimney notch, outline that does not close, auto-closed last wall,
opening given by area, skirting with a door and a passage.
