# 04 Offline maps

Design note for issue #20. Implementation: #21. Tile format and source are
decided in `docs/decisions/0004-offline-tiles-pmtiles-planetiler.md`
(PMTiles built with Planetiler from OSM extracts). This note covers everything
else the first version needs.

## Behaviour

- The map works with no signal inside any region file that is installed.
- Regions are installed by the owner: build on a PC, copy to the phone with
  `adb` (see Region build). No in-app download in the first version.
- The app lists installed regions (file name, size, and bounds from the
  PMTiles header) in a simple Regions screen. Delete is available there.
  Adding a region is not (yet).
- Outside every installed region the map shows an empty background with the
  live position and recorded tracks still drawn. No network fallback: the
  current `demotiles.maplibre.org` style goes away, so the app makes no map
  network requests at all.
- Attribution "© OpenStreetMap contributors" is always visible on the map
  (ODbL). Add the elevation attribution once terrain is shown (see Terrain).

## Region build (on the PC)

One script in the repo, `tools/tiles/build-region.sh`, takes a region name and
a bounding box and produces `<name>.pmtiles`.

1. Download the OSM extract once (Geofabrik, for example British Columbia,
   1.2 GB) and keep it.
2. Run Planetiler (a pinned version, 0.10.2 was used for the ADR experiment)
   `generate-custom --schema=tools/tiles/trails.yml --bounds=<bbox>
   --maxzoom=15 --output=<name>.pmtiles`. It needs Java (Java 25 worked) and
   took 23 s for 10 by 13 km.
3. Copy to the phone: `adb push <name>.pmtiles
   /sdcard/Android/data/ca.mytrails.app/files/regions/`.

Why that folder: it is the app's external files directory
(`getExternalFilesDir`). `adb push` can write to it without any storage
permission, the app can read it without any, and it is removed on uninstall.
Not verified on API 36 yet; check this first in #21. Fallback if it is blocked:
`adb push` to `/data/local/tmp`, then `adb shell run-as` copy into internal
storage `filesDir/regions/`.

### Schema (`tools/tiles/trails.yml`)

Kept in the repo, reviewed like code. Layers, as in the ADR experiment:

| Layer | OSM source | Kept tags |
| --- | --- | --- |
| `trails` | `highway` = path, track, footway, cycleway, bridleway | name, surface, smoothness, tracktype, `mtb:scale`, `mtb:scale:imba`, `sac_scale`, bicycle, foot, horse, width |
| `roads` | other `highway` classes | name, class, surface |
| `water`, `waterway` | water polygons, rivers, streams | name |
| `landuse` | parks, forest, residential, and similar | class |

The file itself is written in #21 together with the style, and checked by
building the Roberts Creek and Largo Road test region and viewing it. Zoom 15
is the maximum stored; MapLibre overzooms beyond that.

## How the app loads regions

- On start (and when the Regions screen opens) scan `regions/` for `*.pmtiles`.
- Each file becomes one vector source in the style, `pmtiles://file://<path>`
  (supported by MapLibre Native Android 11.7.0 and later; the app uses 13.7.1).
  Overlapping regions both draw, which is harmless.
- The style JSON is generated in code from a bundled template, because the
  source list depends on which files exist. It is passed with
  `Style.Builder().fromJson(...)`. The template and layer paint rules live in
  `app/src/main/assets/style/` and are plain JSON so they can be edited without
  Kotlin changes.
- A corrupt or non-PMTiles file is skipped and shown as unreadable on the
  Regions screen. It must never crash the map.
- Initial camera: the live position if there is one, otherwise the centre of
  the first installed region, otherwise the whole world.

## Fonts and sprites

Labels need glyph files (`.pbf`) and any icons need a sprite sheet. Both are
bundled in the APK so the map works with no signal.

