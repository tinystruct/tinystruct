package org.tinystruct.data.tools;

import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * Application code overwhelmingly re-executes the same handful of parameterized SQL
     * shapes (the same {@code SELECT ... WHERE id=?} text, over and over, with only the
     * bound parameters changing). Since the verdict for a given SQL string is a pure
     * function of that string, it's cached here instead of re-running all 13 patterns on
     * every call. The cache is capped so an application that generates large numbers of
     * distinct ad-hoc SQL strings can't grow it without bound; once full, verdicts are
     * simply computed without being cached, so correctness never depends on cache size.
     */
    private static final int MAX_CACHE_SIZE = 512;
    private static final ConcurrentHashMap<String, String> VERDICT_CACHE = new ConcurrentHashMap<>();

    public static void checkForUnsafeSQL(String sql) throws SQLInjectionException {
        String detectedPatterns = VERDICT_CACHE.get(sql);
        if (detectedPatterns == null) {
            detectedPatterns = evaluate(sql);
            if (VERDICT_CACHE.size() < MAX_CACHE_SIZE) {
                VERDICT_CACHE.putIfAbsent(sql, detectedPatterns);
            }
        }

        if (!detectedPatterns.isEmpty()) {
            throw new SQLInjectionException(detectedPatterns);
        }
    }

    private static String evaluate(String sql) {
        StringBuilder detectedPatterns = new StringBuilder();

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                detectedPatterns.append("Potential SQL injection detected: ").append(matcher.group()).append("\n");
            }
        }

        return detectedPatterns.toString();
    }

}

