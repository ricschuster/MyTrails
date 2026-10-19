# Building offline map regions

`build-region.sh` makes one PMTiles region file from an OSM extract using
`trails.yml`. Design: `docs/design/04_offline_maps.md`.

1. Download `planetiler.jar` (v0.10.2) and a Geofabrik `.osm.pbf` for your area.
2. `PLANETILER_JAR=planetiler.jar tools/tiles/build-region.sh <name> <min_lon,min_lat,max_lon,max_lat> <extract.osm.pbf>`
   (about 30 seconds for a 10 by 13 km area, Java 21 or newer).
3. `adb push <name>.pmtiles /sdcard/Android/data/ca.mytrails.app/files/regions/`
4. Open the app. Reopen the Regions screen and go back to reload the map.

Layer and attribute names in `trails.yml` are used by
`app/src/main/assets/style/region-layers.json`; change them together.
