#!/bin/sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST_URL="https://services.gradle.org/distributions/gradle-8.7-bin.zip"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-8.7-bin"
mkdir -p "$CACHE"
ZIP="$CACHE/gradle-8.7-bin.zip"
if [ ! -f "$ZIP" ]; then
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$DIST_URL" -o "$ZIP"
  else
    wget -O "$ZIP" "$DIST_URL"
  fi
fi
DIST_DIR="$CACHE/gradle-8.7"
if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  rm -rf "$DIST_DIR.tmp"
  mkdir -p "$DIST_DIR.tmp"
  unzip -q "$ZIP" -d "$DIST_DIR.tmp"
  mv "$DIST_DIR.tmp/gradle-8.7" "$DIST_DIR"
  rm -rf "$DIST_DIR.tmp"
fi
exec "$DIST_DIR/bin/gradle" "$@"
