#!/bin/sh
# Build the Magisk module zip for a fresh install from the Magisk app:
# build/redmagic-astra-tuner-<version>.zip, holding the governor, the display calibration
# service and its helpers, and the settings app.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$HERE/..
VERSION=$(sed -n 's/^version=//p' "$HERE/module.prop")
sh "$HERE/build.sh"
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
cp -R "$HERE/zip/META-INF" "$STAGE/"
cp "$HERE/module.prop" "$HERE/service.sh" "$HERE/customize.sh" "$HERE/uninstall.sh" "$ROOT/panel/panel-table.sh" "$STAGE/"
cp "$ROOT/build/astrad2" "$ROOT/build/panel-service" "$ROOT/build/panel-mode.jar" "$STAGE/"
cp "$ROOT/build/app/astra.apk" "$STAGE/Astra.apk"
# Fixed timestamps, so the same commit gives the same zip.
find "$STAGE" -exec touch -t 202601010000 {} +
OUT=$ROOT/build/redmagic-astra-tuner-$VERSION.zip
rm -f "$OUT"
(cd "$STAGE" && zip -qr -X "$OUT" META-INF module.prop service.sh customize.sh uninstall.sh astrad2 panel-service panel-table.sh panel-mode.jar Astra.apk)
echo "$OUT"
