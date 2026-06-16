/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    // ANSI Escape codes for coloring console output
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_GREY = "\u001B[90m";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final boolean useColor;

    public LogFormatter() {
        this(true);
    }

    public LogFormatter(boolean useColor) {
        this.useColor = useColor;
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        // 1. Colorized level and timestamp
        if (useColor) {
            builder.append(getColorForLevel(record.getLevel()));
        }

        // Timestamp
        builder.append(dateFormat.format(new Date(record.getMillis())));
        builder.append(" [");
        builder.append(Thread.currentThread().getName());
        builder.append("] ");

        // Level
        builder.append(String.format("%-7s", record.getLevel().getLocalizedName()));

        // Resolve caller source info using StackWalker
        LogSourceInfo sourceInfo = getSourceInfo();
        if (sourceInfo != null) {
            String className = sourceInfo.className;
            if (className.startsWith("org.tinystruct.")) {
                className = className.substring("org.tinystruct.".length());
            }
            builder.append("[").append(className).append(".").append(sourceInfo.methodName);
            if (sourceInfo.fileName != null) {
                builder.append("(").append(sourceInfo.fileName).append(":").append(sourceInfo.lineNumber).append(")");
            }
            builder.append("]");
        } else {
            // Fallback to LogRecord's source if StackWalker didn't find anything
            if (record.getSourceClassName() != null) {
                String className = record.getSourceClassName();
                if (className.startsWith("org.tinystruct.")) {
                    className = className.substring("org.tinystruct.".length());
                }
                builder.append("[").append(className);
                if (record.getSourceMethodName() != null) {
                    builder.append(".").append(record.getSourceMethodName());
                }
                builder.append("]");
            } else {
                builder.append("[").append(record.getLoggerName()).append("]");
            }
        }

        builder.append(" - ");

        // Message
        builder.append(formatMessage(record));

        if (useColor) {
            builder.append(ANSI_RESET);
        }
        builder.append("\n");

        // Stack Trace of associated Exception/Throwable
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            builder.append(sw.toString());
        }

        return builder.toString();
    }

    private LogSourceInfo getSourceInfo() {
        try {
            return StackWalker.getInstance()
                    .walk(frames -> frames
                            .filter(f -> !f.getClassName().startsWith("java.util.logging.") &&
                                         !f.getClassName().equals("org.tinystruct.system.logging.LogFormatter") &&
                                         !f.getClassName().equals("org.tinystruct.system.logging.LoggerConfigurer"))
                            .findFirst()
                            .map(f -> new LogSourceInfo(f.getFileName(), f.getClassName(), f.getMethodName(), f.getLineNumber()))
                            .orElse(null));
        } catch (Throwable t) {
            return null; // Fallback silently on any error with StackWalker
        }
    }

    private String getColorForLevel(Level level) {
        if (level == Level.SEVERE) {
            return ANSI_RED;
        } else if (level == Level.WARNING) {
            return ANSI_YELLOW;
        } else if (level == Level.INFO) {
            return ANSI_GREEN;
        } else if (level == Level.CONFIG) {
            return ANSI_CYAN;
        } else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
            return ANSI_GREY;
        }
        return ANSI_WHITE;
    }

    private static class LogSourceInfo {
        final String fileName;
        final String className;
        final String methodName;
        final int lineNumber;

        LogSourceInfo(String fileName, String className, String methodName, int lineNumber) {
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
    }
}
