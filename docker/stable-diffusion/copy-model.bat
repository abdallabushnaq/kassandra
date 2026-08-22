rem @echo off
setlocal
rem cd /d "%~dp0\..\.."
cd /d "%~dp0"

call :sync "models\Stable-diffusion" "sd-models-stable-diffusion"
if errorlevel 1 exit /b 1

call :sync "models\Lora" "sd-models-lora"
if errorlevel 1 exit /b 1

echo.
echo Model volumes synchronized.
exit /b 0

:sync
if not exist "%~1\" (
    echo Source directory "%~1" does not exist.
    exit /b 1
)

echo Synchronizing "%~1" to Docker volume "%~2"...
docker run --rm -v "%CD%\%~1:/source:ro" -v "%~2:/volume" alpine:3.21 sh -c "rm -rf /volume/* /volume/.[!.]* /volume/..?* && cp -a /source/. /volume/"
exit /b %errorlevel%
