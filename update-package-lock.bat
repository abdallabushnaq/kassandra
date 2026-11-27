@echo off
rem
rem Copyright (C) 2025-2026 Abdalla Bushnaq
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
rem
rem Regenerate the lockfile for the Linux x64 glibc CI environment.
setlocal

set "ROOT=%~dp0"
set "NODE=%USERPROFILE%\.vaadin\node\node.exe"
set "NPM_CLI=%USERPROFILE%\.vaadin\node\node_modules\npm\bin\npm-cli.js"
set "LOCKFILE=%ROOT%package-lock.json"
set "BACKUP=%TEMP%\kassandra-package-lock-%RANDOM%.json"

if not exist "%NODE%" (
    echo Vaadin-managed Node.js was not found. Run a Maven build first.
    exit /b 1
)
if not exist "%NPM_CLI%" (
    echo Vaadin-managed npm was not found. Run a Maven build first.
    exit /b 1
)

copy /y "%LOCKFILE%" "%BACKUP%" >nul
if errorlevel 1 (
    echo Could not back up package-lock.json.
    exit /b 1
)

del "%LOCKFILE%"
if errorlevel 1 goto restore_lockfile

pushd "%ROOT%"
"%NODE%" "%NPM_CLI%" install --package-lock-only --include=optional --os=linux --cpu=x64 --libc=glibc
set "NPM_EXIT=%ERRORLEVEL%"
popd
if not "%NPM_EXIT%"=="0" goto restore_lockfile

del "%BACKUP%"
echo package-lock.json regenerated. Review and commit the diff.
exit /b 0

:restore_lockfile
copy /y "%BACKUP%" "%LOCKFILE%" >nul
del "%BACKUP%"
echo Lockfile generation failed; the original package-lock.json was restored.
exit /b 1
