# 04 – Operations and costs

An **operation** is a piece of finishing work done on whole surfaces of a room
(e.g. "skim coat on walls 1, 2, 4"). Partial surfaces are not modelled.

## Operation types

```kotlin
sealed interface OperationType {
    data class Painting(val colour: String) : OperationType
    data object SkimCoat : OperationType
    data object Priming : OperationType
    data object Tiling : OperationType
    data object WallPanels : OperationType
    data object Skirting : OperationType
    data object CrownMoulding : OperationType
    data class Custom(val name: String) : OperationType
}
```

"Paint the ceiling white" is `Painting("white")` on `Surface.Ceiling`, not a separate type.

The unit follows from the surfaces: m² for walls, floor and ceiling; running metres for
skirting and crown moulding.

## Operation

| Field | Type | Notes |
|---|---|---|
| id | Uuid | |
| type | OperationType | |
| surfaces | Set<Surface> | e.g. `{Wall(1), Wall(2), Ceiling}` |
| materialPrice | Money per unit | per m² or per metre |
| labour | Labour | see below |
| status | `TODO` \| `IN_PROGRESS` \| `DONE` | |

```kotlin
sealed interface Labour {
    data class PerUnit(val price: Money) : Labour  // per m² or per metre
    data class LumpSum(val amount: Money) : Labour // for the whole operation
    data object SelfDone : Labour                  // "I do it myself", 0
}
```

## Cost

```
quantity      = Σ net area (or length) of the operation's surfaces
materialCost  = quantity · materialPrice
labourCost    = quantity · price | amount | 0
totalCost     = materialCost + labourCost
```

Because `quantity` is derived from geometry, changing a room's dimensions updates every cost.

## Templates

An `OperationTemplate` stores a type with default prices, e.g.
"Painting – white matt, 6 PLN/m², labour 15 PLN/m²". Adding an operation from a template copies the
values into the operation, so editing a template later does not silently change existing plans.

## Summary

Grouped by operation type (with its parameter, e.g. colour), either **per room** or **for the whole
level**:

| Operation | Surfaces | Quantity | Cost | Done | Remaining |
|---|---|---|---|---|---|
| Skim coat | 10 walls | 100.00 m² | … | … | … |
| Tiling | 4 walls, 1 floor | 30.00 m² | … | … | … |
| Painting – white | 6 walls, 3 ceilings | 80.00 m² | … | … | … |
| Skirting | 4 rooms | 38.40 m | … | … | … |

*Done* is the cost of operations with status `DONE`; *Remaining* is everything else.

## Price-per-m² calculator

A standalone helper. The user notes the result and types it into an operation manually.

Inputs: package price, package amount (L or kg), coverage (m² per L or kg), number of coats.

```
area per package = amount · coverage / coats
price per m²     = package price / area per package
```

Example: 200 PLN, 5 L, coverage 7 m²/L, 2 coats → 5 · 7 / 2 = 17.5 m² per package →
**11.43 PLN/m²** (5.71 PLN/m² for one coat).
