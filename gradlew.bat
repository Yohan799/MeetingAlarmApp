@rem Gradle wrapper script for Windows

@if "%DEBUG%"=="" @echo off

set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%

set JAVA_EXE=java.exe

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo Downloading Gradle 8.5...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile '%TEMP%\gradle.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%TEMP%\gradle-extract' -Force"
    for /d %%i in (%TEMP%\gradle-extract\gradle-*) do (
        copy "%%i\lib\plugins\gradle-wrapper-*.jar" "%CLASSPATH%" >nul 2>&1
    )
    rd /s /q "%TEMP%\gradle-extract" 2>nul
    del "%TEMP%\gradle.zip" 2>nul
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
