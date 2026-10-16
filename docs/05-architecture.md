# 05 – Architecture

## Platform

Kotlin Multiplatform with a shared Compose Multiplatform UI for Android and iOS
([ADR 0001](adr/0001-kotlin-multiplatform-with-compose.md)). Build: Gradle.

## Modules

| Module | Contents |
|---|---|
| `shared` | All shared code: domain, data, presentation and Compose UI (`commonMain`), platform code in `androidMain` / `iosMain`, tests in `commonTest` |
| `androidApp` | Android entry point |
| `iosApp` | iOS entry point (Xcode project, minimal Swift) |

Inside `shared/src/commonMain/kotlin`, layers are separate packages (`domain`, `data`, `presentation`, `ui`).
If the domain grows, it can be extracted into its own module without changes to its code, because it
has no dependencies on the other layers.

## Layers

```
┌───────────────────────────────┐
│ UI (Compose Multiplatform)    │  screens, components
├───────────────────────────────┤
│ Presentation                  │  ViewModels, UI state
├───────────────────────────────┤
│ Domain (pure Kotlin)          │  model, geometry, costs, use cases
├───────────────────────────────┤
│ Data                          │  repositories, local database
└───────────────────────────────┘
```

- **Domain** has no dependencies on frameworks, the database or the UI. It holds the model from
  [02](02-domain-model.md) and the calculations from [03](03-calculations.md) and
  [04](04-operations-and-costs.md). It is the most heavily unit-tested part.
- **Data** implements repository interfaces defined in the domain layer
  ([ADR 0003](adr/0003-uuid-ids-and-repository-interfaces.md)).
- **Presentation** exposes immutable UI state; derived values (areas, costs) are computed from
  domain functions, never stored.

## Persistence

Local database on the device. Candidates: SQLDelight or Room KMP (decision pending, to be recorded
as an ADR).

## Screens (MVP)

See [06 – Screens](06-screens.md).

## Future: sharing via Firebase

Sharing a level with other people is planned later. The MVP prepares for it by:

- generating `Uuid` IDs on the device;
- hiding storage behind repository interfaces, so a synced implementation can be added without
  touching the domain or UI.

## Quality

- Formatting and lint: Spotless with ktlint (and Compose rules), detekt, SwiftLint. See the root README.
- CI: GitHub Actions (`.github/workflows/ci.yml`) runs lint, the Android build and tests, SwiftLint and
  the iOS simulator tests on every pull request.
