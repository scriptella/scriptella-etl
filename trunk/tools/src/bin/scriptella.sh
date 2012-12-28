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

# Scriptella launcher script for Linux.

BIN_DIR=`dirname $0`
CUR_DIR=`pwd`
if [ "x$SCRIPTELLA_HOME" = "x" ]; then
    SCRIPTELLA_HOME=`cd $BIN_DIR/..; pwd`       # goes one level up
fi

_SCRIPTELLA_CP=""
for _arg in $SCRIPTELLA_HOME/lib/*.jar; do
	_SCRIPTELLA_CP=$_SCRIPTELLA_CP:$_arg
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

$JAVACMD -classpath $_SCRIPTELLA_CP scriptella.tools.launcher.EtlLauncher "$@"

