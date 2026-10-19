# PMTiles built with Planetiler for offline maps

## Status

Accepted

## Context

Offline maps are an MVP feature (issue #19). A good trail map is the main thing
this app needs, so the tile source must carry trail detail, not just roads. The
stack (ADR 0002) uses MapLibre Native, and driving navigation is a later
feature that the choice must not rule out.

What was checked, on the Roberts Creek and Largo Road area in BC (about
10 by 13 km, zoom 0 to 15, OSM data of 2026-10-18):

- MapLibre Native Android supports PMTiles from 11.7.0, loaded from a local
  `pmtiles://file:///...` source. It does not support offline packs or caching
  for PMTiles, which a local file does not need. Native MBTiles support was not
  confirmed.
- The stock Protomaps basemap extract (`pmtiles extract` of a daily build) is
  1.7 MB and draws over 700 trail segments with 176 trail names. It carries no
  surface, difficulty or MTB tags, and no contours or hillshade. Protomaps
  daily builds are kept for about a week, so the app cannot rely on them as a
  download source.
- A custom Planetiler build from a Geofabrik BC extract, using a short YAML
  schema, took 23 seconds, gave a 0.9 MB file, and kept surface, smoothness,
  track grade, MTB and hiking difficulty, bicycle, foot and horse access,
  and width.
- Raw OSM tagging is sparse here. Of 755 trail ways: name 35%, surface 35%,
  bicycle access 25%, MTB difficulty (`mtb:scale:imba`) 4%, `mtb:scale` 1%,
  `sac_scale` under 1%. Difficulty colours in apps like Trailforks come from
  their own databases, which OSM does not contain.
- Terrain data (Mapterhorn, Terrarium-encoded DEM in PMTiles) is a candidate
  for hillshade and contours. Its data licence is not yet confirmed.

## Decision

- Container: PMTiles, one file per region, stored in app storage and loaded
  through MapLibre's `pmtiles://` source.
- Tiles: build our own with Planetiler from OSM extracts (Geofabrik), using a
  schema that keeps the trail tags above. The stock Protomaps basemap is not
  used for the trail map. The Planetiler YAML schema lives in the repo once
  the offline maps design note (#20) is written.
- Regions: for now the owner builds regions on a PC and copies them to the
  phone with `adb`. Downloading regions inside the app is a wanted later
  feature, not part of the first version.
- Difficulty: try importing ratings from another open source with a suitable
  licence. If that is not feasible, accept the gap and show difficulty only
  where OSM has it. Rating trails by hand in the app is a fallback to design
  later.
- Contours and hillshade: to be decided in the design note (#20) after
  checking the elevation data licence.
- Labels and symbols: fonts and sprites must be bundled for the map to work
  with no signal. Not yet verified.

## Consequences

- Maps work from one local file per region with no signal, and files are
  small (under 1 MB for a 10 by 13 km area at zoom 15).
- The tile build is a manual PC step until in-app download exists. It needs
  Java (Java 25 worked) and a large OSM extract (the BC extract is 1.2 GB).
- Trail detail is limited by OSM tagging. Difficulty is mostly missing and
  needs its own solution.
- Data is ODbL, so OpenStreetMap attribution is required in the app.
- The same OSM extracts can later feed a routing engine for driving
  navigation, so this choice does not rule it out.
- Keeping our own schema means maintaining it as OSM tagging changes.
