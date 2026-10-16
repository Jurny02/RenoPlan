# 06 – Screens

MVP screens, how the user moves between them and what each one offers.
UI texts are in Polish (see [glossary](glossary.md)); this document uses English names.

## Navigation

A bottom bar with three destinations:

| Tab | Start screen |
|---|---|
| **Project** | Levels list |
| **Templates** | Operation templates |
| **Calculator** | Price-per-m² calculator |

```mermaid
flowchart TD
    LS[Launch screen] -->|first run| OB[Onboarding]
    LS -->|next runs| L
    OB --> L
    L[1. Levels list] --> LV[2. Level]
    LV -->|Rooms tab| RE[3. Room editor]
    LV -->|Rooms tab| RD[4. Room details]
    LV -->|Summary tab| S[7. Summary]
    LV -->|Costs tab| CO[10. Costs]
    CO --> OP
    RD --> RE
    RD --> O[5. Opening sheet]
    RD --> OP[6. Operation editor]
    OP -.->|pick template| T[8. Templates]
    T --> TE[Template editor]
    C[9. Calculator]
```

Every derived number (areas, lengths, costs) is recalculated live on every screen; nothing needs a
"recalculate" button.

## Launch screen

- Native splash, not a Compose screen: Android SplashScreen API, iOS launch screen in `Info.plist`.
- App icon on the brand background colour; shown only while the app and database start.
- Routes to **Onboarding** on the first run, otherwise to **Levels list**.

## Onboarding

Shown once on the first run; skippable at any time. Can be reopened from the help icon on the
Levels list.

1. **Plan your renovation** – what the app does: rooms, areas, work and costs in one place.
2. **Measure rooms** – rectangle or outline, walls are numbered; openings are subtracted.
3. **Add work and prices** – operations on walls, floor and ceiling; material and labour prices.
4. **Track costs and progress** – summary per room or level; mark work as done.

Last page: **Create your first level** (asks for a name, opens the Level) or **Skip**.
A "seen" flag is stored locally so onboarding does not show again.

## 1. Levels list

Home screen of the Project tab.

- Card per level: name, number of rooms, total floor area, total cost, progress (% of cost done).
- **Add level** (FAB): asks for a name.
- Card menu: rename, delete (with confirmation).
- Empty state: short explanation and an "Add your first level" button.
- Top bar: help icon → Onboarding.
- Tap a card → **Level**.

## 2. Level

Three tabs: **Rooms**, **Summary** (quantities, screen 7) and **Costs** (money, screen 10).

Rooms tab:

- Card per room: name, floor area, net wall area, cost, progress.
- **Add room** (FAB) → **Room editor** (new room).
- Card menu: rename, duplicate (copies geometry, openings and operations), delete.
- Tap a card → **Room details**.

## 3. Room editor

Creating a room or changing its geometry.

