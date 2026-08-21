@echo off
REM Maintainer-only local image build. Public demo users should run
REM "docker compose -f docker-compose.demo.yml up" instead.
REM Copyright (C) 2025-2026 Abdalla Bushnaq
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM       http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM

setlocal

if "%PACKAGES_TOKEN%"=="" (
    echo PACKAGES_TOKEN must contain a GitHub Packages token.
    exit /b 1
)

if "%GITHUB_ACTOR%"=="" set "GITHUB_ACTOR=abdallabushnaq"
set "SETTINGS_FILE=%TEMP%\kassandra-maven-settings-%RANDOM%%RANDOM%.xml"

(
    echo ^<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"^>
    echo   ^<servers^>
    echo     ^<server^>
    echo       ^<id^>github^</id^>
    echo       ^<username^>%GITHUB_ACTOR%^</username^>
    echo       ^<password^>%PACKAGES_TOKEN%^</password^>
    echo     ^</server^>
    echo   ^</servers^>
    echo ^</settings^>
) > "%SETTINGS_FILE%"

docker build --secret id=maven_settings,src="%SETTINGS_FILE%" --tag kassandra-demo:latest .
set "BUILD_RESULT=%ERRORLEVEL%"
del "%SETTINGS_FILE%"
exit /b %BUILD_RESULT%
