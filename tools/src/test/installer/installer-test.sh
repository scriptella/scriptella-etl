#!/bin/sh
# Offline integration tests for the curl installer.

set -eu

fail() {
    echo "installer integration test: $*" >&2
    exit 1
}

assert_file() {
    [ -f "$1" ] || fail "expected file: $1"
}

assert_dir() {
    [ -d "$1" ] || fail "expected directory: $1"
}

assert_not_exists() {
    [ ! -e "$1" ] && [ ! -L "$1" ] || fail "unexpected path: $1"
}

assert_contains() {
    grep -F "$2" "$1" >/dev/null || fail "$1 does not contain: $2"
}

assert_not_contains() {
    if grep -F "$2" "$1" >/dev/null 2>&1; then
        fail "$1 unexpectedly contains: $2"
    fi
}

sha256_file() {
    checksum_file=$1
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$checksum_file" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$checksum_file" | awk '{print $1}'
    else
        fail 'missing checksum command (need sha256sum or shasum)'
    fi
}

script_dir=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$script_dir/../../../.." && pwd)
installer=$repo_dir/install.sh
downloader=$script_dir/test-downloader.sh
[ -x "$installer" ] || fail "installer is not executable"
[ -x "$downloader" ] || fail "test downloader is not executable"

for command_name in mktemp mkdir rm cp chmod zip unzip grep awk sed perl; do
    command -v "$command_name" >/dev/null 2>&1 || fail "missing test command: $command_name"
done

