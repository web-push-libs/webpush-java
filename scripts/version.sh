#!/usr/bin/env sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: version.sh OLD_VERSION NEW_VERSION" && exit 1
fi

OLD_VERSION=$1
NEW_VERSION=$2

files=(
  "build.gradle"
  "README.md"
)

for file in ${files[@]}; do
  sed -i '' "s/$OLD_VERSION/$NEW_VERSION/g" $file
done


