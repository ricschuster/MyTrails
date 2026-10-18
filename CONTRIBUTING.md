# Contributing

MyTrails is a solo project for now, so this file is mostly a note to myself and
to Claude. Outside contributions are not expected; issues are welcome as
feedback.

## Workflow

1. Branch from `main` (one short-lived branch per change).
2. Keep changes small and focused.
3. Run the checks locally before pushing:
   `./gradlew ktlintCheck lint test assembleDebug`
   (`./gradlew ktlintFormat` fixes formatting).
4. Open a pull request into `main`. CI (`repo-hygiene` and `build`) must pass
   before merge. Squash auto-merge is queued with `gh pr merge --auto --squash`.

Setup (JDK 21, Android SDK, emulator) is in the Commands section of
`CLAUDE.md`. The process in full is in `docs/design/01_development_plan.md`.

## Conventions

- Conventional Commits (for example `feat: ...`, `fix: ...`, `chore: ...`,
  `docs: ...`).
- No em dashes in code, comments, docs, commit messages, or user-facing text.
- See `CLAUDE.md` for the full working rules and `docs/` for design notes and
  decision records.
