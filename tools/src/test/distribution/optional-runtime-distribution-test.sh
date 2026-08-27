#!/bin/sh
# Artifact-level smoke test for Scriptella's optional runtime contracts.

set -eu

binary_zip=$1
examples_zip=$2
source_zip=$3
rhino_engine_jar=$4
rhino_runtime_jar=$5

fail() {
    echo "optional runtime contract failed: $*" >&2
    exit 1
}

assert_contains() {
    grep -F "$2" "$1" >/dev/null || fail "$1 does not contain: $2"
}

for required_file in "$binary_zip" "$examples_zip" "$source_zip" "$rhino_engine_jar" "$rhino_runtime_jar"; do
    [ -f "$required_file" ] || fail "missing required file: $required_file"
done

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/scriptella-runtime-contract.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT HUP INT TERM

mkdir "$work_dir/binary" "$work_dir/examples" "$work_dir/source"
unzip -q "$binary_zip" -d "$work_dir/binary"
unzip -q "$examples_zip" -d "$work_dir/examples"
unzip -q "$source_zip" -d "$work_dir/source"
dist_dir=$(find "$work_dir/binary" -mindepth 1 -maxdepth 1 -type d | head -1)
[ -n "$dist_dir" ] || fail "binary distribution has no root directory"
examples_dir="$work_dir/examples"
source_dir="$work_dir/source"
[ -d "$source_dir/lib" ] || fail "source distribution has no top-level lib directory"

assert_rhino_bundle() {
    bundle_lib=$1
    [ -d "$bundle_lib" ] || fail "missing library directory: $bundle_lib"

    find "$bundle_lib" -maxdepth 1 -type f -name 'rhino*.jar' -exec basename {} \; \
        | sort >"$work_dir/actual-rhino-jars.txt"
    {
        echo "rhino-engine.jar"
        echo "rhino.jar"
    } >"$work_dir/expected-rhino-jars.txt"
    cmp "$work_dir/expected-rhino-jars.txt" "$work_dir/actual-rhino-jars.txt" >/dev/null \
        || fail "$bundle_lib does not contain exactly the supported Rhino JARs"

    cmp "$rhino_engine_jar" "$bundle_lib/rhino-engine.jar" >/dev/null \
        || fail "$bundle_lib/rhino-engine.jar does not match the selected artifact"
    cmp "$rhino_runtime_jar" "$bundle_lib/rhino.jar" >/dev/null \
        || fail "$bundle_lib/rhino.jar does not match the selected artifact"

    [ -f "$bundle_lib/rhino.license.txt" ] || fail "$bundle_lib has no Rhino license"
    [ -f "$bundle_lib/rhino.source.txt" ] || fail "$bundle_lib has no Rhino source notice"
    assert_contains "$bundle_lib/rhino.license.txt" "Mozilla Public License Version 2.0"
    assert_contains "$bundle_lib/rhino.source.txt" "org.mozilla:rhino-engine:1.9.1"
    assert_contains "$bundle_lib/rhino.source.txt" "org.mozilla:rhino:1.9.1"
    assert_contains "$bundle_lib/rhino.source.txt" \
        "rhino-engine/1.9.1/rhino-engine-1.9.1-sources.jar"
    assert_contains "$bundle_lib/rhino.source.txt" \
        "rhino/1.9.1/rhino-1.9.1-sources.jar"

    unzip -p "$bundle_lib/rhino-engine.jar" META-INF/MANIFEST.MF \
        | tr -d '\r' >"$work_dir/rhino-engine-manifest.txt"
    unzip -p "$bundle_lib/rhino.jar" META-INF/MANIFEST.MF \
        | tr -d '\r' >"$work_dir/rhino-manifest.txt"
    assert_contains "$work_dir/rhino-engine-manifest.txt" "Implementation-Version: 1.9.1"
    assert_contains "$work_dir/rhino-manifest.txt" "Implementation-Version: 1.9.1"
    provider=$(unzip -p "$bundle_lib/rhino-engine.jar" \
        META-INF/services/javax.script.ScriptEngineFactory | tr -d '\r\n')
    [ "$provider" = "org.mozilla.javascript.engine.RhinoScriptEngineFactory" ] \
        || fail "$bundle_lib/rhino-engine.jar has an unexpected JSR-223 provider"
}

assert_rhino_bundle "$dist_dir/lib"
assert_rhino_bundle "$examples_dir/lib"

assert_velocity_dependencies() {
    bundle_lib=$1
    [ -d "$bundle_lib" ] || fail "missing library directory: $bundle_lib"

    for artifact in commons-lang3 slf4j-api; do
        [ -f "$bundle_lib/$artifact.jar" ] \
            || fail "$bundle_lib has no $artifact.jar"
        [ -f "$bundle_lib/$artifact.license.txt" ] \
            || fail "$bundle_lib has no $artifact license"
    done
}

