#!/bin/sh
# Copyright 2006-2012 The Scriptella Project Team.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Scriptella launcher script for Unix-like systems.

if [ -z "$SCRIPTELLA_HOME" ]; then
    _SCRIPTELLA_LAUNCHER=$0
    _SCRIPTELLA_SYMLINK_DEPTH=0
    while [ -L "$_SCRIPTELLA_LAUNCHER" ]; do
        if [ "$_SCRIPTELLA_SYMLINK_DEPTH" -ge 40 ]; then
            echo "Scriptella launcher: too many symbolic links while resolving $0" >&2
            exit 1
        fi
        _SCRIPTELLA_SYMLINK_DEPTH=$((_SCRIPTELLA_SYMLINK_DEPTH + 1))
        _SCRIPTELLA_LAUNCHER_DIR=$(CDPATH= cd -- "$(dirname -- "$_SCRIPTELLA_LAUNCHER")" && pwd)
        _SCRIPTELLA_LAUNCHER_TARGET=$(readlink "$_SCRIPTELLA_LAUNCHER")
        case "$_SCRIPTELLA_LAUNCHER_TARGET" in
            /*) _SCRIPTELLA_LAUNCHER=$_SCRIPTELLA_LAUNCHER_TARGET ;;
            *) _SCRIPTELLA_LAUNCHER=$_SCRIPTELLA_LAUNCHER_DIR/$_SCRIPTELLA_LAUNCHER_TARGET ;;
        esac
    done
    SCRIPTELLA_HOME=$(CDPATH= cd -- "$(dirname -- "$_SCRIPTELLA_LAUNCHER")/.." && pwd)
fi

if [ -z "$SCRIPTELLA_JAVA_OPTS" ]; then
    _SCRIPTELLA_JAVA_OPTS="$JAVA_OPTS"
else
    _SCRIPTELLA_JAVA_OPTS="$SCRIPTELLA_JAVA_OPTS"
fi

_SCRIPTELLA_CP=""
for _arg in "$SCRIPTELLA_HOME"/lib/*.jar; do
    [ -f "$_arg" ] || continue
    if [ -z "$_SCRIPTELLA_CP" ]; then
        _SCRIPTELLA_CP="$_arg"
    else
        _SCRIPTELLA_CP="$_SCRIPTELLA_CP:$_arg"
    fi
done


# Setup the Java Virtual Machine
if [ -n "$JAVA_HOME" ]; then                    # true if string's length is not zero
    if [ -x "$JAVA_HOME/bin/java" ] ; then      # true if file exists and can be executed
      JAVACMD="$JAVA_HOME/bin/java"
    fi
fi
if [ -z "$JAVACMD" ]; then                      # true if string's length is zero
    JAVACMD="java"
fi

exec "$JAVACMD" $_SCRIPTELLA_JAVA_OPTS -classpath "$_SCRIPTELLA_CP" \
    scriptella.tools.launcher.EtlLauncher "$@"
