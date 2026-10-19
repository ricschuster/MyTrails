# 06 GPX import

Design note for issue #22. Implementation: #23 (GPX parser, file picker, display).
Export is in `docs/design/03_gpx_export.md`.

## Behaviour

- The Tracks screen has an Import action. It opens the system file picker; the
  owner chooses a `.gpx` file.
- Each imported track is saved as a normal track in the list, with the same
  actions as a recorded one (open on the map, rename, export, delete). After a
  successful import the first imported track opens on the map.
- A short message reports the result ("Imported 2 tracks") or the reason it
  failed. A failed import saves nothing.
- Importing the same file twice makes duplicates. Detecting that is left out.

## Supported GPX content

| Element | Handling |
| --- | --- |
| `<trk>` | One saved track per `<trk>` |
| `<trkseg>` | One segment each (same as a pause in a recording), so gaps are not joined by a line |
| `<trkpt lat lon>` with optional `<ele>`, `<time>` | Point, elevation and time kept when present |
| `<rte>` and `<rtept>` | Imported as a track with one segment, since a planned route is drawn the same way |
| `<wpt>` | Ignored. Waypoints are not an MVP feature |
| `<extensions>`, `<metadata>` other than the track name | Ignored |

Both GPX 1.0 and 1.1 are accepted: elements are matched by local name and
namespace is not checked, because real files vary.

Points are skipped when `lat` or `lon` is missing, not a number, or out of range
(latitude beyond 90, longitude beyond 180). A segment keeps its remaining
points. A track with fewer than 2 valid points in total is not imported.

## Track fields

- Name: the `<name>` of the `<trk>` or `<rte>`; otherwise the file's display
  name without extension; otherwise "Imported track". A file with several
  unnamed tracks gets " 1", " 2" appended.
- Start time: time of the first point; otherwise the import time. End time: time
  of the last point, or empty if no point has a time.
- Distance: summed haversine distance within segments (the existing function).
- Duration: last point time minus first point time, or 0 when times are missing.
  Pauses between segments are not subtracted because they cannot be told apart
  from slow movement in an imported file.
- State: completed.

## Storage

No schema change. Imported points reuse the `point` table. Two columns are
not nullable, so unknown values use sentinels:

- Missing `<time>`: `timeMillis` is 0. Export omits `<time>` for a point whose
  time is 0, so a round trip does not invent 1970 timestamps.
- `accuracyMeters` is 0 (unknown) and speed is null.

All tracks and points of one file are written in one database transaction, so
a failure leaves nothing behind.

## Parsing

- SAX (`javax.xml.parsers`, available on Android and in JVM tests), streaming,
  so a large file is not held as text or a DOM.
- XML hardening: doctype declarations and external entities are turned off, as
  a file from another app is untrusted input.
- Limit: 500,000 points per file. Beyond that the import fails with a "file too
  large" message rather than running out of memory. A long all-day track is
  about 30,000 points, so this is generous.
- Reading happens on a background thread. The parser is plain Kotlin with no
  Android types so it is tested on the JVM.

## File access

`ActivityResultContracts.OpenDocument`, no storage permission. It accepts any
type because phones often label `.gpx` as `application/octet-stream`. Choosing
the wrong kind of file produces the "not a GPX file" message.

## Errors shown

| Cause | Message |
| --- | --- |
| Not well-formed XML | The file is not valid GPX |
| Root element is not `gpx` | The file is not a GPX file |
| No track or route with 2 valid points | No tracks found in the file |
| More than 500,000 points | The file is too large |
| Cannot read the file | Could not read the file |

## Testing

- JVM unit tests for the parser: one track with segments, route import, waypoints
  ignored, GPX 1.0 and 1.1, missing times and elevations, bad coordinates
  skipped, malformed XML, wrong root, doctype refused, point limit, and a large
  file (200,000 points) parsed without trouble.
- Round trip: a track written by `GpxWriter` parses back to the same points.
- Emulator: import a file with the picker, open it, export it again.

## Left out of the MVP

- Opening `.gpx` files from other apps (an intent filter) and the share sheet.
- Waypoints, route-specific display, and extensions such as heart rate.
- Duplicate detection and merging.
- Importing several files at once.

## Decisions for the owner

1. Imported data becomes ordinary tracks, with no separate "imported" kind.
2. Routes are imported as tracks; waypoints are ignored.
3. Time 0 means unknown, and export leaves it out.
