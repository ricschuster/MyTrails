# 01 Development Plan

How work gets planned, built, checked and tracked in MyTrails. Short on
purpose: this is a one-person project, so the process only needs to keep
changes small, reviewable and safe to install on my own phone.

## Workflow

- Branch from `main`, one short-lived branch per change.
- Open a pull request into `main`. CI must pass before merge.
- Squash merge, queued with `gh pr merge --auto --squash` right after the PR is
  opened.
- Conventional Commits for commit messages and PR titles.
- The owner is new to Android, so Claude writes the code and the owner reviews.
  PR descriptions should explain what changed and why in plain terms.

## Tracking work

- GitHub issues are the task list. `CLAUDE.md` stays free of task lists.
- One issue per MVP feature (live position polish, track recording, offline
  maps, GPX import and export), each linked to its design note.
- Smaller items (bugs, chores, cleanups) get an issue only if they will not be
  done in the current session.
- Labels: `enhancement`, `bug`, `docs`, `chore`, plus `mvp` for the four MVP
  features and `post-mvp` for candidates in the project brief.
- PRs link their issue with `Closes #N`.

## Design before code

A feature gets a design note in `docs/design/` before coding starts. The note
covers the user-visible behaviour, data and storage, permissions, and what is
deliberately left out. A choice that is hard to reverse (tile format, routing
engine, storage engine) gets an ADR in `docs/decisions/` instead.

Needed before specific features:

| Feature | Needed first |
| --- | --- |
| Track recording | Design note: foreground service type, notification, battery, storage |
| Offline maps | ADR: tile format and source |
| GPX import and export | Design note: supported GPX elements, file access |
| Driving navigation | ADR: routing engine (post-MVP) |

## Definition of done

A change is done when:

1. It builds and all CI checks pass.
2. New logic has JVM unit tests where it can be tested without a device.
3. Behaviour that touches GPS, permissions, the map or services has been tried
   on the emulator, and on the phone once the feature is usable outdoors.
4. Docs are updated if behaviour or design changed (design note, ADR,
   `CLAUDE.md` Commands or status, `CHANGELOG.md`).
5. The owner has reviewed the PR.

## Test strategy

- JVM unit tests (`./gradlew test`) for pure logic: GPX parsing and writing,
  track statistics, distance maths, anything that does not need Android. Keep
  such logic out of Activities and composables so it stays testable.
- Static checks in CI: Android lint plus ktlint or detekt (to be chosen in the
  lint change).
- Instrumented tests on an emulator are not part of CI for now. They are slow
  and flaky to run on hosted runners. Revisit if a feature, such as the
  recording service, proves hard to trust without them.
- Manual checks use the emulator (`adb emu geo fix`) and, for real GPS quality
  and battery behaviour, the phone outdoors.

## CI

Two required checks on `main`:

- `repo-hygiene`: workflow lint, markdown lint, no em dashes, docs structure.
- `build`: compiles the app (`assembleDebug`) on every PR, with Gradle caching and no APK upload.

Lint and unit tests join the `build` job as they are added, so the required
check name does not change. Dependabot keeps GitHub Actions and Gradle
dependencies current.

## Sequence

1. This plan and the CI build job.
2. Lint, format and unit test setup, then fix the known deprecation warning.
3. Housekeeping: `CONTRIBUTING.md` Setup, Dependabot Gradle entry, README and
   ADR 0001 naming.
4. Issues for the four MVP features.
5. Features, one at a time: track recording, GPX export, offline maps, GPX
   import. Each starts with its design note.
6. Install on the phone and confirm GPS works outdoors (can happen any time
   after step 1).

## Open items

- Release signing: the release build uses the debug key. Back up
  `~/.android/debug.keystore` or move to a dedicated key before it matters.
- License is the template's GPL-3.0 and still needs the owner's confirmation.