- Name, height.
- Shape mode toggle: **Rectangle** / **Outline**.
  - Rectangle: length, width.
  - Outline: list of rows `#, length, turn (L/R)`; add / remove row; "close automatically" option
    computes the last wall ([03 – Calculations](03-calculations.md#auto-closing-the-last-wall)).
    Turning it on adds the computed wall after the typed rows; if the typed rows already close the
    outline, their last row is replaced by the computed wall. Turning it off keeps the computed wall
    as an editable row.
- Read-only **preview** of the outline with wall numbers, so the user can match numbers to real walls.
  The start point is marked with a dot; an outline that does not close shows the gap.
- Live results: floor area, perimeter, volume.
- Validation message when the outline does not close (distance to the start point, or wrong turn
  count); shown once every wall has a valid length. **Save** is disabled until it closes.
- Switching Rectangle → Outline converts the 4 walls into rows; Outline → Rectangle is only allowed
  for a 4-wall outline.
- Lengths are typed in metres with a comma or a dot (e.g. `2,65`) and stored as millimetres. A field
  shows an error only once something invalid has been typed into it; Save stays disabled until the
  name and every length are valid.

Changing geometry keeps openings and operations. If a wall number disappears (fewer walls), the user
is warned which openings and operations will lose that wall.

## 4. Room details

The main working screen of a room.

- Header: floor, ceiling, net walls, perimeter, skirting, volume. Button **Edit shape** → Room editor.
- **Walls** section: row per wall `Wall 1 – 3.20 m – 7.20 m² net – 2 openings`; tap a wall to see its
  openings (tap an opening to edit it); **Add opening** → Opening sheet. The floating **Add opening**
  button preselects the expanded wall, or wall 1.
- **Operations** section: row per operation with type, surfaces, quantity, cost and status chip;
  tap the status chip to cycle *to do → in progress → done*; **Add operation** → Operation editor.
- Room totals: cost, done, remaining.
- Room totals sit above the operations list, with a progress bar (done ÷ total cost).

## 5. Opening sheet

Bottom sheet for adding or editing an opening.

- Type: window / door / passage / niche.
- Wall: number picker (preselected when opened from a wall).
- Size: toggle **Dimensions** (width, height) / **Area** (m²).
- **Starts at floor** switch, default from type (changing the type resets it); when on, Dimensions mode
  is required and Area is disabled.
- Live result: opening area, the wall's net area after the change.
- Validation from [03](03-calculations.md#openings): wider than the wall, taller than the room, or
  openings larger than the wall; **Save** is disabled while any of them applies. Changing the wall
  moves the opening.
- Delete (when editing).

## 6. Operation editor

- **Type**: list of types; Painting asks for a colour, Custom asks for a name.
  Or **From template** → picks a template and prefills everything below.
- **Surfaces**: multi-select chips `Wall 1 … Wall n`, `All walls`, `Floor`, `Ceiling`, `Skirting`,
  `Crown moulding`. Wall surfaces and linear surfaces cannot be mixed in one operation (different units).
- Live quantity (m² or m).
- **Material price** per unit.
- **Labour**: segmented control *per unit / lump sum / I do it myself*, with the price field
  where it applies.
- **Status**: to do / in progress / done.
- Live cost: material, labour, total.
- **Save as template** checkbox.
- Templates, when there are any, are offered as chips at the top; picking one fills the type and
  prices.
- Prices are typed in złoty; a blank price counts as 0 zł.
- Choosing Skirting or Crown moulding as the type preselects that surface when nothing is selected.
  Selecting a surface of the other unit (m² vs m) starts a new selection instead of mixing them.
- **Delete** (with confirmation) in the top bar when editing.

## 7. Summary

Quantities only: how much of what. Shown as the Summary tab of a Level (and as a filtered view for a
single room from Room details).

- Toggle **By room** / **Whole level**.
- Geometry block: total floor, ceiling and net wall area, skirting and crown moulding length.
- Table grouped by operation type (with colour for painting): surfaces, quantity, status counts.
  - By room: one group per room.
  - Whole level: one row per type, e.g. "Skim coat – 10 walls – 100.00 m²".

## 8. Templates

- List of templates: type, material price, labour (prices per unit of the operation).
- Add (FAB) / edit (tap a template) / delete (in the editor, with confirmation). The editor is the
  Operation editor without surfaces and status.
- Editing a template does not change operations created from it earlier
  ([04](04-operations-and-costs.md#templates)).

## 9. Calculator

- Inputs: package price, package amount, unit (L / kg), coverage (m² per unit), coats (default 2).
- Results: area per package, **price per m²**, and the 1-coat price for reference.
- Copy-to-clipboard button for the result; the user enters it into an operation manually.

## 10. Costs

Money: what it costs and how prices are set. Costs tab of a Level.

- Header: total cost, done, remaining, progress bar; split into material and labour.
- Toggle **By room** / **By operation type**.
- Row per operation group: quantity, material price, labour (per unit / lump sum / self), cost, status.
  Tap a row → **Operation editor**.
- **Edit price for all**: on a type group (e.g. "Painting – white"), set material price and labour once
  and apply them to every operation of that type on the level. Shows how many operations will change
  and the cost before → after.
- Toggle labour **I do it myself** for a whole group, to see quickly how much doing it yourself saves.
- Levels list cards show each level's total, so the cost of the whole apartment is visible on the
  home screen.

## Implementation order

Follows the [roadmap](01-scope.md#roadmap):

1. Launch screen, Levels list, Level (Rooms tab), Room editor – rectangle mode.
2. Room editor – outline mode with preview; Room details – walls and openings.
3. Operation editor, Room details – operations, Summary, Costs, Templates.
4. Statuses and progress, Calculator, Onboarding (last, when the screens it describes are stable).

## Out of scope for the MVP

Settings (currency, units are fixed to PLN and metres), export/share, dark/light theme
switch beyond the system setting.
