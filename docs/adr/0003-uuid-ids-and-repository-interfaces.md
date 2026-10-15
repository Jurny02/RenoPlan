# 0003 – UUID identifiers and repository interfaces

- **Status:** Accepted
- **Date:** 2026-10-15

## Context

The MVP stores data locally only, but sharing a level with other people (likely via Firebase) is
planned. Retrofitting sync onto auto-increment IDs and a UI that talks to the database directly
would require a rewrite.

## Decision

- Every entity gets a `Uuid` generated on the device.
- The domain layer defines repository interfaces; the data layer implements them.
  UI and domain never depend on a concrete database.

## Consequences

- A synced repository can be added later without changing domain or UI code.
- Slightly more boilerplate in the MVP.
