# 0005 – Store money as integer grosze

- **Status:** Accepted
- **Date:** 2026-10-17

## Context

Operations have prices per m² or per metre and lump sums, and summaries add up many costs.
Floating-point złoty values accumulate rounding errors (`0.1 + 0.2 ≠ 0.3`), so totals could differ
by a grosz from the sum of the rows shown. The MVP works in PLN; euro and US dollar will be supported
later, each as a separate currency without conversion between them.

## Decision

Store and pass all amounts as integer grosze (a `Money` value class over `Long`, 1 zł = 100 gr).
A cost is `price per unit × quantity`, where the quantity is a `Double` area or length; the result
is rounded to the nearest grosz once, per operation. Totals add rounded operation costs.

## Consequences

- Sums and comparisons of amounts are exact, and a total always equals the sum of its rows.
- Amounts are always shown in złoty (e.g. "11,43 zł"), never in grosze; prices are typed in złoty and
  converted to grosze.
- EUR and USD also have 100 minor units (cents), so the same integer representation works for them.
  Supporting them means adding a currency to the amounts (likely set per project or level) and to the
  formatting; amounts in different currencies are never added together or converted.
