# Native Kotlin, Jetpack Compose and MapLibre

## Status

Accepted

## Context

MyTrails is a single-user, sideloaded Android app (see
`docs/design/00_project_brief.md`). Three requirements drive the stack:

- Reliable GPS track recording in the background with the screen off.
- Offline maps, using OpenStreetMap-derived vector tiles.
- Eventual driving navigation to trailheads, including logging roads, which
  implies an on-device routing engine.

The owner is new to Android development and expects Claude to write most of
the code, with the owner reviewing and running it. Options considered: native
Kotlin with Jetpack Compose, Flutter, and a web app or React Native.

## Decision

Build MyTrails as a native Android app:

- Language: Kotlin.
- UI: Jetpack Compose.
- Map display: MapLibre Native for Android, with OpenStreetMap-derived vector
  tiles stored on the device for offline use.
- Builds: Gradle, runnable from the command line so CI can build the APK and
  the owner can sideload it without needing Android Studio at first.

The offline tile format and source, and the routing engine for driving
navigation, are separate decisions and will get their own ADRs.

## Consequences

- Background location, battery control and offline maps are first-class, and
  routing engines can be added without a cross-language bridge.
- One language to read and maintain.
- Android only, which matches the brief. Running on iOS is not possible
  without a rewrite.
- The Gradle build, Android permissions and background-work rules are the
  steepest part for a newcomer. The first milestone is a minimal app that
  shows a MapLibre map and the current position and installs on the phone, to
  prove the toolchain and sideloading before building features.
- Working mode: Claude writes the code in small, single-feature pull
  requests with plain-language explanations, and the owner reviews and tests
  on the phone.
- Flutter was rejected because its cross-platform benefit is unused and
  background GPS and routing would still need native Kotlin behind a bridge.
  A web app or React Native was rejected because background tracking and
  offline maps are unreliable there.
