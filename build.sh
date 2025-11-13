#!/bin/sh
# Build script for tinystruct native image using POSIX sh
# Usage: ./build.sh

set -eu

# Enable debugging output
set -x

# Ensure GRAALVM_HOME is set
if [ -z "${GRAALVM_HOME:-}" ]; then
  echo "Error: GRAALVM_HOME is not set."
  echo "Please set it to your GraalVM installation directory."
  exit 1
fi

# Clean and build the project
./mvnw clean install

# Reload profile if exists
if [ -f "$HOME/.profile" ]; then
  . "$HOME/.profile"
elif [ -f "$HOME/.bash_profile" ]; then
  . "$HOME/.bash_profile"
fi

# Define variables
TARGET_JAR="./target/tinystruct-1.7.11.jar"
NATIVE_NAME="dispatcher-native"
MAIN_CLASS="org.tinystruct.system.Dispatcher"
CONFIG_DIR="./bin/.metadata"

# Verify target JAR exists
if [ ! -f "$TARGET_JAR" ]; then
  echo "Error: Target JAR not found at $TARGET_JAR"
  exit 1
fi

# Build native image
"$GRAALVM_HOME/bin/native-image" \
  -H:ConfigurationFileDirectories="$CONFIG_DIR" \
  --no-fallback \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  -cp "$TARGET_JAR" \
  -H:Name="$NATIVE_NAME" \
  -H:Class="$MAIN_CLASS" \
  -H:+ReportExceptionStackTraces \
  -H:+ReportUnsupportedElementsAtRuntime

echo "Native image '$NATIVE_NAME' successfully built!"
