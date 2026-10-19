#!/usr/bin/env bash
# Build the optional hillshade file for a region from Mapterhorn terrain tiles
# (docs/design/04_offline_maps.md). Downloads only the tiles inside the bounds.
#
# Usage: build-terrain.sh <name> <min_lon,min_lat,max_lon,max_lat>
# Needs the pmtiles CLI (PMTILES, default pmtiles) and network access.
# Writes <name>-terrain.pmtiles; install it next to <name>.pmtiles.
# Example: build-terrain.sh sunshine-coast -123.72,49.40,-123.58,49.52
set -euo pipefail

if [ "$#" -ne 2 ]; then
  sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

name=$1
bounds=$2
pmtiles=${PMTILES:-pmtiles}
base=https://download.mapterhorn.com
maxzoom=15
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# Zoom 0 to 12 come from the planet file, zoom 13 and up from the 6-x-y file covering the bounds.
"$pmtiles" extract "$base/planet.pmtiles" "$tmp/low.pmtiles" --bbox="$bounds" --maxzoom=12

IFS=, read -r min_lon min_lat max_lon max_lat <<<"$bounds"
inputs=("$tmp/low.pmtiles")
# One zoom 6 tile spans 5.625 degrees of longitude; rows follow the Web Mercator formula.
for x in $(python3 -c "
print(*range(int((${min_lon}+180)//5.625), int((${max_lon}+180)//5.625)+1))"); do
  for y in $(python3 -c "
import math
def row(lat):
    r = math.radians(lat)
    return int((1 - math.asinh(math.tan(r)) / math.pi) / 2 * 64)
print(*range(row(${max_lat}), row(${min_lat})+1))"); do
  part="$tmp/6-$x-$y.pmtiles"
  "$pmtiles" extract "$base/6-$x-$y.pmtiles" "$part" --bbox="$bounds" --maxzoom=$maxzoom
  inputs+=("$part")
  done
done

"$pmtiles" merge "${inputs[@]}" "$name-terrain.pmtiles"

echo "Built $name-terrain.pmtiles. Install with:"
echo "  adb push $name-terrain.pmtiles /sdcard/Android/data/ca.mytrails.app/files/regions/"
