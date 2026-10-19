#!/usr/bin/env bash
# Build one offline map region as a PMTiles file (docs/design/04_offline_maps.md).
#
# Usage: build-region.sh <name> <min_lon,min_lat,max_lon,max_lat> <osm.pbf>
# Needs Java and planetiler.jar (PLANETILER_JAR, default ./planetiler.jar).
# Example: build-region.sh sunshine-coast -123.72,49.40,-123.58,49.52 british-columbia.osm.pbf
set -euo pipefail

if [ "$#" -ne 3 ]; then
  sed -n '2,6p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

name=$1
bounds=$2
pbf=$3
jar=${PLANETILER_JAR:-planetiler.jar}
here=$(cd "$(dirname "$0")" && pwd)

java -Xmx4g -jar "$jar" generate-custom \
  --schema="$here/trails.yml" \
  --osm_path="$pbf" \
  --bounds="$bounds" \
  --maxzoom=15 \
  --output="$name.pmtiles" \
  --force

echo "Built $name.pmtiles. Install with:"
echo "  adb push $name.pmtiles /sdcard/Android/data/ca.mytrails.app/files/regions/"
