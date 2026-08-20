#!/bin/sh
# Scriptella 1.4 user-local installer.
#
# This file is intentionally self-contained and is streamed by the documented
# curl command. Test-only SCRIPTELLA_INSTALLER_* overrides are documented below
# next to the values they replace.

set -eu
set -f

SCRIPTELLA_RELEASE_VERSION=1.4
SCRIPTELLA_RELEASE_TAG=scriptella-parent-1.4
SCRIPTELLA_ARCHIVE_NAME=scriptella-1.4.zip
SCRIPTELLA_ARCHIVE_URL=https://github.com/scriptella/scriptella-etl/releases/download/scriptella-parent-1.4/scriptella-1.4.zip
# Independently checked against the GitHub Release API asset digest for the
# immutable scriptella-parent-1.4/scriptella-1.4.zip asset.
SCRIPTELLA_ARCHIVE_SHA256=e96900158e0b2823b48954c33901a7867f8b9f82eb9510089d24f91a674e66ee

fail() {
    echo "Scriptella installer: $*" >&2
    exit 1
}

command_path() {
    command -v "$1" 2>/dev/null || true
}

path_exists() {
    [ -e "$1" ] || [ -L "$1" ]
}

require_command() {
    command_path "$1" >/dev/null || fail "required command not found: $1"
}

home=${SCRIPTELLA_INSTALLER_HOME-${HOME-}}
[ -n "$home" ] || fail 'HOME is not set'

# These overrides are intentionally narrow and are used by the offline test
# suite. A custom downloader accepts URL and destination as its two args.
archive_url=${SCRIPTELLA_INSTALLER_ARCHIVE_URL-$SCRIPTELLA_ARCHIVE_URL}
archive_sha256=${SCRIPTELLA_INSTALLER_SHA256-$SCRIPTELLA_ARCHIVE_SHA256}
installer_path=${SCRIPTELLA_INSTALLER_PATH-${PATH-}}
test_downloader=${SCRIPTELLA_INSTALLER_DOWNLOADER-}
test_java_override_set=${SCRIPTELLA_INSTALLER_JAVA_COMMAND+x}
test_java_override=${SCRIPTELLA_INSTALLER_JAVA_COMMAND-}
startup_override=${SCRIPTELLA_INSTALLER_STARTUP_FILE-}

case "$archive_sha256" in
    ''|*[!0123456789abcdefABCDEF]*) fail 'embedded archive checksum is not a hexadecimal SHA-256 value' ;;
esac
case "$archive_sha256" in
    ????????????????????????????????????????????????????????????????) ;;
    *) fail 'embedded archive checksum is not a 64-character SHA-256 value' ;;
esac

PATH=$installer_path
export PATH

require_command mktemp
require_command unzip
require_command mkdir
require_command mv
require_command rm
require_command grep
require_command sed
require_command awk
require_command cp

if [ -n "$test_downloader" ]; then
    [ -x "$test_downloader" ] || fail "test downloader is not executable: $test_downloader"
    downloader_kind=test
    downloader_command=$test_downloader
elif [ -n "$(command_path curl)" ]; then
    downloader_kind=curl
    downloader_command=$(command_path curl)
elif [ -n "$(command_path wget)" ]; then
    downloader_kind=wget
    downloader_command=$(command_path wget)
else
    fail 'required downloader not found (need curl or wget)'
fi

if [ -n "${SCRIPTELLA_INSTALLER_SHA256_COMMAND-}" ]; then
    checksum_command=$SCRIPTELLA_INSTALLER_SHA256_COMMAND
    [ -x "$checksum_command" ] || fail "checksum command is not executable: $checksum_command"
    checksum_kind=custom
elif [ -n "$(command_path sha256sum)" ]; then
    checksum_command=$(command_path sha256sum)
    checksum_kind=sha256sum
elif [ -n "$(command_path shasum)" ]; then
    checksum_command=$(command_path shasum)
    checksum_kind=shasum
else
    fail 'required SHA-256 command not found (need sha256sum or shasum)'
fi

tmp_parent=${TMPDIR-/tmp}
work_dir=$(mktemp -d "$tmp_parent/scriptella-installer.XXXXXX") \
    || fail 'unable to create a temporary staging directory'
backup_root=
transaction_active=0
startup_changed=0
startup_had_file=0

