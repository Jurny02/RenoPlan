# 0002 – Store lengths as integer millimetres

- **Status:** Accepted
- **Date:** 2026-10-15

## Context

Floating-point lengths in metres accumulate rounding errors (e.g. `0.1 + 0.2 ≠ 0.3`), which breaks
checks such as "does the outline close?". Tape measures used in renovation give millimetres at best.

## Decision

Store and pass all lengths as integer millimetres (a `Millimetres` value class over `Long`).
Areas and volumes are derived as `Double` in m² / m³. Values are rounded only for display.

## Consequences

- Outline closure and comparisons are exact.
- Input and display need conversion between metres/centimetres and millimetres.
