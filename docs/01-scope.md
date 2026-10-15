# 01 – Scope

## Goal

Let a user describe the rooms of one apartment level, assign finishing work to each surface and see
how many square metres of each finish are needed and what it costs. When room dimensions change as
work progresses, everything recalculates automatically.

The MVP follows the 80/20 rule: cover the most common cases simply, leave rare ones for later.

## Main use case

1. The user creates a **level** (e.g. "Ground floor").
2. They add a **room** "Bathroom" and enter its dimensions:
   - as a rectangle (length × width × height), or
   - as a right-angled outline (walls with lengths and left/right turns), e.g. a kitchen with a chimney.
3. They add **openings** (window, door, passage, niche) to numbered walls, giving width × height or
   just an area, and whether the opening starts at the floor.
4. They add **operations** to surfaces (wall 1–n, floor, ceiling): paint (colour), skim coat,
   primer, tiles, wall panels, custom…
5. They enter prices: material per m² and labour per m², as a lump sum, or "I do it myself".
6. The **summary** shows totals per room or for the whole level, e.g. "skim coat: 10 walls, 100 m²",
   "tiles: walls 20 m², floor 10 m²", plus costs.
7. As work progresses, each operation is marked *to do / in progress / done*.

## In scope (MVP)

- Multiple levels, each with multiple rooms.
- Room shapes: rectangle, or any outline with **90° corners only**.
- Flat ceiling at a single height per room.
- Openings: window, door, passage (door without a leaf), niche. Position on the wall is irrelevant.
- Window reveals are ignored.
- Skirting length (perimeter minus openings that start at the floor) and crown moulding length
  (full perimeter).
- Operations per surface with material and labour pricing, reusable operation templates.
- Summary per room and per level.
- Price-per-m² calculator (package price, amount, coverage, coats).
- Local storage only, no accounts.

## Out of scope (for now)

- Non-right angles, sloped ceilings, free-standing pillars.
- Openings shared between rooms (a door must be added in both rooms).
- Material quantities in litres/kg or package counts.
- Drawing the floor plan by hand; the outline preview is read-only.
- Accounts, cloud sync, sharing.

## Roadmap

1. **Calculation engine** – domain model and geometry in pure Kotlin with unit tests.
2. **App shell** – levels, rooms, outline and opening editor, local database.
3. **Operations and costs** – operations, templates, pricing, summary.
4. **Progress and calculator** – work status, done vs remaining cost, price-per-m² calculator.

Later: free-standing pillars, linking a door to the neighbouring room, sharing a level with other
people via Firebase.
