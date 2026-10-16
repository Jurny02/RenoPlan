# 0005 – Store money as integer grosze

- **Status:** Proposed
- **Date:** 2026-10-17

## Context

Operations have prices per m² or per metre and lump sums, and summaries add up many costs.
Floating-point złoty values accumulate rounding errors (`0.1 + 0.2 ≠ 0.3`), so totals could differ
by a grosz from the sum of the rows shown. The app works in PLN only.

## Decision

Store and pass all amounts as integer grosze (a `Money` value class over `Long`, 1 zł = 100 gr).
A cost is `price per unit × quantity`, where the quantity is a `Double` area or length; the result
is rounded to the nearest grosz once, per operation. Totals add rounded operation costs.

## Consequences

- Sums and comparisons of amounts are exact, and a total always equals the sum of its rows.
- Prices typed in złoty need converting to grosze and back for display.
- A currency other than PLN would need a currency field; out of scope for the MVP.
