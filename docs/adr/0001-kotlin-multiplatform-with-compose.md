# 0001 – Kotlin Multiplatform with shared Compose UI

- **Status:** Accepted
- **Date:** 2026-10-15

## Context

The app targets Android and iOS and is built by one developer. Most of its value is calculation
logic that must behave identically on both platforms.

## Decision

Use Kotlin Multiplatform with Compose Multiplatform for a shared UI, built with Gradle.

## Consequences

- Domain logic and UI are written once and tested once.
- The iOS UI is not native SwiftUI; platform-specific look and feel is limited.
- Swift code is kept to the minimal iOS entry point.
