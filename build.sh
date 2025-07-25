#!/usr/bin/env bash
set -ex

./mvnw clean install
source ~/.bash_profile

$GRAALVM_HOME/bin/native-image -H:ConfigurationFileDirectories=/Volumes/Untitled/development/tinystruct/bin/.metadata --no-fallback \
--initialize-at-run-time=org.slf4j.LoggerFactory \
--initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils \
--initialize-at-run-time=io.netty.util.AbstractReferenceCounted \
--trace-class-initialization=org.slf4j.LoggerFactory \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-cp ./target/tinystruct-1.7.2.jar \
-H:Name=dispatcher-native \
-H:Class=org.tinystruct.system.Dispatcher \
-H:+ReportExceptionStackTraces \
-Dio.netty.tryReflectionSetAccessible=true \
-H:+ReportUnsupportedElementsAtRuntime
