# Room for track storage

## Status

Accepted

## Context

Track recording (`docs/design/02_track_recording.md`, issue #14) writes a GPS
point about once a second to every few seconds, possibly for hours, and must
survive the app being killed or the phone rebooting. Tracks are listed, viewed
and exported as GPX. Options considered: Room (SQLite), one GPX file per track,
and an append-only custom file plus an index.

## Decision

Store tracks and points in a Room (SQLite) database. Each point is inserted as
it arrives. GPX is generated on export from the database, not used as the
storage format.

## Consequences

- A crash or kill loses at most the last point.
- Listing, renaming, deleting and later statistics are plain queries.
- Adds the Room dependency and schema migrations, which must be kept in step
  with app changes.
- GPX remains the way data leaves the app, so data is not locked in.
- Moving to another store later means a one-off data migration.
