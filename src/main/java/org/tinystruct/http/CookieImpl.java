package org.tinystruct.http;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CookieImpl implements Cookie {
    protected static final String ANCIENT_DATE;
    private static final String COOKIE_DATE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";
    protected static final ThreadLocal<DateFormat> COOKIE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> {
                DateFormat df =
                        new SimpleDateFormat(COOKIE_DATE_PATTERN, Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                return df;
            });
    private static final BitSet domainValid = new BitSet(128);

    static {
        ANCIENT_DATE = COOKIE_DATE_FORMAT.get().format(new Date(10000));
    }

    static {
        for (char c = '0'; c <= '9'; c++) {
            domainValid.set(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            domainValid.set(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            domainValid.set(c);
        }
        domainValid.set('.');
        domainValid.set('-');
    }

    private final String name;
    private String value;
    private boolean httpOnly;
    private boolean wrap;
    private String domain;
    private String path;
    private long maxAge;
    private boolean secure;

    public CookieImpl(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean wrap() {
        return this.wrap;
    }

    @Override
    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    @Override
    public String domain() {
        return this.domain;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public long maxAge() {
        return this.maxAge;
    }

    @Override
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    @Override
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Override
    public String toString() {
        // Can't use StringBuilder due to DateFormat
        StringBuffer header = new StringBuffer();

        // TODO: Name validation takes place in Cookie and cannot be configured
        //       per Context. Moving it to here would allow per Context config
        //       but delay validation until the header is generated. However,
        //       the spec requires an IllegalArgumentException on Cookie
        //       generation.
        header.append(this.name);
        header.append('=');
        String value = this.value;
        if (value != null && value.length() > 0) {
            validateCookieValue(value);
            header.append(value);
        }

        // RFC 6265 prefers Max-Age to Expires but... (see below)
        long maxAge = this.maxAge;
        if (maxAge > -1) {
            // Negative Max-Age is equivalent to no Max-Age
            header.append("; Max-Age=");
            header.append(maxAge);

            // Microsoft IE and Microsoft Edge don't understand Max-Age so send
            // expires as well. Without this, persistent cookies fail with those
            // browsers. See http://tomcat.markmail.org/thread/g6sipbofsjossacn

            // Wdy, DD-Mon-YY HH:MM:SS GMT ( Expires Netscape format )
            header.append("; Expires=");
            // To expire immediately we need to set the time in past
            if (maxAge == 0) {
                header.append(ANCIENT_DATE);
            } else {
                COOKIE_DATE_FORMAT.get().format(
                        new Date(System.currentTimeMillis() + maxAge * 1000L),
                        header,
                        new FieldPosition(0));
            }
        }

        String domain = this.domain;
        if (domain != null && domain.length() > 0) {
            validateDomain(domain);
            header.append("; Domain=");
            header.append(domain);
        }

        String path = this.path;
        if (path != null && path.length() > 0) {
            validatePath(path);
            header.append("; Path=");
            header.append(path);
        }

        if (this.secure) {
            header.append("; Secure");
        }

        if (this.httpOnly) {
            header.append("; HttpOnly");
        }

        header.append("; SameSite=");
        header.append("Strict");

        return header.toString();
    }

    private void validateCookieValue(String value) {
        int start = 0;
        int end = value.length();

        if (end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"') {
            start = 1;
            end--;
        }

        char[] chars = value.toCharArray();
        for (int i = start; i < end; i++) {
            char c = chars[i];
            if (c < 0x21 || c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f) {
                throw new IllegalArgumentException("Invalid Char value:" + Integer.toString(c));
            }
        }
    }

    private void validateDomain(String domain) {
        int i = 0;
        int prev = -1;
        int cur = -1;
        char[] chars = domain.toCharArray();
        while (i < chars.length) {
            prev = cur;
            cur = chars[i];
            if (!domainValid.get(cur)) {
                throw new IllegalArgumentException("Invalid Domain:" + domain);
            }
            // labels must start with a letter or number
            if ((prev == '.' || prev == -1) && (cur == '.' || cur == '-')) {
                throw new IllegalArgumentException("Invalid Domain:" + domain);

            }
            // labels must end with a letter or number
            if (prev == '-' && cur == '.') {
                throw new IllegalArgumentException("Invalid Domain:" + domain);

            }
            i++;
        }
        // domain must end with a label
        if (cur == '.' || cur == '-') {
            throw new IllegalArgumentException("Invalid Domain:" + domain);
        }
    }

    private void validatePath(String path) {
        char[] chars = path.toCharArray();

        for (char ch : chars) {
            if (ch < 0x20 || ch > 0x7E || ch == ';') {
                throw new IllegalArgumentException("Invalid Path:" + path);
            }
        }
    }
}

