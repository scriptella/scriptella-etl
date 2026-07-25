#!/bin/sh
# Artifact-level smoke test for Scriptella's optional JavaScript provider contract.

set -eu

binary_zip=$1
examples_zip=$2
rhino_engine_jar=$3
rhino_runtime_jar=$4

fail() {
    echo "optional JavaScript contract failed: $*" >&2
    exit 1
}

assert_contains() {
    grep -F "$2" "$1" >/dev/null || fail "$1 does not contain: $2"
}

assert_archive_has_no_rhino() {
    if unzip -Z1 "$1" | grep -Ei '(^|/)(rhino[^/]*\.(jar|license\.txt)|org/mozilla/javascript/)' >/dev/null; then
        fail "$1 contains Rhino"
    fi
}

for required_file in "$binary_zip" "$examples_zip" "$rhino_engine_jar" "$rhino_runtime_jar"; do
    [ -f "$required_file" ] || fail "missing required file: $required_file"
done

assert_archive_has_no_rhino "$binary_zip"
assert_archive_has_no_rhino "$examples_zip"

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/scriptella-js-contract.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT HUP INT TERM

mkdir "$work_dir/unpacked"
unzip -q "$binary_zip" -d "$work_dir/unpacked"
dist_dir=$(find "$work_dir/unpacked" -mindepth 1 -maxdepth 1 -type d | head -1)
[ -n "$dist_dir" ] || fail "binary distribution has no root directory"

main_jar="$dist_dir/scriptella.jar"
[ -f "$main_jar" ] || fail "binary distribution has no scriptella.jar"
if jar tf "$main_jar" | grep -E '(^|/)org/mozilla/javascript/' >/dev/null; then
    fail "scriptella.jar embeds Rhino classes"
fi
if unzip -p "$main_jar" META-INF/services/javax.script.ScriptEngineFactory \
        | grep -F 'org.mozilla.javascript' >/dev/null; then
    fail "scriptella.jar registers Rhino"
fi

assert_contains "$dist_dir/bin/scriptella.sh" 'lib/*.jar'
assert_contains "$dist_dir/bin/scriptella.bat" '\lib'
assert_contains "$dist_dir/bin/scriptella.bat" '%*'

cat >"$work_dir/jexl.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="jexl" driver="jexl"/>
    <script connection-id="jexl">answer = 6 * 7;</script>
</etl>
EOF
sh "$dist_dir/bin/scriptella.sh" "$work_dir/jexl.etl.xml" \
    >"$work_dir/jexl.out" 2>&1 || fail "bundled JEXL ETL failed"

cat >"$work_dir/javascript-missing.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="javascript" driver="script"/>
    <script connection-id="javascript">1 + 1;</script>
</etl>
EOF
if sh "$dist_dir/bin/scriptella.sh" "$work_dir/javascript-missing.etl.xml" \
        >"$work_dir/javascript-missing.out" 2>&1; then
    fail "JavaScript unexpectedly worked without a provider"
fi
assert_contains "$work_dir/javascript-missing.out" 'language=js'
assert_contains "$work_dir/javascript-missing.out" 'Available values are:'
assert_contains "$work_dir/javascript-missing.out" 'external JSR-223 provider on JDK 17'
assert_contains "$work_dir/javascript-missing.out" 'org.mozilla:rhino-engine:1.9.1'
assert_contains "$work_dir/javascript-missing.out" 'org.mozilla:rhino:1.9.1'
assert_contains "$work_dir/javascript-missing.out" 'bin/scriptella.sh'
assert_contains "$work_dir/javascript-missing.out" 'bin/scriptella.bat'
assert_contains "$work_dir/javascript-missing.out" 'plain java -jar'

cp "$rhino_engine_jar" "$dist_dir/lib/rhino-engine.jar"
cp "$rhino_runtime_jar" "$dist_dir/lib/rhino.jar"

for language in omitted js JavaScript rhino; do
    if [ "$language" = omitted ]; then
        language_property=
    else
        language_property="language=$language"
    fi
    cat >"$work_dir/javascript-$language.etl.xml" <<EOF
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="javascript" driver="script">$language_property</connection>
    <script connection-id="javascript">
        java.lang.System.out.println("JAVASCRIPT_$language");
    </script>
</etl>
EOF
    sh "$dist_dir/bin/scriptella.sh" "$work_dir/javascript-$language.etl.xml" \
        >"$work_dir/javascript-$language.out" 2>&1 \
        || fail "JavaScript failed for language=$language"
    assert_contains "$work_dir/javascript-$language.out" "JAVASCRIPT_$language"
done

cat >"$work_dir/javascript-nested.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="javascript" driver="script">language=js</connection>
    <query connection-id="javascript">
        for (var i = 0; i &lt; 2; i++) {
            nestedValue = i;
            query.next();
        }
        <script>
            java.lang.System.out.println("NESTED_" + nestedValue);
        </script>
    </query>
</etl>
EOF
sh "$dist_dir/bin/scriptella.sh" "$work_dir/javascript-nested.etl.xml" \
    >"$work_dir/javascript-nested.out" 2>&1 || fail "nested JavaScript ETL failed"
assert_contains "$work_dir/javascript-nested.out" 'NESTED_0'
assert_contains "$work_dir/javascript-nested.out" 'NESTED_1'

cat >"$work_dir/unknown-language.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="unknown" driver="script">language=python</connection>
    <script connection-id="unknown">1 + 1;</script>
</etl>
EOF
if sh "$dist_dir/bin/scriptella.sh" "$work_dir/unknown-language.etl.xml" \
        >"$work_dir/unknown-language.out" 2>&1; then
    fail "unknown language unexpectedly worked"
fi
assert_contains "$work_dir/unknown-language.out" 'language=python'
if grep -F 'org.mozilla:' "$work_dir/unknown-language.out" >/dev/null; then
    fail "unknown-language diagnostic recommends Rhino"
fi

echo "Optional JavaScript distribution contract passed."
