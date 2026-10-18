# MyTrails

A GPS and mapping app for my own Android phone: live position on a map, track
recording, offline maps, and GPX import and export. Single user, sideloaded, no
Play Store release planned.

Native Android: Kotlin, Jetpack Compose and MapLibre Native. Project goals and
MVP scope are in `docs/design/00_project_brief.md`, the stack choice is in
`docs/decisions/0002-native-kotlin-stack.md`, and the working process is in
`docs/design/01_development_plan.md`.

## Build and run

Needs JDK 21 and the Android SDK. See the Commands section of `CLAUDE.md` for
the exact setup, the check commands, and how to use the emulator.

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Repo layout

- `app/`: the Android app.
- `CLAUDE.md`: working rules and durable context, loaded automatically by
  Claude Code.
- `docs/design/`: project brief, development plan and per-feature design notes.
- `docs/decisions/`: Architecture Decision Records (ADRs).
- `docs/handoffs/`: session handoffs for continuity between work sessions.
- `.github/`: CI, issue forms, PR template and Dependabot config.
- `scripts/bootstrap-repo-settings.sh`: one-time repo settings setup (branch
  protection, auto-merge, labels).

## License

GNU General Public License v3.0 (see `LICENSE`).
