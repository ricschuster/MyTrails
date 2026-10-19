# 03 GPX export

Design note for issue #17. Implementation: #18 (GPX writer and export action).
GPX import is a separate later note.

## Behaviour

- Each saved track has an Export action on the Tracks screen and in the track
  view.
- Export writes one `.gpx` file to a location the user picks, then shows a
  short confirmation (or an error message).
- Completed and interrupted tracks can be exported. A track that is still
  recording cannot (the action is disabled).
- Exporting never changes or deletes the track.

## GPX content

GPX 1.1 (`http://www.topografix.com/GPX/1/1`), `creator="MyTrails"`.

- `<metadata>`: `<name>` (track name) and `<time>` (track start).
- One `<trk>` with the track name in `<name>`.
- One `<trkseg>` per recorded segment. A pause starts a new segment, so the
  gap is never drawn as a straight line in other apps.
- Each point is a `<trkpt lat lon>` with `<ele>` (only when the fix had an
  altitude) and `<time>` in UTC, ISO 8601 with a `Z` suffix and millisecond
  precision.
- Coordinates are written with 7 decimals (about 1 cm), plain decimal notation,
  always with a `.` separator regardless of the phone's locale.
- Text (the track name) is XML-escaped.

Left out of the file: horizontal accuracy, speed, and any vendor extensions
(for example Garmin `gpxtpx`). Other apps recompute speed from the points, and
extensions add size and compatibility questions for no current use. Add them in
a later note if a need appears.

## File naming

`<track name>.gpx`, with characters that are unsafe in file names (`/ \ : * ?
" < > |` and control characters) replaced by `-`, runs of whitespace collapsed,
and a fallback of `track.gpx` if nothing remains. The default track name is the
start date and time, so an unrenamed track exports as for example
`Oct 19, 2026 3-54 AM.gpx`. The system file picker handles a name collision.

## Saving: Storage Access Framework

Use the system "create document" picker (`ActivityResultContracts.CreateDocument`
with MIME type `application/gpx+xml`), pre-filled with the file name.

| Option | For | Against |
| --- | --- | --- |
| Storage Access Framework (create document) | No storage permission, no `FileProvider`; user picks the folder or Drive; produces a real file | Extra picker step; no direct "send to" |
| Share sheet (`ACTION_SEND` + `FileProvider`) | One tap to send to another app | Needs a cache file and `FileProvider`; the file is a temporary copy |

Recommendation: Storage Access Framework. It matches the goal that the data is
never locked in (a file I choose where it lives), and needs the least plumbing.
A share action can be added later if wanted.

## Implementation notes

- A plain Kotlin `GpxWriter` with no Android types: takes track name, start
  time and a list of segments (each a list of points) and writes to an
  `Appendable`. Time formatting uses `java.time.Instant`.
- The export action reads points from Room (`getPoints`), groups by segment,
  and writes to the `OutputStream` the picker returns, off the main thread.
- Memory: a long track is tens of thousands of points, which is small enough
  to load in one list; no paging needed for the MVP.

## Testing

- JVM unit tests for `GpxWriter`: header and namespace, one `trkseg` per
  segment, `ele` present only when known, UTC time format, locale-independent
  decimals, XML escaping of the name, an empty segment skipped, file name
  sanitising.
- Round trip check: the output parses as well-formed XML (JVM `javax.xml`).
- Emulator: export a track, open the file in the Files app and check it; open
  it in another GPX viewer if one is installed.

## Left out of the MVP

- GPX import (separate note and issue).
- Exporting several tracks at once, routes (`<rte>`) and waypoints (`<wpt>`).
- Accuracy, speed and vendor extensions in the file.
- A share-sheet action.

## Decisions for the owner

1. GPX 1.1, one `<trk>`, one `<trkseg>` per segment, no extensions.
2. Storage Access Framework for saving; share sheet later if wanted.
3. File name from the track name, sanitised.
