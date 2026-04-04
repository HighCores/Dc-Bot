@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java is not installed. Please install Java 17+
    exit /b 1
)

if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile %WRAPPER_JAR%}"
)

java -jar %WRAPPER_JAR% %*
