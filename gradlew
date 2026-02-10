#!/bin/bash
# Gradle wrapper script for Unix

# Determine the project directory
WRAPPER_DIR=$(cd "$(dirname "$0")" && pwd)
APP_HOME=$(cd "$WRAPPER_DIR/.." && pwd)

# Download gradle wrapper jar if not present
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper..."
    GRADLE_DIST_URL=$(grep distributionUrl "$WRAPPER_PROPS" | cut -d'=' -f2 | sed 's/\\//g')
    curl -L -o /tmp/gradle.zip "$GRADLE_DIST_URL"
    unzip -q /tmp/gradle.zip -d /tmp
    GRADLE_DIR=$(ls -d /tmp/gradle-* | head -1)
    cp "$GRADLE_DIR/lib/plugins/gradle-wrapper-"*.jar "$WRAPPER_JAR"
    rm -rf /tmp/gradle.zip /tmp/gradle-*
fi

exec java -jar "$WRAPPER_JAR" "$@"
