@echo off
rem Copyright 2006-2007 The Scriptella Project Team.
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem Scriptella launcher script for Windows 2000 or higher.

if "%OS%"=="Windows_NT" goto winnt;
echo Scriptella launcher works only under Windows 2000 or higher
exit /b 1;
:winnt
@setlocal ENABLEDELAYEDEXPANSION

rem ----- use the location of this script to infer $SCRIPTELLA_HOME -------
if "%SCRIPTELLA_HOME%" == "" set SCRIPTELLA_HOME=%~dp0\..

rem ----- set the current working dir as the CUR_DIR variable  ----
set CUR_DIR=%CD%

@cd /d %~dp0..\lib
set _SCRIPTELLA_CP=
@for %%i in (*.jar) do set _SCRIPTELLA_CP=!_SCRIPTELLA_CP!;%%~fi
@cd /d %cur_dir%

rem ---- define java cmd. Use JAVACMD to specify java.exe location and VM options
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java.exe

:noJavaHome
if "%JAVACMD%" == "" set JAVACMD=java.exe

rem ---- run scriptella
%JAVACMD% -cp %_SCRIPTELLA_CP% scriptella.tools.launcher.EtlLauncher %1 %2 %3 %4 %5 %6 %7 %8 %9
