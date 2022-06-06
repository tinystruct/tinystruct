/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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
package org.tinystruct.system.util;

import org.tinystruct.http.Cookie;

import java.util.Iterator;

public class StringUtilities implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F'};
    private final char[] HTML = "<>'\"".toCharArray();
    private String raw;

    public StringUtilities(String raw) {
        this.raw = raw;
    }

    public StringUtilities(byte[] bytes) {
        this.raw = new String(bytes);
    }

    public static String getURL(String url) {
        if (url.toLowerCase().indexOf("http://") == 0 && url.toLowerCase().indexOf("mms://") == 0
                && url.toLowerCase().indexOf("rtsp://") == 0 && url.toLowerCase().indexOf("https://") == 0)
            url = "http://" + url;
        return url;
    }

    public static boolean isAbsoluteURL(String url) {
        return url.toLowerCase().indexOf("http://") != 0 || url.toLowerCase().indexOf("mms://") != 0
                || url.toLowerCase().indexOf("rtsp://") != 0 || url.toLowerCase().indexOf("https://") != 0;
    }

    public static String leftPadding(String raw, int len, char replacement) {
        return String.format("%" + len + "s", raw).replace(' ', replacement);
    }

    public static String rightPadding(String raw, int len, char replacement) {
        return String.format("%-" + len + "s", raw).replace(' ', replacement);
    }

    public static String htmlSpecialChars(String value) {
        String[] unicode = new String[]{"&amp;", "&lt;", "&gt;", "&#39;", "&#34;"};

        char[] html_chars = "&<>'\"".toCharArray();

        for (int i = 0; i < html_chars.length; i++) {
            value = value.replaceAll(String.valueOf(html_chars[i]), unicode[i]);
        }

        return value;
    }

    public static boolean safe(String string) {
        return new StringUtilities(string).safe();
    }

    public static String leave(String string, int length) {
        if (string.trim().length() == 0)
            return "";
        if (string.length() <= length)
            return string;
        char sub;

        do {
            sub = string.charAt(length++);
        } while (string.indexOf('>') != -1 && sub != '>');

        return string.substring(0, length) + "...";
    }

    public static String sign(String string, String substring) {
        StringBuilder temp = new StringBuilder();
        String color_start = "<b>", color_end = "</b>";
        int index = 0, position, length = substring.length();
        StringUtilities s = new StringUtilities(string);
        while (true) {
            position = s.stringAt(substring, index);
            if (position == -1) {
                temp.append(string.substring(index));
                break;
            }
            temp.append(string, index, position);

            index = position + length;
            temp.append(color_start).append(string, position, index).append(color_end);
        }
        return temp.toString();
    }

    public static String linefeed(String string, String delimiter) {
        return new StringUtilities(string).linefeed(delimiter);
    }

    public static String linefeed(String string, char delimiter) {
        return new StringUtilities(string).linefeed(delimiter);
    }

    public static String getRealPath(String FileName) {
        String path = new java.io.File(FileName).getAbsolutePath();
        int length = path.length();

        return path.substring(0, length - FileName.length());
    }

    public static String getWebRoot(String FileName) {
        String path = new java.io.File(FileName).getAbsolutePath();
        if (!path.contains("WEB-INF"))
            return "";
        return path.substring(0, path.indexOf("WEB-INF"));
    }

    public static boolean isValid(String parameters) {
        boolean isValid = false;
        if (parameters != null) {
            StringBuilder s2 = new StringBuilder();
            for (int i = 0; i < parameters.length(); i++) {
                if (parameters.charAt(i) == ',' || i == parameters.length() - 1) {
                    if (i == parameters.length() - 1 && parameters.charAt(i) != ',')
                        s2.append(parameters.charAt(i));
                    isValid = s2.toString().trim().length() > 0;
                    s2 = new StringBuilder();
                } else {
                    s2.append(parameters.charAt(i));
                }
            }
        }
        return isValid;
    }

    public static boolean isValid(javax.servlet.http.HttpServletRequest request, String parameters) {
        if (parameters != null) {
            StringBuilder s2 = new StringBuilder();
            for (int i = 0; i < parameters.length(); i++) {
                if (parameters.charAt(i) == ',' || i == parameters.length() - 1) {
                    if (i == parameters.length() - 1 && parameters.charAt(i) != ',')
                        s2.append(parameters.charAt(i));
                    if (request.getParameter(s2.toString()) == null || request.getParameter(s2.toString()).trim().length() <= 0)
                        return false;

                    s2 = new StringBuilder();
                } else {
                    s2.append(parameters.charAt(i));
                }
            }
        }
        return true;
    }

    public static String setCharToLower(String s, int index) {
        char[] charArray = s.toCharArray();

        for (int i = 0; i < charArray.length; i++) {
            if (i == index)
                charArray[i] = Character.toLowerCase(charArray[i]);
        }

        return String.valueOf(charArray);
    }

    public static String setCharToUpper(String s, int index) {
        char[] charArray = s.toCharArray();

        for (int i = 0; i < charArray.length; i++) {
            if (i == index)
                charArray[i] = Character.toUpperCase(charArray[i]);
        }

        return String.valueOf(charArray);
    }

    public static String setCharToUpper(String s, char afterChar) {
        char[] charArray = s.toCharArray();
        int i = 0;

        while (i < charArray.length) {
            if (charArray[i++] == afterChar) {
                if (i >= charArray.length)
                    break;
                charArray[i] = Character.toUpperCase(charArray[i]);
            }
        }

        return String.valueOf(charArray);
    }

    public static Cookie getCookieByName(Cookie[] cookies, String name) {
        int i = 0;
        if (cookies != null)
            while (i < cookies.length) {
                if (cookies[i].name().equalsIgnoreCase(name))
                    return cookies[i];
                i++;
            }

        return null;
    }

    @Deprecated
    public static String implode(String separator, Iterable<String> iterator) {
        Iterator<String> iter = iterator.iterator();
        StringBuilder value = new StringBuilder(iter.hasNext() ? iter.next() : "");
        while (iter.hasNext())
            value.append(separator).append(iter.next());

        return value.toString();
    }

    public static String escape(String raw) {
        int len = raw.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }

        StringBuilder buffer = new StringBuilder(bufLen);
        char c;
        for (int x = 0; x < len; x++) {
            c = raw.charAt(x);
            // avoids the special chars
            if ((c >= 60) && (c < 127)) {
                if (c == '\\') {
                    buffer.append('\\').append('\\');
                    continue;
                }
                buffer.append(c);

                continue;
            }

            switch (c) {
                case ' ':
                    if (x == 0)
                        buffer.append('\\');
                    buffer.append(' ');
                    break;
                case '\t':
                    buffer.append('\\').append('t');
                    break;
                case '\n':
                    buffer.append('\\').append('n');
                    break;
                case '\r':
                    buffer.append('\\').append('r');
                    break;
                case '\f':
                    buffer.append('\\').append('f');
                    break;
                // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    buffer.append(c);
                    break;
                case '"':
                    buffer.append("\\\"");
                    break;
                default:
                    if ((c < 0x0020) || (c > 0x007e)) {
                        buffer.append('\\');
                        buffer.append('u');
                        buffer.append(toHex((c >> 12) & 0xF));
                        buffer.append(toHex((c >> 8) & 0xF));
                        buffer.append(toHex((c >> 4) & 0xF));
                        buffer.append(toHex(c & 0xF));
                    } else {
                        buffer.append(c);
                    }
            }
        }

        return buffer.toString();
    }

    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    public String getString() {
        return raw;
    }

    public static String nospace(String raw) {
        return remove(raw, (char) 32);
    }

    public static String remove(String raw, char ch) {
        StringBuilder buffer = new StringBuilder();
        int position = 0;
        char currentChar;

        while (position < raw.length()) {
            currentChar = raw.charAt(position++);
            if (currentChar != ch)
                buffer.append(currentChar);
        }
        return buffer.toString();
    }

    public String replace(char res, String des) {
        if (raw == null) {
            return "";
        }
        char[] temp = raw.toCharArray();
        StringBuilder buffer = new StringBuilder();
        for (char c : temp) {
            if (c == res)
                buffer.append(des);
            else
                buffer.append(c);
        }
        return buffer.toString();
    }

    public boolean safe() {
        for (char c : HTML)
            if (raw.indexOf(c) != -1)
                return false;
        return true;
    }

    public String leave(int length) {
        if (isNull())
            return "";
        if (raw.length() <= length)
            return raw;
        char sub;

        do {
            sub = raw.charAt(length++);
        } while (raw.indexOf('>') != -1 && sub != '>');

        return raw.substring(0, length) + "...";
    }

    public boolean isNull() {
        return raw.trim().length() == 0;
    }

    public boolean isNaN() {
        char c;

        for (int i = 0; i < raw.length(); i++) {
            c = raw.charAt(i);
            if (c >= 48 && c <= 57)
                return true;
        }
        return false;
    }

    public boolean isLetter() {
        char c;

        for (int i = 0; i < raw.length(); i++) {
            c = raw.charAt(i);
            if ((c >= 65 && c <= 90) || (c >= 97 && c <= 122))
                return true;
        }
        return false;
    }

    public int stringAt(String substring, int index) {
        return raw.toLowerCase().indexOf(substring.toLowerCase(), index);
    }

    public String linefeed(String end) {
        StringBuilder htmlcode = new StringBuilder();
        int index = 0;
        while (true) {
            int position = raw.indexOf(0x0D, index);
            if (position == -1) {
                htmlcode.append(raw.substring(index));
                break;
            }
            htmlcode.append(raw, index, position);
            index = position + 2;
            htmlcode.append(end);
        }
        return htmlcode.toString();
    }

    public String linefeed(char delimiter) {
        StringBuilder htmlCode = new StringBuilder();

        char[] ch = raw.toCharArray();
        for (char c : ch) {
            if (c == 0x0D)
                htmlCode.append(delimiter);
            else
                htmlCode.append(c);
        }
        return htmlCode.toString();
    }

    public String hideHTML(int length) {
        if (isNull())
            return "";
        StringBuilder text = new StringBuilder();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<br>|<BR>|<br />|<br/><BR />|<BR/>");
        java.util.regex.Matcher matcher = pattern.matcher(raw);
        raw = matcher.replaceAll(String.valueOf('\n'));

        int flag = 0, k = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (k > length - 1)
                break;
            char now = raw.charAt(i);
            if (now == '<' && flag == 0) {
                flag = 1;
            } else if (now == '>' && flag == 1) {
                flag = 0;
            } else if (flag == 0) {
                if (now == '\n' && raw.charAt(i + 1) != '<') {
                    text.append("<BR />");
                } else {
                    text.append(now);
                    k++;
                }
            }
        }
        text.append("...");
        return text.toString();
    }

    public String removeTrailingSlash() {
        int i;
        while ((i = raw.lastIndexOf('/')) != -1 && i == (raw + "/").lastIndexOf("//")) {
            raw = raw.substring(0, i);
        }
        return raw;
    }
}
