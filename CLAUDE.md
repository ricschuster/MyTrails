# CLAUDE.md

Working rules and durable context for this project. This file is loaded into
context automatically, so keep it to direction and rules, not a task list.

## Project name

MyTrails

## Project purpose

A GPS and mapping app for my own Android phone. Single user, sideloaded, no Play Store release planned. See `docs/design/00_project_brief.md`.

## Project status

Pre-design. Stack and feature set are undecided; the next step is to fill in the brief and record the stack choice as an ADR.

## Scope

Personal Android GPS and mapping app. Deliberately NOT a Play Store product, multi-user, or account-based. Do not expand scope without a design note in `docs/design/` or an ADR in `docs/decisions/`.

## Stack

Undecided. To be recorded in an ADR in `docs/decisions/`.

## Commands

To be filled in once the stack is chosen.

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