cleanup() {
    cleanup_status=0
    if [ "$transaction_active" = 1 ]; then
        set +e
        if path_exists "$install_dir"; then
            rm -rf "$install_dir" || cleanup_status=1
        fi
        if [ "$old_install" = 1 ] && path_exists "$backup_root/installation"; then
            mv "$backup_root/installation" "$install_dir" || cleanup_status=1
        fi
        if [ "$startup_changed" = 1 ]; then
            if [ "$startup_had_file" = 1 ]; then
                rm -f "$startup_file" || cleanup_status=1
                mv "$backup_root/startup" "$startup_file" || cleanup_status=1
            else
                rm -f "$startup_file" || cleanup_status=1
            fi
        fi
        transaction_active=0
        set -e
    fi
    if [ -n "$backup_root" ] && path_exists "$backup_root"; then
        rm -rf "$backup_root" || cleanup_status=1
    fi
    rm -rf "$work_dir" || cleanup_status=1
    if [ "$cleanup_status" != 0 ]; then
        echo 'Scriptella installer: cleanup or rollback failed; inspect the user-local directory' >&2
    fi
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

archive_file=$work_dir/$SCRIPTELLA_ARCHIVE_NAME
case "$downloader_kind" in
    test)
        "$downloader_command" "$archive_url" "$archive_file" \
            || fail 'archive download failed'
        ;;
    curl)
        "$downloader_command" -fL --silent --show-error "$archive_url" \
            --output "$archive_file" || fail 'archive download failed'
        ;;
    wget)
        "$downloader_command" --https-only --quiet --output-document="$archive_file" \
            "$archive_url" || fail 'archive download failed'
        ;;
esac
[ -s "$archive_file" ] || fail 'downloaded archive is empty'

case "$checksum_kind" in
    custom|sha256sum)
        actual_sha256=$($checksum_command "$archive_file" | awk '{print $1}')
        ;;
    shasum)
        actual_sha256=$($checksum_command -a 256 "$archive_file" | awk '{print $1}')
        ;;
esac
case "$actual_sha256" in
    "$archive_sha256") ;;
    *) fail 'archive SHA-256 checksum does not match the pinned release value' ;;
esac

archive_entries=$work_dir/archive-entries.txt
unzip -Z1 "$archive_file" >"$archive_entries" \
    || fail 'archive entry listing failed'
[ -s "$archive_entries" ] || fail 'archive contains no entries'

distribution_root=
entry_count=0
while IFS= read -r entry || [ -n "$entry" ]; do
    entry_count=$((entry_count + 1))
    case "$entry" in
        ''|/*|*/../*|../*|*/..|..|*\\*|*:*/*|*:* )
            fail "unsafe archive entry: $entry"
            ;;
    esac
    case "$entry" in
        */*) entry_root=${entry%%/*} ;;
        *) entry_root=$entry ;;
    esac
    [ -n "$entry_root" ] || fail 'archive contains an entry without a root'
    if [ -z "$distribution_root" ]; then
        distribution_root=$entry_root
    elif [ "$distribution_root" != "$entry_root" ]; then
        fail 'archive must contain one distribution root directory'
    fi
done <"$archive_entries"
[ "$entry_count" -gt 0 ] || fail 'archive contains no entries'
case "$distribution_root" in
    */*|.|..) fail 'archive root directory is invalid' ;;
esac

unzip -tqq "$archive_file" >/dev/null \
    || fail 'archive integrity check failed'
archive_metadata=$work_dir/archive-metadata.txt
unzip -Z -v "$archive_file" >"$archive_metadata" \
    || fail 'archive metadata listing failed'
if grep -E 'Unix file attributes \([^)]*\):[[:space:]]*l' \
        "$archive_metadata" >/dev/null 2>&1; then
    fail 'archive contains a symbolic-link entry'
fi

extract_dir=$work_dir/extracted
mkdir "$extract_dir"
unzip -q "$archive_file" -d "$extract_dir" \
    || fail 'archive extraction failed'

dist_dir=$extract_dir/$distribution_root
launcher=$dist_dir/bin/scriptella.sh
[ -d "$dist_dir" ] || fail 'archive distribution root is missing after extraction'
[ -d "$dist_dir/lib" ] || fail 'archive is missing lib/'
[ -f "$dist_dir/scriptella.jar" ] || fail 'archive is missing scriptella.jar'
[ -f "$launcher" ] || fail 'archive is missing bin/scriptella.sh'
[ -x "$launcher" ] || fail 'packaged bin/scriptella.sh is not executable'
grep -F 'lib/*.jar' "$launcher" >/dev/null \
    || fail 'packaged bin/scriptella.sh does not look like the Scriptella launcher'

install_dir=$home/.local/scriptella
install_parent=$home/.local
launcher_target=$install_dir/bin/scriptella.sh
path_dir=$install_dir/bin

path_contains_dir=0
old_ifs=$IFS
IFS=:
for path_entry in $installer_path; do
    if [ "$path_entry" = "$path_dir" ]; then
        path_contains_dir=1
        break
    fi
done
IFS=$old_ifs

if [ -n "$startup_override" ]; then
    startup_file=$startup_override
elif [ -n "${SHELL-}" ]; then
    case "$SHELL" in
        */zsh)
            if [ -e "$home/.zshrc" ] || [ -L "$home/.zshrc" ]; then
                startup_file=$home/.zshrc
            else
                startup_file=
            fi
            ;;
        */bash)
            if [ -e "$home/.bash_profile" ] || [ -L "$home/.bash_profile" ]; then
                startup_file=$home/.bash_profile
            elif [ -e "$home/.bash_login" ] || [ -L "$home/.bash_login" ]; then
                startup_file=$home/.bash_login
            elif [ -e "$home/.profile" ] || [ -L "$home/.profile" ]; then
                startup_file=$home/.profile
            else
                startup_file=
            fi
            ;;
        *)
            if [ -e "$home/.profile" ] || [ -L "$home/.profile" ]; then
                startup_file=$home/.profile
            else
                startup_file=
            fi
            ;;
    esac
