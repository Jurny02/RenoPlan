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

Packages in `shared/src/commonMain/kotlin/dev/jurczyk/renoplan/`:

| Package | Contents |
|---|---|
| `domain` | Model, calculations, repository interfaces. Pure Kotlin, no framework imports |
| `data` | Room database, repository implementations |
| `presentation` | ViewModels and UI state |
| `ui` | Compose screens and components; `ui/theme` (Material 3 theme), `ui/navigation` (routes, bottom bar) |
| `di` | Koin modules and `initKoin()` |

If the domain grows, it can be extracted into its own module without changes to its code, because it
has no dependencies on the other layers.

### Startup

- Android: `RenoPlanApplication` calls `initKoin { androidContext(...) }`, then `MainActivity` shows `App()`.
- iOS: `iOSApp.init()` calls `KoinKt.doInitKoin()`, then `MainViewController()` shows `App()`.
- `App()` applies `RenoPlanTheme` and hosts the navigation with the bottom bar.

### Resources

All UI texts live in `shared/src/commonMain/composeResources/values/strings.xml` (Polish is the
default locale) and are read with `stringResource(...)`. No hard-coded UI strings in Kotlin.
Icons are vector drawables in `composeResources/drawable/`.

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
- Form input that text fields edit (room, opening, operation, template editors and the calculator)
  is Compose `mutableStateOf` in the ViewModel, not a `StateFlow`: text fields need every change
  synchronously, and the extra hop of collecting a flow moved the cursor on iOS. Data from the
  repositories still arrives as flows.

## Persistence

Room KMP with the bundled SQLite driver; small settings in multiplatform-settings.
Dependency injection with Koin, navigation with Navigation Compose
([ADR 0004](adr/0004-core-libraries.md)).

- Repository interfaces live in `domain/repository` (`LevelRepository`, `RoomRepository`,
  `TemplateRepository`); the Room implementations (`LocalLevelRepository`, `LocalRoomRepository`,
  `LocalTemplateRepository`) in `data/repository`.
- Tables: `levels`, `rooms`, `walls`, `openings`, `operations`, `templates`. Child rows are deleted with
  their parent (`ON DELETE CASCADE`); an opening references its wall by `(roomId, wallNumber)`.
- Lengths are stored as `Long` millimetres, money as `Long` grosze, ids as `Uuid` strings, enums by
  name. An operation's type, surfaces (e.g. `WALL:1,CEILING`) and labour are stored as text
  (`data/mapper/OperationMappers.kt`). Levels, rooms, operations and templates keep their order in a
  `position` column.
- A room is saved as a whole: the room row is upserted, its walls, openings and operations are
  replaced.
- `RenoPlanDatabase` is created by `buildDatabase(builder)` (bundled SQLite driver, `Dispatchers.IO`);
  the platform builder comes from Koin's `platformModule` (Android: app context, iOS: Documents folder).
- Schemas are exported to `shared/schemas/` and committed. Every schema change bumps the version and
  comes with a migration (an `AutoMigration` where Room can derive it), because the app is already
  installed on test devices. Version 2 added `operations` and `templates`.
- Database tests run in `iosTest` on the simulator, because the Android database builder needs a `Context`
  and the JVM host tests have no Robolectric; entity mappers are tested in `commonTest`.

## Screens (MVP)

See [06 – Screens](06-screens.md).

## Future: accounts, sharing and crash reporting via Firebase

Sharing a level with other people is planned later. The MVP prepares for it by:

- generating `Uuid` IDs on the device;
- hiding storage behind repository interfaces, so a synced implementation can be added without
  touching the domain or UI.

Planned tools:

- Accounts and sharing: Firebase Authentication and Firestore (free Spark plan covers a first app).
- Crash reporting: Firebase Crashlytics (free), added together with Firebase. iOS stack traces from
  Kotlin need CrashKiOS to be readable.
- Kotlin access to Firebase from `commonMain`: GitLive Firebase SDK.

## Quality

- Formatting and lint: Spotless with ktlint (and Compose rules), detekt, SwiftLint. See the root README.
- CI: GitHub Actions (`.github/workflows/ci.yml`) runs lint, the Android build and tests, SwiftLint, the
  iOS simulator tests and an Xcode build of the iOS app on every pull request.
