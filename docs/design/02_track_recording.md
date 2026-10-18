# 02 Track recording

Design note for issue #14. Implementation: #15 (foreground service) and #16
(saved tracks list and view). GPX export is a separate note (#17).

## Behaviour

- A record button on the map screen starts a track. While recording, the
  screen shows elapsed time and distance, with pause and stop controls.
- Recording keeps running with the screen off or the app in the background.
- Pause stops adding points but keeps the track open. Resume continues the
  same track; the gap is drawn as a break, not a straight line.
- Stop closes the track and saves it. A track with fewer than 2 points is
  discarded after a confirmation.
- A saved tracks screen lists tracks (name, date, distance, duration) and
  opens one on the map. Rename and delete are included; edit is not.
- If the app is killed or the phone reboots mid-recording, the points already
  written are kept and the track is marked as interrupted. It is not resumed
  automatically.

## Foreground service

- A started foreground service owns recording, so it survives the activity
  being closed. The activity binds to it only to show live state.
- Service type `location` (`android:foregroundServiceType="location"`), with
  the `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION` permissions.
- The service uses the existing `GpsLocationEngine` approach: the platform GPS
  provider directly, no Google Play services.
- Start only from a user action while the app is visible. Android 14 and later
  reject starting a location foreground service from the background.
- Stops itself on stop; never restarts itself (no `START_STICKY`).

## Notification

- Ongoing, low importance channel "Recording" (no sound).
- Shows status (recording or paused), elapsed time and distance.
- Actions: Pause or Resume, and Stop. Tapping the body opens the app.
- Updated at most about once per second, and only when the text changes.

## Permissions

- Foreground location (`ACCESS_FINE_LOCATION`) is already requested for the
  live position and is enough while the foreground service is running, because
  a location foreground service started from a visible app counts as
  while-in-use access.
- `ACCESS_BACKGROUND_LOCATION` is therefore not requested. This avoids the
  extra settings-screen flow. Recheck on the phone (#12); if recording stops
  with the screen off, revisit this.
- `POST_NOTIFICATIONS` (Android 13 and later) is requested when the user first
  taps record. If denied, recording still works because the foreground
  service runs regardless; the notification is just hidden, and the service
  shows only in the system's Task Manager.
- Many phones also kill background apps aggressively. The app shows a one-time
  hint to exempt MyTrails from battery optimisation, linking to the system
  settings page. It does not request the exemption permission.

## Battery and sampling

- GPS provider updates at 1 s interval, 0 m displacement, while recording.
  A point is stored when it is at least 5 m from the last stored point, or 10 s
  have passed, whichever comes first. Tunable constants, not user settings.
- Points with horizontal accuracy worse than 30 m are dropped.
- No wake lock: the foreground service with an active GPS request is enough
  for location delivery; add a partial wake lock only if testing shows gaps.
- Idle (not recording): no background work at all.

## Data and storage

Each point: latitude, longitude, time, altitude (nullable), horizontal
accuracy, speed (nullable). Each track: id, name, start time, end time, state
(recording, paused, completed, interrupted), and segments (a pause starts a new
segment).

Storage options:

| Option | For | Against |
| --- | --- | --- |
| Room (SQLite) | Incremental inserts are crash safe; easy list and query; cheap to add stats later | One more dependency and schema migrations |
| One GPX file per track | No conversion for export; data never locked in | Appending to XML safely is awkward; list screen must parse files |
| Append-only CSV or binary plus an index | Simple and crash safe | Custom format to maintain |

Recommendation: Room. Every point is inserted as it arrives, so a crash loses
at most the last point. GPX is produced on export (#17/#18) from the database.
Storage engine is hard to reverse, so on approval of this note it gets
`docs/decisions/0003-room-for-track-storage.md` before #15 starts.

## Testing

- JVM unit tests: point filtering (accuracy, minimum distance and time),
  distance calculation, segment handling on pause and resume.
- Keep that logic in plain Kotlin classes, separate from the service.
- Emulator: record a track with `adb emu geo fix` sequences, screen off,
  pause, resume, stop, kill the app mid-recording.
- Phone: a real outdoor walk with the screen off (part of #12 follow-up).

## Left out of the MVP

- Auto-pause, auto-start, and live stats beyond time and distance.
- Elevation profile, speed charts, and any editing of recorded tracks.
- Cloud sync and sharing (GPX export is covered by #17).
- Resuming an interrupted track.
- Waypoints during a recording (post-MVP candidate).

## Decisions for the owner

1. Room for storage (recommended) versus GPX files.
2. Sampling defaults above (5 m or 10 s, 30 m accuracy cut-off).
3. No background location permission unless phone testing shows it is needed.