else
    if [ -e "$home/.profile" ] || [ -L "$home/.profile" ]; then
        startup_file=$home/.profile
    else
        startup_file=
    fi
fi

add_path_stanza=0
startup_stanza_present=0
manual_path_setup=0
if [ "$path_contains_dir" = 0 ]; then
    if [ -n "$startup_file" ]; then
        add_path_stanza=1
    else
        manual_path_setup=1
    fi
fi

old_install=0
if path_exists "$install_dir"; then
    old_install=1
fi

umask 077
mkdir -p "$install_parent"
backup_root=$install_parent/.scriptella-installer-backup.$$
mkdir "$backup_root" || fail 'unable to create installation rollback state'
transaction_active=1

if [ "$old_install" = 1 ]; then
    mv "$install_dir" "$backup_root/installation" \
        || fail 'unable to save the existing installation'
fi
mv "$dist_dir" "$install_dir" || fail 'unable to place the new installation'

if [ "$add_path_stanza" = 1 ]; then
    if [ -e "$startup_file" ] || [ -L "$startup_file" ]; then
        [ -f "$startup_file" ] || fail "startup path is not a regular file: $startup_file"
        [ -w "$startup_file" ] || fail "startup file is not writable: $startup_file"
        if grep -F '.local/scriptella/bin' "$startup_file" >/dev/null 2>&1; then
            add_path_stanza=0
            startup_stanza_present=1
        else
            cp "$startup_file" "$backup_root/startup" \
                || fail 'unable to save the existing startup file'
            startup_had_file=1
        fi
    fi
    if [ "$add_path_stanza" = 1 ]; then
        startup_changed=1
        {
            printf '\n# Scriptella installer PATH (Scriptella bin; do not edit)\n'
            printf 'if [ -d "$HOME/.local/scriptella/bin" ]; then\n'
            printf '    case ":${PATH-}:" in\n'
            printf '        *:"$HOME/.local/scriptella/bin":*) ;;\n'
            printf '        *) PATH="$HOME/.local/scriptella/bin:${PATH-}"; export PATH ;;\n'
            printf '    esac\n'
            printf 'fi\n'
        } >>"$startup_file" || fail 'unable to update the startup file'
    fi
fi

transaction_active=0
rm -rf "$backup_root" || fail 'unable to remove installation rollback state'

java_cmd=
if [ "$test_java_override_set" = x ]; then
    java_cmd=$test_java_override
elif [ -n "${JAVACMD-}" ]; then
    java_cmd=$(command_path "$JAVACMD")
elif [ -n "${JAVA_HOME-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    java_cmd=$JAVA_HOME/bin/java
elif [ -n "$(command_path java)" ]; then
    java_cmd=$(command_path java)
fi

java_notice=
if [ -z "$java_cmd" ] || [ ! -x "$java_cmd" ]; then
    java_notice='Java 17 or newer was not found; install Java 17+ before running Scriptella.'
else
    java_version=$($java_cmd -version 2>&1 || true)
    java_value=$(printf '%s\n' "$java_version" \
        | sed -n 's/.*version "\([^"]*\)".*/\1/p' | sed -n '1p')
    case "$java_value" in
        1.*)
            java_major=${java_value#1.}
            java_major=${java_major%%.*}
            ;;
        '') java_major=0 ;;
        *)
            java_major=${java_value%%.*}
            ;;
    esac
    case "$java_major" in
        ''|*[!0-9]*) java_notice='Java 17 or newer could not be confirmed; install Java 17+ before running Scriptella.' ;;
        *)
            [ "$java_major" -ge 17 ] \
                || java_notice="Java $java_major was found; Scriptella requires Java 17 or newer."
            ;;
    esac
fi

echo "Scriptella $SCRIPTELLA_RELEASE_VERSION installed in $install_dir"
echo "Command: $launcher_target"
if [ -n "$java_notice" ]; then
    echo "Warning: $java_notice" >&2
fi
if [ "$path_contains_dir" = 1 ]; then
    echo "PATH already contains $path_dir"
elif [ "$startup_changed" = 1 ]; then
    echo "PATH updated in $startup_file; start a new shell or reload that file before using scriptella.sh."
elif [ "$startup_stanza_present" = 1 ]; then
    echo "PATH stanza already present in $startup_file; start a new shell or reload that file before using scriptella.sh."
elif [ "$manual_path_setup" = 1 ]; then
    echo 'PATH was not changed automatically.'
    echo 'Add this line to an appropriate shell startup file: export PATH="$HOME/.local/scriptella/bin:$PATH"'
else
    echo "Add $path_dir to PATH before using scriptella.sh."
fi
echo "Example: scriptella.sh path/to/file.etl.xml"
