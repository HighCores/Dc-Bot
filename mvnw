#!/bin/sh
set -e

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed. Please install Java 17+"
    exit 1
fi

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    if command -v curl &> /dev/null; then
        curl -sL -o "$WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget &> /dev/null; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "ERROR: curl or wget required to download Maven Wrapper"
        exit 1
    fi
fi

exec java -jar "$WRAPPER_JAR" "$@"
