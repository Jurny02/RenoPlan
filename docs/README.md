# RenoPlan documentation

RenoPlan is a Kotlin Multiplatform (Android & iOS) app that helps an investor plan a home renovation:
model rooms, calculate wall, floor and ceiling areas, plan finishing work and track its cost.

It is a helper for the investor, not a contractor's estimating tool: results are good approximations,
not millimetre-exact bills of quantities.

## Contents

| Document | What it covers |
|---|---|
| [01 – Scope](01-scope.md) | MVP scope, use cases, what is out of scope, roadmap |
| [02 – Domain model](02-domain-model.md) | Entities and their relations |
| [03 – Calculations](03-calculations.md) | Geometry formulas: outline, areas, volume, mouldings |
| [04 – Operations and costs](04-operations-and-costs.md) | Work operations, pricing, summaries, price-per-m² calculator |
| [05 – Architecture](05-architecture.md) | Layers, modules, persistence, future sync |
| [06 – Screens](06-screens.md) | Screens, navigation and options on each screen |
| [Glossary](glossary.md) | Polish ↔ English terms used in code and UI |
| [ADR](adr/) | Architecture Decision Records |

## Docs as code

- Documentation lives next to the code and changes in the same pull request as the code it describes.
- A change to a formula in `03-calculations.md` must come with a matching unit test.
- Significant technical decisions are recorded as ADRs in [`adr/`](adr/) using the [template](adr/0000-template.md).
  ADRs are never edited after acceptance; a new ADR supersedes an old one.
