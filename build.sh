#!/usr/bin/env bash
set -ex

./mvnw clean install
source ~/.bash_profile
$JAVA_HOME/bin/native-image --no-fallback \
                        --initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils \
                        --initialize-at-run-time=io.netty.util.AbstractReferenceCounted \
                        --initialize-at-run-time=io.netty.channel.epoll.Epoll,io.netty.channel.epoll.EpollEventLoop,io.netty.channel.epoll.Native,io.netty.channel.epoll.EpollEventArray,io.netty.channel.DefaultFileRegion,io.netty.channel.unix.Errors \
                        --initialize-at-build-time=org.slf4j.MDC \
                        --initialize-at-build-time=ch.qos.logback.classic.Level \
                        --initialize-at-build-time=ch.qos.logback.classic.Logger \
                        --initialize-at-build-time=ch.qos.logback.core.util.StatusPrinter \
                        --initialize-at-build-time=ch.qos.logback.core.status.StatusBase \
                        --initialize-at-build-time=ch.qos.logback.core.status.InfoStatus \
                        --initialize-at-build-time=ch.qos.logback.core.spi.AppenderAttachableImpl \
                        --initialize-at-build-time=org.slf4j.LoggerFactory \
                        --initialize-at-build-time=ch.qos.logback.core.util.Loader \
                        --initialize-at-build-time=org.slf4j.impl.StaticLoggerBinder \
                        --initialize-at-build-time=ch.qos.logback.classic.spi.ThrowableProxy \
                        --initialize-at-build-time=ch.qos.logback.core.CoreConstants \
                        --report-unsupported-elements-at-runtime \
                        --add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core=ALL-UNNAMED \
                        --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-cp ./target/tinystruct-0.8.2-jar-with-dependencies.jar \
-H:Name=dispatcher-native \
-H:Class=org.tinystruct.system.Dispatcher \
-H:+ReportExceptionStackTraces \
-Dio.netty.tryReflectionSetAccessible=true \
-H:+ReportUnsupportedElementsAtRuntime