assert_velocity_dependencies "$examples_dir/lib"
[ ! -f "$examples_dir/lib/commons-collections.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Commons Collections 3"
[ ! -f "$examples_dir/lib/commons-lang.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Commons Lang 2"
[ ! -f "$examples_dir/lib/velocity.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Velocity 1.x"
[ ! -f "$dist_dir/lib/velocity-engine-core.jar" ] \
    || fail "$dist_dir/lib unexpectedly bundles velocity-engine-core.jar"
[ ! -f "$dist_dir/lib/commons-lang3.jar" ] \
    || fail "$dist_dir/lib unexpectedly bundles commons-lang3.jar"
[ ! -f "$dist_dir/lib/slf4j-api.jar" ] \
    || fail "$dist_dir/lib unexpectedly bundles slf4j-api.jar"
[ -f "$examples_dir/lib/velocity-engine-core.jar" ] \
    || fail "$examples_dir/lib has no velocity-engine-core.jar"
[ -f "$examples_dir/lib/velocity-engine-core.license.txt" ] \
    || fail "$examples_dir/lib has no Velocity Engine license"
[ ! -f "$examples_dir/lib/micrometer-observation.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Spring's Micrometer observation API"
[ ! -f "$examples_dir/lib/micrometer-commons.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Spring's Micrometer commons API"
[ ! -f "$examples_dir/lib/jspecify.jar" ] \
    || fail "$examples_dir/lib unexpectedly contains Spring's JSpecify annotations"

assert_jar_version() {
    jar_file=$1
    expected_version=$2
    manifest_file="$work_dir/$(basename "$jar_file").manifest.txt"
    unzip -p "$jar_file" META-INF/MANIFEST.MF \
        | tr -d '\r' >"$manifest_file"
    assert_contains "$manifest_file" "Implementation-Version: $expected_version"
}

assert_jar_version "$examples_dir/lib/velocity-engine-core.jar" "2.4.1"
assert_jar_version "$examples_dir/lib/commons-lang3.jar" "3.20.0"
assert_jar_version "$examples_dir/lib/slf4j-api.jar" "1.7.36"

find "$source_dir/lib" -maxdepth 1 -type f -name 'spring-*.jar' -exec basename {} \; \
    | sort >"$work_dir/actual-spring-jars.txt"
{
    echo "spring-aop.jar"
    echo "spring-beans.jar"
    echo "spring-context.jar"
    echo "spring-core.jar"
    echo "spring-expression.jar"
    echo "spring-jdbc.jar"
    echo "spring-tx.jar"
} >"$work_dir/expected-spring-jars.txt"
cmp "$work_dir/expected-spring-jars.txt" "$work_dir/actual-spring-jars.txt" >/dev/null \
    || fail "source distribution does not contain exactly the supported Spring JARs"
[ ! -f "$source_dir/lib/spring-jcl.jar" ] \
    || fail "source distribution unexpectedly contains removed spring-jcl.jar"
for artifact in spring-aop spring-beans spring-context spring-core spring-expression spring-jdbc spring-tx; do
assert_jar_version "$source_dir/lib/$artifact.jar" "7.0.9"
done
assert_jar_version "$source_dir/lib/commons-logging.jar" "1.3.5"

find "$source_dir/lib" -maxdepth 1 -type f -name 'micrometer-*.jar' -exec basename {} \; \
    | sort >"$work_dir/actual-micrometer-jars.txt"
{
    echo "micrometer-commons.jar"
    echo "micrometer-observation.jar"
} >"$work_dir/expected-micrometer-jars.txt"
cmp "$work_dir/expected-micrometer-jars.txt" "$work_dir/actual-micrometer-jars.txt" >/dev/null \
    || fail "source distribution does not contain exactly the Spring Micrometer runtime set"
for artifact in micrometer-observation micrometer-commons; do
    assert_jar_version "$source_dir/lib/$artifact.jar" "1.16.7"
    [ -f "$source_dir/lib/$artifact.license.txt" ] \
        || fail "source distribution has no $artifact license"
done
assert_jar_version "$source_dir/lib/jspecify.jar" "1.0.0"
[ -f "$source_dir/lib/jspecify.license.txt" ] \
    || fail "source distribution has no JSpecify license"

main_jar="$dist_dir/scriptella.jar"
[ -f "$main_jar" ] || fail "binary distribution has no scriptella.jar"
if jar tf "$main_jar" | grep -E '(^|/)org/mozilla/javascript/' >/dev/null; then
    fail "scriptella.jar embeds Rhino classes"
fi
if unzip -p "$main_jar" META-INF/services/javax.script.ScriptEngineFactory \
        | grep -F 'org.mozilla.javascript' >/dev/null; then
    fail "scriptella.jar registers Rhino"
fi
unzip -p "$main_jar" META-INF/MANIFEST.MF | tr -d '\r' >"$work_dir/scriptella-manifest.txt"
assert_contains "$work_dir/scriptella-manifest.txt" \
    "Class-Path: lib/rhino-engine.jar lib/rhino.jar"

assert_contains "$dist_dir/bin/scriptella.sh" 'lib/*.jar'
[ -x "$dist_dir/bin/scriptella.sh" ] \
    || fail "$dist_dir/bin/scriptella.sh is not executable"
assert_contains "$dist_dir/bin/scriptella.bat" '\lib'
assert_contains "$dist_dir/bin/scriptella.bat" '%*'

mkdir "$work_dir/launcher-cycle"
ln -s launcher-b "$work_dir/launcher-cycle/launcher-a"
ln -s launcher-a "$work_dir/launcher-cycle/launcher-b"
if perl -e 'alarm shift; exec @ARGV' 5 sh -c '. "$1"' \
        "$work_dir/launcher-cycle/launcher-a" "$dist_dir/bin/scriptella.sh" \
        >"$work_dir/launcher-cycle.out" 2>"$work_dir/launcher-cycle.err"; then
    fail "launcher symlink cycle unexpectedly succeeded"
fi
assert_contains "$work_dir/launcher-cycle.err" \
    'Scriptella launcher: too many symbolic links'

cat >"$work_dir/jexl.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="jexl" driver="jexl"/>
    <script connection-id="jexl">answer = 6 * 7;</script>
</etl>
EOF
java -jar "$main_jar" "$work_dir/jexl.etl.xml" \
    >"$work_dir/jexl.out" 2>&1 || fail "bundled JEXL ETL failed through java -jar"

mv "$dist_dir/lib/rhino-engine.jar" "$work_dir/rhino-engine.jar"
mv "$dist_dir/lib/rhino.jar" "$work_dir/rhino.jar"
cat >"$work_dir/javascript-missing.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="javascript" driver="script"/>
    <script connection-id="javascript">1 + 1;</script>
</etl>
EOF
if java -jar "$main_jar" "$work_dir/javascript-missing.etl.xml" \
        >"$work_dir/javascript-missing.out" 2>&1; then
    fail "JavaScript unexpectedly worked without its bundled provider"
fi
assert_contains "$work_dir/javascript-missing.out" 'language=js'
assert_contains "$work_dir/javascript-missing.out" 'Available values are:'
assert_contains "$work_dir/javascript-missing.out" 'Rhino JSR-223 provider on JDK 17'
assert_contains "$work_dir/javascript-missing.out" 'org.mozilla:rhino-engine:1.9.1'
assert_contains "$work_dir/javascript-missing.out" 'org.mozilla:rhino:1.9.1'
assert_contains "$work_dir/javascript-missing.out" \
    'Official Scriptella distributions bundle both JARs under lib/'
assert_contains "$work_dir/javascript-missing.out" 'restore the complete distribution'
mv "$work_dir/rhino-engine.jar" "$dist_dir/lib/rhino-engine.jar"
mv "$work_dir/rhino.jar" "$dist_dir/lib/rhino.jar"

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
    java -jar "$main_jar" "$work_dir/javascript-$language.etl.xml" \
        >"$work_dir/javascript-$language.out" 2>&1 \
        || fail "java -jar failed for language=$language"
    assert_contains "$work_dir/javascript-$language.out" "JAVASCRIPT_$language"
done

sh "$dist_dir/bin/scriptella.sh" "$work_dir/javascript-js.etl.xml" \
    >"$work_dir/javascript-launcher.out" 2>&1 || fail "Unix launcher JavaScript failed"
assert_contains "$work_dir/javascript-launcher.out" "JAVASCRIPT_js"

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
java -jar "$main_jar" "$work_dir/javascript-nested.etl.xml" \
    >"$work_dir/javascript-nested.out" 2>&1 || fail "nested JavaScript ETL failed"
assert_contains "$work_dir/javascript-nested.out" 'NESTED_0'
assert_contains "$work_dir/javascript-nested.out" 'NESTED_1'

for template in header.vm footer.vm; do
    [ -f "$examples_dir/primes/$template" ] \
        || fail "packaged primes sample has no $template"
done
(
    cd "$examples_dir/primes"
    java -jar ../lib/scriptella.jar etl.xml \
        >"$work_dir/primes.out" 2>&1
) || fail "packaged primes ETL failed with split Velocity dependencies"
[ -s "$examples_dir/primes/report.html" ] \
    || fail "packaged primes ETL did not create its Velocity report"

cat >"$work_dir/unknown-language.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="unknown" driver="script">language=python</connection>
    <script connection-id="unknown">1 + 1;</script>
</etl>
EOF
if java -jar "$main_jar" "$work_dir/unknown-language.etl.xml" \
        >"$work_dir/unknown-language.out" 2>&1; then
    fail "unknown language unexpectedly worked"
fi
assert_contains "$work_dir/unknown-language.out" 'language=python'
if grep -F 'org.mozilla:' "$work_dir/unknown-language.out" >/dev/null; then
    fail "unknown-language diagnostic recommends Rhino"
fi

echo "Optional runtime distribution contract passed."
installer_test=$(CDPATH= cd -- "$(dirname "$0")/../installer" && pwd)/installer-test.sh
sh "$installer_test" "$binary_zip" \
    || fail "installer integration test failed"
