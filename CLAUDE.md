# CLAUDE.md

Working rules and durable context for this project. This file is loaded into
context automatically, so keep it to direction and rules, not a task list.

## Project name

MyTrails

## Project purpose

A GPS and mapping app for my own Android phone. Single user, sideloaded, no Play Store release planned. See `docs/design/00_project_brief.md`.

## Project status

Early build. Core features and MVP scope are in the brief and the stack is chosen (ADR 0002). A minimal "Hello map" app shows a map and the live position; the next steps are offline maps and track recording.

## Scope

Personal Android GPS and mapping app. Deliberately NOT a Play Store product, multi-user, or account-based. Do not expand scope without a design note in `docs/design/` or an ADR in `docs/decisions/`.

## Stack

Native Android: Kotlin, Jetpack Compose, MapLibre Native with offline OSM vector tiles, built with Gradle. See `docs/decisions/0002-native-kotlin-stack.md`. Offline tile format and the driving routing engine are still to be decided in their own ADRs.

## Commands

Run from the repo root. Needs JDK 21 (`JAVA_HOME`, for example
`~/.local/jdks/jdk-21*`) and the Android SDK (`ANDROID_HOME=~/Android/Sdk`, or
`sdk.dir` in the git-ignored `local.properties`).

- Build debug APK: `./gradlew assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- Checks CI runs (do all before pushing): `./gradlew ktlintCheck lint test`.
  Fix formatting with `./gradlew ktlintFormat`. Lint warnings fail the build;
  version-currency checks are off because Dependabot handles them.
- Install on a device or emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Emulator: AVD `phone` (Pixel 7, API 36, GPS on). Start with a window:
  `~/Android/Sdk/emulator/emulator -avd phone`. Fake a GPS fix with
  `adb -s emulator-5554 emu geo fix <lon> <lat>`.

## Repo structure

Not yet needed; explore the tree.

## Working rules

- Branch from `main`; one short-lived branch per change. Open a pull request
  into `main`; CI must pass before merge.
- Conventional Commits (for example `feat: ...`, `fix: ...`, `chore: ...`,
  `docs: ...`).
- No em dashes in code, comments, docs, commit messages, or user-facing text.
- Significant technical or design decisions get an ADR in `docs/decisions/`.
- Design notes live in `docs/design/`; session handoffs in `docs/handoffs/`.
- Keep changes small and focused; update docs when behaviour or design changes.
