#!/usr/bin/env bash
set -ex

./mvnw clean install
source ~/.bash_profile

$GRAALVM_HOME/bin/native-image -H:ConfigurationFileDirectories=/Volumes/Untitled/development/tinystruct/bin/.metadata --no-fallback \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-cp ./target/tinystruct-1.7.7.jar \
-H:Name=dispatcher-native \
-H:Class=org.tinystruct.system.Dispatcher \
-H:+ReportExceptionStackTraces \
-H:+ReportUnsupportedElementsAtRuntime
