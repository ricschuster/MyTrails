# 07 Roadmap

Design note for work after the MVP. The four MVP features in
`docs/design/00_project_brief.md` are built (milestones M1 to M5). This note says
what comes next, in what order, and why. It expands the post-MVP candidates in
the brief, so it also serves as the scope record the project rules ask for.

Issues are the task list (see `docs/design/01_development_plan.md`). Each milestone
below is a GitHub milestone, and each feature is one issue. This note does not
list issue numbers, so it does not go stale when issues change.

## Where ideas come from

The reference apps are Trailforks, AllTrails, Gaia GPS, Komoot and Strava. The
filter for this project is the brief: one user, sideloaded, offline first.

What is borrowed:

- Knowing the trail under the finger: name, length, surface, difficulty.
- Following a route with off-route alerts, and finding the way back.
- Rich recorded data: elevation, speed, splits, history.
- Personal progress: a ridden-trails view, a heatmap, comparing a ride with my
  own earlier rides.
- Offline routing on trails, and route building.
- Safety basics that need no network: coordinates, sunset, low-battery recording.

What is deliberately left out, because it needs other people, a server or
proprietary data:

- Community trail reports, ratings, photos, leaderboards, following people.
- Accounts, sync between devices, and any hosted service.
- Trailforks or Strava data as a source. Their trail data and ratings are
  proprietary. Map features use OpenStreetMap only.

## Data constraints

- Trail attributes come from OSM tags. In the Roberts Creek test area only about
  4% of trail ways carry a difficulty tag (`docs/design/05_trail_difficulty.md`).
  Features that depend on tags (tap to identify, difficulty colours) will look
  thin where the map is thin.
- The answer to thin data is my own data: ratings, notes and ridden history that
  I add. That is why the personal features in M8 are planned as a core track
  and not an extra.
- GPS altitude is noisy. Elevation figures should come from the terrain files
  where they exist, with GPS altitude as a fallback. This needs checking in the
  M7 design note.
- The terrain licence texts (Open Government Licence - Canada, Copernicus) are
  still unread. Anything that ships or stores elevation derived from them waits
  for that.

## Assumptions to confirm

These shape the order. Each has a default, and a different answer changes the
plan.

| Question | Default used here |
| --- | --- |
| Main activities | Mountain biking and hiking equally, running third |
| Offline only? | Yes. Anything that needs a network is isolated in M11 |
| Phone sensors | Heart rate and cadence over Bluetooth are later, not early |

## Milestones

Milestones M1 to M5 are the finished MVP. The rest follow in order, but a feature
can move if a milestone's dependencies are met.

### M6 Daily use

The app should be something I can rely on for a normal ride without a PC in the
loop.

- Download offline map regions inside the app. Today a region is built on a PC and
  copied with adb, which is the biggest barrier to daily use.
- Waypoints: drop, name, and navigate to a saved point such as the trailhead or
  the car.
- Live stats on the recording panel: speed, moving time, elevation gain, grade.
- Tap a trail on the map to see its name, length, surface, difficulty and whether
  it is one-way.
- Open a `.gpx` file from other apps, and a launcher icon.
- Check that `INTERNET` can be dropped now that maps are local.
- A "where am I" screen with coordinates, plus sunrise and sunset computed
  offline.
- Check the battery behaviour on the Pixel and verify recording over a long trail
  outing.

Needs first: a design note for region download (source, storage, updates), and one
for waypoints.

### M7 Stats, history and layers

Make recorded data worth looking at afterwards.

- Track detail: elevation profile, gain and loss, speed profile, moving time, and
  splits.
- Colour a track by speed or gradient.
- Activity type on each track (ride, hike, run) with a filter on the list.
- Edit tracks: trim, split, merge.
- Photos and notes attached to a point on a track.
- Totals by month and by activity type.
- Map layers: contours and slope shading, then a satellite option if it can stay
  offline.

Needs first: a design note for elevation (source, smoothing, licence) and one for
track editing.

### M8 Know my trails

The personal version of what Trailforks gets from its community, built from my
own rides.

- A personal heatmap of everywhere I have been.
- Ridden trails: highlight OSM trails I have covered and show what is left in a
  region.
- My own difficulty ratings and condition notes on trails, shown on the map and
  in the tap-to-identify sheet.
- Compare a trail section with my own earlier runs of it.
- Import my ride history from a Strava or Trailforks export.

Needs first: a design note for matching a recorded track to OSM trails. Matching
quality decides whether ridden trails and self-comparison feel right.

### M9 Follow and plan

Go where I have not been, and find the way back.

- Follow a loaded route with off-route alerts and distance remaining.
- Backtrack: route me back along my own recorded track.
- ADR: offline routing engine. BRouter is the likely candidate for bike and hike
  profiles. The driving case in the brief should stay possible.
- A route builder that snaps to trails, with an elevation preview.
- A loop and out-and-back generator.
- Audio and haptic cues so I can follow a route with the screen off.

Needs first: the routing ADR, then a design note for route following.

### M10 UI and pocket use

The first screens were built for function. This milestone is a designed pass over
the whole app, done after M6 and M7 so the screens stop changing under it.

- A UI design note: navigation structure, the map screen layout, the recording
  panel, the track list and detail screens.
- Rework the navigation to match the note.
- Dark theme and a dark map style.
- Outdoor readability and large touch targets, including for gloves and font
  scaling.
- A low-battery recording mode.
- A widget and lock-screen or notification controls.

### M11 Later

Candidates that need a network, extra hardware, or too much effort for now. They
stay listed so the stack choices do not rule them out.

- Heart rate and cadence over Bluetooth.
- Weather and trail conditions fetched when there is signal.
- Overdue alerts and live location sharing.
- Driving navigation to trailheads, including logging roads.

## Sequence and dependencies

1. M6 first. It is what makes the app usable without a PC.
2. M7 and M8 build on the same track data. M8's matching work depends on the
   elevation and track-editing decisions in M7.
3. M9 is independent of M7 and M8 but needs the routing ADR first. It can start
   its ADR early.
4. M10 starts after M6 and M7, when the screens have settled.
5. M11 is not scheduled.

## Process for each feature

Follow `docs/design/01_development_plan.md`: a design note before code where the
milestone says so, an ADR for choices that are hard to reverse, and one issue per
feature linked to its note.
