#!/usr/bin/env sh
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
GRADLE_HOME="${APP_HOME}/.gradle-wrapper"
GRADLE_VERSION="8.2"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -f "${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin/gradle" ]; then
    mkdir -p "${GRADLE_HOME}"
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    curl -sL "${GRADLE_URL}" -o "${GRADLE_HOME}/gradle.zip"
    unzip -q "${GRADLE_HOME}/gradle.zip" -d "${GRADLE_HOME}"
    rm "${GRADLE_HOME}/gradle.zip"
fi

exec "${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
