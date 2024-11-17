package org.tinystruct.data.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLInjectionDetector {

    private static final Pattern[] SQL_INJECTION_PATTERNS = new Pattern[]{
            Pattern.compile("(?i)\\sOR\\s+1=1"),
            Pattern.compile("(?i)\\sOR\\s+'1'='1'"),
//            Pattern.compile("(?i)'.*?'"),
            Pattern.compile("(?i)--[^\r\n]*"),
            Pattern.compile(";"),
            Pattern.compile("(?i)\\bUNION\\b\\s+SELECT"),
            Pattern.compile("(?i)\\bSELECT\\b\\s+\\*\\s+\\bFROM\\b"),
            Pattern.compile("(?i)\\bSLEEP\\b\\s*\\(|\\bBENCHMARK\\b\\s*\\(|\\bWAITFOR\\b\\s+DELAY"),
            Pattern.compile("(?i)@@\\w+|\\bDATABASE\\b\\s*\\(|\\bUSER\\b\\s*\\("),
            Pattern.compile("(?i)\\b0x[0-9A-Fa-f]+\\b|\\b0b[01]+\\b"),
            Pattern.compile("(?i)\\bLIKE\\b\\s+['\"].*[%_].*['\"]"),
            Pattern.compile("(?i)\\bCAST\\b\\s*\\(|\\bCONVERT\\b\\s*\\("),
            Pattern.compile("(?i)\\bAND\\b\\s+['\"]?.*['\"]?\\b=\\b|\\bOR\\b\\s+['\"]?.*['\"]?\\b=\\b"),
            Pattern.compile("(?i)\\b(DROP|ALTER|CREATE|TRUNCATE|RENAME|INSERT|UPDATE|DELETE)\\b\\s+TABLE")
    };

    public static void checkForUnsafeSQL(String sql) throws SQLInjectionException {
        StringBuilder detectedPatterns = new StringBuilder();

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                detectedPatterns.append("Potential SQL injection detected: ").append(matcher.group()).append("\n");
            }
        }

        if (detectedPatterns.length() > 0) {
            throw new SQLInjectionException(detectedPatterns.toString());
        }
    }

}

