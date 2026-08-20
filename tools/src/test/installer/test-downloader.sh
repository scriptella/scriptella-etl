#!/bin/sh

set -eu

source_file=$1
destination=$2
[ -f "$source_file" ] || exit 22
cp "$source_file" "$destination"