- Glyphs: use one open-licensed font family (for example Noto Sans, OFL),
  pre-built as `{fontstack}/{range}.pbf`, put in `assets/style/glyphs/`. The
  Protomaps `basemaps-assets` repository publishes ready-made glyph and sprite
  files that can be downloaded as a ZIP. Reuse them if their licence permits
  (check the repo's LICENSE in #21, not yet checked) and restrict to the one
  family and the Latin ranges to keep the APK small.
- Sprites: start with none. Trail names and peaks are text only in the first
  version, so no sprite is needed. Add one if icons are wanted later.
- Style references: `"glyphs": "asset://style/glyphs/{fontstack}/{range}.pbf"`.
  MapLibre Native on Android documents `asset://` for styles, and the same
  scheme is used for glyphs and sprites in iOS examples. Not confirmed for
  glyphs on Android 13.7.1, so the first task in #21 is a spike with a bundled
  style and a text label. Fallback: copy the glyphs to `filesDir` at first run
  and use `file://`.

## Terrain: contours and hillshade

Licence check for the elevation data (Mapterhorn), from
`https://download.mapterhorn.com/attribution.json`:

- Mapterhorn merges many open datasets. For Canada the entries are
  Canada DTM 2018-2024 (2 m, Natural Resources Canada, Open Government
  Licence - Canada) and Copernicus GLO-30 (30 m, Copernicus full, free and
  open licence). Both allow storing and reuse with attribution.
- The catalogue gives licence names only, not full terms. The owner's use is
  personal and offline, which both licences permit, but the full texts have not
  been read. Do that before shipping terrain.
- The Mapterhorn attribution page states the credit text. Show
  "Elevation data by Mapterhorn" and the producer credit it asks for.

Format and size: Terrarium-encoded WebP DEM tiles in PMTiles, 512 px, up to
zoom 17. Extract per region with `pmtiles extract --bbox=...` from the planet
file (zoom 0 to 12) and the covering high-zoom file, then `pmtiles merge`.
The size for a 10 by 13 km area is unknown; `--dry-run` shows it. This is a
raster-dem source in the style, so it works the same way as the vector one.

Decision for the first version: hillshade only, as a separate
`<name>-terrain.pmtiles` per region, shown if present. Hillshade is a built-in
MapLibre layer and needs no extra code. Contour lines need either the
`maplibre-contour` plugin (a JS library, not available in MapLibre Native) or
contours precomputed into the vector tiles at build time. Precomputing needs a
separate tool (for example `pmtiles`-compatible contour generation from the DEM)
and is left for a later note once hillshade is proven.

## Storage and sizes

- Trail region about 1 MB at zoom 15 for 10 by 13 km, so a whole province of
  trail tiles stays in the hundreds of MB. Storage is not a constraint.
- Terrain size is unknown until measured. Keep it optional so the trail map
  works without it.
- Files are never modified by the app, only read or deleted.

## Permissions

None added. No network permission use for the map, so `INTERNET` can be
dropped once the demo style is gone, unless a later feature needs it.

## Left out of the first version

- Downloading regions inside the app, or a region picker on the map.
- Contour lines, 3D terrain, and trail difficulty ratings (ADR 0004 covers
  difficulty separately; the style shows OSM tags where present).
- Place search, routing, and map rotation or tilt settings.
- Updating regions when OSM changes (rebuild and copy over the old file).
- Several map styles (one style, trails first).

## Testing

- JVM unit tests: region file scanning (valid, empty, corrupt files), the
  style JSON generator (source per region, none when no regions), PMTiles
  header parsing if the bounds are read in app.
- Emulator: install the Roberts Creek and Largo Road test region, switch the
  emulator to airplane mode, and check the map, labels and position still
  work. Delete the file and check the empty state.
- Phone: same test, plus a walk outdoors with the screen off (the open item
  from the handoff).

## Decisions for the owner

1. Regions are installed by `adb push` into the app's external files folder,
   listed and deleted in a Regions screen, no in-app download yet.
2. The map makes no network requests; outside installed regions it is blank.
3. Fonts and glyphs are bundled in the APK (one open font family, no sprites at
   first).
4. Hillshade first, as an optional per-region terrain file. Contours later.
5. Before shipping terrain, read the full Open Government Licence - Canada and
   Copernicus terms and the Mapterhorn attribution wording.

## Open questions to settle in #21

- Does `getExternalFilesDir` accept `adb push` on API 36?
- Do bundled `asset://` glyphs load on MapLibre Native Android 13.7.1?
- Does the `basemaps-assets` licence allow reuse of its glyphs?
- Terrain file size for a 10 by 13 km region.
