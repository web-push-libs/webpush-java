#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: version.sh OLD_VERSION NEW_VERSION" >&2
  exit 1
fi

OLD_VERSION=$1
NEW_VERSION=$2
OLD_VERSION_REGEX=$(printf '%s' "$OLD_VERSION" | sed 's/\./\\./g')

for file in build.gradle README.md; do
  tmp_file="${file}.tmp"
  sed "s/${OLD_VERSION_REGEX}/${NEW_VERSION}/g" "$file" > "$tmp_file"
  mv "$tmp_file" "$file"
done