work_dir=$(mktemp -d "${TMPDIR-/tmp}/scriptella-installer-test.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT HUP INT TERM
fixture_dir=$work_dir/fixture
mkdir -p "$fixture_dir/scriptella-1.4/bin" "$fixture_dir/scriptella-1.4/lib"
printf '%s\n' '#!/bin/sh' '# lib/*.jar' 'set -eu' 'case "${1-}" in --version) echo fixture-1.4; exit 0;; esac' 'printf "%s\n" "$1" > "${SCRIPTELLA_EXECUTION_LOG:?}"' \
    >"$fixture_dir/scriptella-1.4/bin/scriptella.sh"
chmod 755 "$fixture_dir/scriptella-1.4/bin/scriptella.sh"
printf 'miniature scriptella jar\n' >"$fixture_dir/scriptella-1.4/scriptella.jar"
printf 'provider\n' >"$fixture_dir/scriptella-1.4/lib/provider.jar"
printf 'extra\n' >"$fixture_dir/scriptella-1.4/extra.txt"
(cd "$fixture_dir" && zip -q -r valid.zip scriptella-1.4)
valid_archive=$fixture_dir/valid.zip

archive_sha256=$(sha256_file "$valid_archive")

make_renamed_archive() {
    source_archive=$1
    renamed_archive=$2
    renamed_entry=$3
    cp "$source_archive" "$renamed_archive"
    case "$renamed_entry" in
        ../*) replacement="../outside.txt.........." ;;
        /*) replacement="/absolute-outside.txt..." ;;
        *) fail "unsupported malicious fixture name: $renamed_entry" ;;
    esac
    REPLACEMENT=$replacement perl -0777 -pi -e \
        's/scriptella-1\.4\/extra\.txt/$ENV{REPLACEMENT}/g' "$renamed_archive"
}

make_renamed_archive "$valid_archive" "$fixture_dir/traversal.zip" '../outside.txt'
make_renamed_archive "$valid_archive" "$fixture_dir/absolute.zip" '/absolute-outside.txt'
traversal_sha256=$(sha256_file "$fixture_dir/traversal.zip")
absolute_sha256=$(sha256_file "$fixture_dir/absolute.zip")

printf 'not a zip\n' >"$fixture_dir/invalid.zip"
invalid_sha256=$(sha256_file "$fixture_dir/invalid.zip")

make_non_executable_archive() {
    rm -f "$fixture_dir/non-executable.zip"
    chmod 644 "$fixture_dir/scriptella-1.4/bin/scriptella.sh"
    (cd "$fixture_dir" && zip -q -r -FS non-executable.zip scriptella-1.4)
    chmod 755 "$fixture_dir/scriptella-1.4/bin/scriptella.sh"
}
make_non_executable_archive
non_executable_sha256=$(sha256_file "$fixture_dir/non-executable.zip")

symlink_archive=$fixture_dir/symlink.zip
mkdir "$work_dir/symlink-outside"
ln -s ../../symlink-outside "$fixture_dir/scriptella-1.4/escape"
(cd "$fixture_dir" && zip -q -y "$symlink_archive" scriptella-1.4/escape)
rm "$fixture_dir/scriptella-1.4/escape"
mkdir "$fixture_dir/scriptella-1.4/escape"
printf 'escaped payload\n' >"$fixture_dir/scriptella-1.4/escape/payload.txt"
(cd "$fixture_dir" && zip -q "$symlink_archive" \
    scriptella-1.4/escape/payload.txt \
    scriptella-1.4/bin/scriptella.sh \
    scriptella-1.4/lib/provider.jar \
    scriptella-1.4/scriptella.jar)
rm -rf "$fixture_dir/scriptella-1.4/escape"
symlink_sha256=$(sha256_file "$symlink_archive")

common_path=/usr/bin:/bin
run_install() {
    run_home=$1
    run_path=$2
    run_archive=$3
    run_sha256=$4
    run_java=$5
    run_output=$run_home/install.stdout
    run_errors=$run_home/install.stderr
    mkdir -p "$run_home"
    if SCRIPTELLA_INSTALLER_HOME=$run_home \
        SCRIPTELLA_INSTALLER_PATH=$run_path \
        SCRIPTELLA_INSTALLER_ARCHIVE_URL=$run_archive \
        SCRIPTELLA_INSTALLER_SHA256=$run_sha256 \
        SCRIPTELLA_INSTALLER_DOWNLOADER=$downloader \
        SCRIPTELLA_INSTALLER_JAVA_COMMAND=$run_java \
        SCRIPTELLA_INSTALLER_STARTUP_FILE=$run_home/.profile \
        sh "$installer" >"$run_output" 2>"$run_errors"; then
        return 0
    fi
    return 1
}

run_install_with_shell_startup() {
    run_home=$1
    run_path=$2
    run_archive=$3
    run_sha256=$4
    run_java=$5
    run_shell=$6
    run_output=$run_home/install.stdout
    run_errors=$run_home/install.stderr
    mkdir -p "$run_home"
    if (
        unset SCRIPTELLA_INSTALLER_STARTUP_FILE
        SCRIPTELLA_INSTALLER_HOME=$run_home \
            SCRIPTELLA_INSTALLER_PATH=$run_path \
            SCRIPTELLA_INSTALLER_ARCHIVE_URL=$run_archive \
            SCRIPTELLA_INSTALLER_SHA256=$run_sha256 \
            SCRIPTELLA_INSTALLER_DOWNLOADER=$downloader \
            SCRIPTELLA_INSTALLER_JAVA_COMMAND=$run_java \
            SHELL=$run_shell \
            sh "$installer"
    ) >"$run_output" 2>"$run_errors"; then
        return 0
    fi
    return 1
}

expect_install_failure() {
    failure_home=$1
    failure_path=$2
    failure_archive=$3
    failure_sha256=$4
    failure_java=$5
    if run_install "$failure_home" "$failure_path" "$failure_archive" "$failure_sha256" "$failure_java"; then
        cat "$failure_home/install.stderr" >&2
        fail "installation unexpectedly succeeded for $failure_archive"
    fi
}

home_one="$work_dir/home with spaces"
mkdir -p "$home_one/bin"
run_install "$home_one" "$home_one/bin:$common_path" "$valid_archive" "$archive_sha256" ''
assert_dir "$home_one/.local/scriptella"
assert_file "$home_one/.local/scriptella/scriptella.jar"
assert_file "$home_one/.local/scriptella/lib/provider.jar"
assert_file "$home_one/.local/scriptella/bin/scriptella.sh"
[ -x "$home_one/.local/scriptella/bin/scriptella.sh" ] || fail 'launcher lost executable mode'
assert_not_exists "$home_one/bin/scriptella"
assert_not_exists "$home_one/bin/scriptella.sh"
assert_contains "$home_one/install.stderr" 'Java 17 or newer was not found'
assert_contains "$home_one/install.stdout" 'Example: scriptella.sh path/to/file.etl.xml'
assert_contains "$home_one/install.stdout" 'PATH updated in'

SCRIPTELLA_EXECUTION_LOG=$home_one/execution.log \
    PATH="$home_one/.local/scriptella/bin:$common_path" \
    scriptella.sh sample.etl.xml
assert_contains "$home_one/execution.log" 'sample.etl.xml'
run_install "$home_one" "$home_one/bin:$common_path" "$valid_archive" "$archive_sha256" ''
assert_contains "$home_one/install.stdout" 'PATH stanza already present'

home_on_path="$work_dir/on-path-home"
on_path_dir="$home_on_path/.local/scriptella/bin"
mkdir -p "$on_path_dir"
run_install "$home_on_path" "$on_path_dir:$common_path" "$valid_archive" "$archive_sha256" ''
assert_contains "$home_on_path/install.stdout" 'PATH already contains'
assert_not_exists "$home_on_path/.profile"

home_two="$work_dir/fallback-home"
run_install "$home_two" "$common_path" "$valid_archive" "$archive_sha256" ''
assert_file "$home_two/.profile"
assert_contains "$home_two/.profile" '# Scriptella installer PATH (Scriptella bin; do not edit)'
assert_contains "$home_two/.profile" '.local/scriptella/bin'
[ "$(grep -F -c '# Scriptella installer PATH (Scriptella bin; do not edit)' "$home_two/.profile")" -eq 1 ] || fail 'initial PATH stanza count mismatch'
run_install "$home_two" "$common_path" "$valid_archive" "$archive_sha256" ''
[ "$(grep -F -c '# Scriptella installer PATH (Scriptella bin; do not edit)' "$home_two/.profile")" -eq 1 ] || fail 'PATH stanza was duplicated'

home_transition="$work_dir/legacy-path-home"
mkdir -p "$home_transition"
cat >"$home_transition/.profile" <<'EOF'
# Scriptella installer PATH (do not edit)
if [ -d "$HOME/.local/bin" ]; then
    case ":${PATH-}:" in
        *:"$HOME/.local/bin":*) ;;
        *) PATH="$HOME/.local/bin:${PATH-}"; export PATH ;;
    esac
fi
EOF
run_install "$home_transition" "$common_path" "$valid_archive" "$archive_sha256" ''
assert_contains "$home_transition/.profile" '# Scriptella installer PATH (Scriptella bin; do not edit)'
assert_contains "$home_transition/.profile" '.local/scriptella/bin'
[ "$(grep -F -c '# Scriptella installer PATH (Scriptella bin; do not edit)' "$home_transition/.profile")" -eq 1 ] \
    || fail 'legacy PATH marker was mistaken for the new stanza'
assert_contains "$home_transition/install.stdout" 'PATH updated in'

home_three="$work_dir/collision-home"
mkdir -p "$home_three/.profile"
expect_install_failure "$home_three" "$home_three/bin:$common_path" "$valid_archive" "$archive_sha256" ''
assert_not_exists "$home_three/.local/scriptella"

home_four="$work_dir/download-failure-home"
expect_install_failure "$home_four" "$common_path" "$fixture_dir/missing.zip" "$archive_sha256" ''
assert_not_exists "$home_four/.local/scriptella"

home_five="$work_dir/checksum-failure-home"
expect_install_failure "$home_five" "$common_path" "$valid_archive" "0000000000000000000000000000000000000000000000000000000000000000" ''
assert_not_exists "$home_five/.local/scriptella"

home_six="$work_dir/invalid-archive-home"
expect_install_failure "$home_six" "$common_path" "$fixture_dir/invalid.zip" "$invalid_sha256" ''
assert_not_exists "$home_six/.local/scriptella"

home_seven="$work_dir/traversal-home"
expect_install_failure "$home_seven" "$common_path" "$fixture_dir/traversal.zip" "$traversal_sha256" ''
assert_not_exists "$home_seven/.local/scriptella"
assert_not_exists "$home_seven/outside.txt"

home_eight="$work_dir/absolute-home"
expect_install_failure "$home_eight" "$common_path" "$fixture_dir/absolute.zip" "$absolute_sha256" ''
assert_not_exists "$home_eight/.local/scriptella"
assert_not_exists "/absolute-outside.txt"

home_nine="$work_dir/mode-home"
expect_install_failure "$home_nine" "$common_path" "$fixture_dir/non-executable.zip" "$non_executable_sha256" ''
assert_not_exists "$home_nine/.local/scriptella"

home_symlink="$work_dir/symlink-archive-home"
expect_install_failure "$home_symlink" "$common_path" "$symlink_archive" "$symlink_sha256" ''
assert_contains "$home_symlink/install.stderr" 'archive contains a symbolic-link entry'
assert_not_exists "$home_symlink/.local/scriptella"
assert_not_exists "$work_dir/symlink-outside/payload.txt"

home_ten="$work_dir/preservation-home"
preserved_path="$home_ten/.local/scriptella/bin"
run_install "$home_ten" "$preserved_path:$common_path" "$valid_archive" "$archive_sha256" ''
printf 'keep this installation\n' >"$home_ten/.local/scriptella/preservation-marker"
expect_install_failure "$home_ten" "$preserved_path:$common_path" "$valid_archive" "0000000000000000000000000000000000000000000000000000000000000000" ''
assert_contains "$home_ten/.local/scriptella/preservation-marker" 'keep this installation'
mkdir "$home_ten/.profile"
expect_install_failure "$home_ten" "$common_path" "$valid_archive" "$archive_sha256" ''
assert_contains "$home_ten/.local/scriptella/preservation-marker" 'keep this installation'
[ -d "$home_ten/.profile" ] || fail 'startup collision was not preserved'
assert_not_exists "$home_ten/.local/bin"

java_eight="$work_dir/java8"
printf '%s\n' '#!/bin/sh' "echo 'java version \"1.8.0_402\"' >&2" >"$java_eight"
chmod 755 "$java_eight"
home_eleven="$work_dir/java-warning-home"
run_install "$home_eleven" "$common_path" "$valid_archive" "$archive_sha256" "$java_eight"
assert_contains "$home_eleven/install.stderr" 'Java 8 was found'
assert_dir "$home_eleven/.local/scriptella"

home_bashrc="$work_dir/bashrc-home"
mkdir -p "$home_bashrc"
printf 'alias preserved=true\n' >"$home_bashrc/.bashrc"
if ! run_install_with_shell_startup "$home_bashrc" "$common_path" "$valid_archive" "$archive_sha256" '' /bin/bash; then
    cat "$home_bashrc/install.stderr" >&2
    fail 'bash startup-file installation failed'
fi
assert_contains "$home_bashrc/.bashrc" 'alias preserved=true'
assert_not_contains "$home_bashrc/.bashrc" '# Scriptella installer PATH'
assert_not_exists "$home_bashrc/.bash_profile"
assert_not_exists "$home_bashrc/.bash_login"
assert_not_exists "$home_bashrc/.profile"
assert_contains "$home_bashrc/install.stdout" 'PATH was not changed automatically.'
assert_contains "$home_bashrc/install.stdout" 'export PATH="$HOME/.local/scriptella/bin:$PATH"'

home_bash_profile="$work_dir/bash-profile-home"
mkdir -p "$home_bash_profile"
printf '# existing Bash login configuration\n' >"$home_bash_profile/.bash_profile"
if ! run_install_with_shell_startup "$home_bash_profile" "$common_path" "$valid_archive" "$archive_sha256" '' /bin/bash; then
    cat "$home_bash_profile/install.stderr" >&2
    fail 'existing bash profile installation failed'
fi
assert_contains "$home_bash_profile/.bash_profile" '# existing Bash login configuration'
assert_contains "$home_bash_profile/.bash_profile" '# Scriptella installer PATH (Scriptella bin; do not edit)'

home_zshrc="$work_dir/zshrc-home"
mkdir -p "$home_zshrc"
printf '# existing zsh configuration\n' >"$home_zshrc/.zshrc"
if ! run_install_with_shell_startup "$home_zshrc" "$common_path" "$valid_archive" "$archive_sha256" '' /bin/zsh; then
    cat "$home_zshrc/install.stderr" >&2
    fail 'zsh startup-file installation failed'
fi
assert_contains "$home_zshrc/.zshrc" '# existing zsh configuration'
assert_contains "$home_zshrc/.zshrc" '# Scriptella installer PATH (Scriptella bin; do not edit)'
assert_not_exists "$home_zshrc/.zprofile"

home_zsh_manual="$work_dir/zsh-manual-home"
if ! run_install_with_shell_startup "$home_zsh_manual" "$common_path" "$valid_archive" "$archive_sha256" '' /bin/zsh; then
    cat "$home_zsh_manual/install.stderr" >&2
    fail 'zsh manual PATH installation failed'
fi
assert_not_exists "$home_zsh_manual/.zshrc"
assert_not_exists "$home_zsh_manual/.zprofile"
assert_contains "$home_zsh_manual/install.stdout" 'PATH was not changed automatically.'
assert_contains "$home_zsh_manual/install.stdout" 'export PATH="$HOME/.local/scriptella/bin:$PATH"'

if [ "$#" -gt 1 ]; then
    fail 'expected zero or one real distribution archive argument'
fi
if [ "$#" -eq 1 ]; then
    real_archive=$1
    assert_file "$real_archive"
    real_sha256=$(sha256_file "$real_archive")
    real_java=$(command -v java 2>/dev/null || true)
    [ -n "$real_java" ] || fail 'Java is required for the real distribution smoke test'
    real_home="$work_dir/real-distribution-home"
    if ! run_install "$real_home" "$common_path" "$real_archive" "$real_sha256" "$real_java"; then
        cat "$real_home/install.stderr" >&2
        fail 'real distribution installation failed'
    fi
    assert_file "$real_home/.local/scriptella/scriptella.jar"
    assert_file "$real_home/.local/scriptella/lib/rhino-engine.jar"
    if ! PATH="$real_home/.local/scriptella/bin:$common_path" \
        "$real_home/.local/scriptella/bin/scriptella.sh" --version \
        >"$real_home/version.out" 2>"$real_home/version.err"; then
        cat "$real_home/version.err" >&2
        fail 'installed real distribution launcher failed'
    fi
    assert_contains "$real_home/version.out" 'Scriptella'
    cat >"$real_home/simple.etl.xml" <<'EOF'
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="jexl" driver="jexl"/>
    <script connection-id="jexl">answer = 6 * 7;</script>
</etl>
EOF
    if ! PATH="$real_home/.local/scriptella/bin:$common_path" \
        "$real_home/.local/scriptella/bin/scriptella.sh" "$real_home/simple.etl.xml" \
        >"$real_home/simple.out" 2>"$real_home/simple.err"; then
        cat "$real_home/simple.err" >&2
        fail 'installed real distribution failed to execute an ETL'
    fi
fi

echo 'installer integration tests passed'
