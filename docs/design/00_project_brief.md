# 00 Project Brief

## What this is

MyTrails is a GPS and mapping app for Android.

It is a personal tool for a single user, not a Play Store product. The goal
is to have a mapping and GPS app on my own phone that does what I want it to.

## Who it is for

- Me (the repo owner), on my own Android phone. Installed by sideloading.

## The core idea (one screen)

An offline-capable map that shows where I am, records where I have been, and
reads and writes GPX so my data is never locked in. Used for hiking, cycling
and running; the same core serves all three.

## MVP scope

1. Live position on the map: position marker, follow mode, compass heading.
2. Track recording: background GPS recording with start, pause and stop, and
   saved tracks I can view again.
3. Offline maps: download map regions ahead of time and use them with no
   signal.
4. GPX import and export: load routes and tracks from GPX files, save
   recorded tracks as GPX.

## Post-MVP candidates (noted, not committed)

- Waypoints: drop, name and navigate to saved points.
- Route navigation: follow a loaded route with off-route alerts.
- Trip stats and elevation: distance, time, speed, elevation profile.
- Route planning: draw or generate routes in the app.
- Driving navigation: get to trailheads, including on logging roads and other
  unpaved forest roads. This is an intended eventual feature, so the stack and
  map data choices should not rule it out.

## Explicit non-goals (for now)

- Play Store release, multi-user support, accounts, or monetization.
- Driving navigation is not in the MVP (see post-MVP candidates).

## Design principles

1. Single-user and sideloaded: no store compliance or multi-device support
   needed unless it is cheap.

## Open questions

- Tech stack (native Kotlin, Flutter, other).
- Map source and licensing (for example OpenStreetMap tiles, MapLibre).
- Offline support and battery use during background location tracking.
