#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
    echo "usage: validate-etl.sh SCRIPTELLA_JAR ETL_FILE" >&2
    exit 64
fi

scriptella_jar=$1
etl_file=$2

test -r "$scriptella_jar"
test -r "$etl_file"
command -v unzip >/dev/null
command -v xmllint >/dev/null

validation_dir=$(mktemp -d "${TMPDIR:-/tmp}/scriptella-validate.XXXXXX")
trap 'rm -rf "$validation_dir"' EXIT HUP INT TERM

unzip -p "$scriptella_jar" scriptella/dtd/etl.dtd >"$validation_dir/etl.dtd"
cat >"$validation_dir/catalog.xml" <<'EOF'
<?xml version="1.0"?>
<!DOCTYPE catalog PUBLIC "-//OASIS//DTD Entity Resolution XML Catalog V1.0//EN"
                         "http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd">
<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
  <system systemId="http://scriptella.org/dtd/etl.dtd" uri="etl.dtd"/>
</catalog>
EOF

XML_CATALOG_FILES="$validation_dir/catalog.xml" \
    xmllint --nonet --noout --valid "$etl_file"
printf 'VALID %s\n' "$etl_file"
