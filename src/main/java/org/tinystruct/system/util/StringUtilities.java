/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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

/**
 * Utility class for string manipulation operations.
 * Provides methods for common string operations like escaping, HTML handling,
 * padding, case conversion, and more.
 */
public class StringUtilities implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F'};
    private final char[] HTML = "<>'\"".toCharArray();
    private String raw;

    /**
     * Constructs a StringUtilities instance with the specified string.
     *
     * @param raw The string to work with
     */
    public StringUtilities(String raw) {
        this.raw = raw;
    }

    /**
     * Constructs a StringUtilities instance from a byte array.
     *
     * @param bytes The byte array to convert to a string
     */
    public StringUtilities(byte[] bytes) {
        this.raw = bytes != null ? new String(bytes) : null;
    }

    /**
     * Ensures a URL has a proper protocol prefix.
     * Adds "http://" prefix if the URL doesn't start with any of the supported protocols.
     *
     * @param url The URL to process
     * @return The URL with proper protocol prefix
     */
    public static String getURL(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        String lowerUrl = url.toLowerCase();
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://") &&
            !lowerUrl.startsWith("mms://") && !lowerUrl.startsWith("rtsp://")) {
            return "http://" + url;
        }
        return url;
    }

    /**
     * Checks if a URL is absolute (starts with a protocol).
     *
     * @param url The URL to check
     * @return true if the URL is absolute, false otherwise
     */
    public static boolean isAbsoluteURL(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") ||
               lowerUrl.startsWith("mms://") || lowerUrl.startsWith("rtsp://");
    }

    /**
     * Pads a string on the left with a specified character to reach the desired length.
     *
     * @param raw The string to pad
     * @param len The desired length
     * @param replacement The character to use for padding
     * @return The padded string
     */
    public static String leftPadding(String raw, int len, char replacement) {
        if (raw == null) {
            return String.format("%" + len + "s", "").replace(' ', replacement);
        }
        return String.format("%" + len + "s", raw).replace(' ', replacement);
    }

    /**
     * Pads a string on the right with a specified character to reach the desired length.
     *
     * @param raw The string to pad
     * @param len The desired length
     * @param replacement The character to use for padding
     * @return The padded string
     */
    public static String rightPadding(String raw, int len, char replacement) {
        if (raw == null) {
            return String.format("%-" + len + "s", "").replace(' ', replacement);
        }
        return String.format("%-" + len + "s", raw).replace(' ', replacement);
    }

    /**
     * Converts special characters to HTML entities.
     * Specifically handles: &, <, >, ', and ".
     *
     * @param value The string to convert
     * @return The string with HTML entities
     */
    public static String htmlSpecialChars(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // More efficient implementation using a single pass through the string
        StringBuilder result = new StringBuilder(value.length() * 2); // Allocate extra space for entities

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '\'':
                    result.append("&#39;");
                    break;
                case '"':
                    result.append("&#34;");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }

        return result.toString();
    }

    public static boolean safe(String string) {
        return new StringUtilities(string).safe();
    }

    /**
     * Truncates a string to a specified length, ensuring not to cut in the middle of an HTML tag.
     * Adds ellipsis to indicate truncation.
     *
     * @param string The string to truncate
     * @param length The maximum length
     * @return The truncated string
     */
    public static String leave(String string, int length) {
        if (string == null || string.trim().isEmpty()) {
            return "";
        }

        if (string.length() <= length) {
            return string;
        }

        // If we're in the middle of an HTML tag, continue until we find the closing '>'
        int endPos = length;
        if (string.substring(0, length).lastIndexOf('<') > string.substring(0, length).lastIndexOf('>')) {
            // We're inside a tag, find the closing bracket
            int closingBracket = string.indexOf('>', length);
            if (closingBracket != -1) {
                endPos = closingBracket + 1;
            }
        }

        return string.substring(0, endPos) + "...";
    }

    /**
     * Highlights occurrences of a substring within a string by wrapping them in bold tags.
     *
     * @param string The source string
     * @param substring The substring to highlight
     * @return The string with highlighted occurrences
     */
    public static String sign(String string, String substring) {
        if (string == null || substring == null || substring.isEmpty()) {
            return string != null ? string : "";
        }

        StringBuilder temp = new StringBuilder();
        String highlightStart = "<b>";
        String highlightEnd = "</b>";
        int index = 0;
        int position;
        int substringLength = substring.length();

        // Case-insensitive search
        String lowerString = string.toLowerCase();
        String lowerSubstring = substring.toLowerCase();

        while (true) {
            position = lowerString.indexOf(lowerSubstring, index);
            if (position == -1) {
                // No more occurrences, append the rest of the string
                temp.append(string.substring(index));
                break;
            }

            // Append text before the match
            temp.append(string, index, position);

            // Append the highlighted match (using the original case from the source string)
            temp.append(highlightStart)
                .append(string, position, position + substringLength)
                .append(highlightEnd);

            // Move past this occurrence
            index = position + substringLength;
        }

        return temp.toString();
    }

    /**
     * Replaces carriage returns in a string with a specified delimiter string.
     *
     * @param string The string to process
     * @param delimiter The delimiter to replace carriage returns with
     * @return The processed string
     */
    public static String linefeed(String string, String delimiter) {
        if (string == null) {
            return "";
        }
        return new StringUtilities(string).linefeed(delimiter);
    }

    /**
     * Replaces carriage returns in a string with a specified delimiter character.
     *
     * @param string The string to process
     * @param delimiter The delimiter character to replace carriage returns with
     * @return The processed string
     */
    public static String linefeed(String string, char delimiter) {
        if (string == null) {
            return "";
        }
        return new StringUtilities(string).linefeed(delimiter);
    }

    /**
     * Gets the parent directory path of a file.
     *
     * @param fileName The file name or path
     * @return The parent directory path
     */
    public static String getRealPath(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        java.io.File file = new java.io.File(fileName);
        java.io.File parent = file.getParentFile();
        return parent != null ? parent.getAbsolutePath() + java.io.File.separator : "";
    }

    /**
     * Gets the web application root directory from a file path.
     * Returns the portion of the path before "WEB-INF".
     *
     * @param fileName The file name or path
     * @return The web root directory path or empty string if not found
     */
    public static String getWebRoot(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        String path = new java.io.File(fileName).getAbsolutePath();
        if (!path.contains("WEB-INF")) {
            return "";
        }
        return path.substring(0, path.indexOf("WEB-INF"));
    }

    /**
     * Checks if a comma-separated string contains at least one non-empty value.
     *
     * @param parameters The comma-separated string to check
     * @return true if at least one non-empty value exists, false otherwise
     */
    public static boolean isValid(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return false;
        }

        // Split by comma and check if any non-empty values exist
        String[] parts = parameters.split(",");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                return true;
            }
        }

        return false;
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

    /**
     * Converts a string to a proper camel case Java class name.
     * For example: "user_profile" becomes "UserProfile"
     *
     * @param input The input string to convert
     * @return A properly formatted camel case Java class name
     */
    public static String convertToCamelCase(String input) {
        // Remove any non-alphanumeric characters except underscores
        String cleanName = input.replaceAll("[^a-zA-Z0-9_]", "");

        // Handle empty string case
        if (cleanName.isEmpty()) {
            return "GeneratedClass";
        }

        // Split by underscores
        String[] parts = cleanName.split("_");
        StringBuilder camelCase = new StringBuilder();

        // Process each part
        for (String part : parts) {
            if (part.isEmpty()) continue;

            // Capitalize first letter of each part
            camelCase.append(Character.toUpperCase(part.charAt(0)));

            // Add rest of the part in lowercase
            if (part.length() > 1) {
                camelCase.append(part.substring(1).toLowerCase());
            }
        }

        // If no camel case was generated (e.g., all parts were empty)
        if (camelCase.length() == 0) {
            return "GeneratedClass";
        }

        // Handle case where the name starts with a number
        if (Character.isDigit(camelCase.charAt(0))) {
            camelCase.insert(0, "Class");
        }

        return camelCase.toString();
    }

    /**
     * Finds a cookie by name in an array of cookies.
     *
     * @param cookies The array of cookies to search
     * @param name The name of the cookie to find
     * @return The found cookie or null if not found
     */
    public static Cookie getCookieByName(Cookie[] cookies, String name) {
        if (cookies == null || name == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equalsIgnoreCase(cookie.name())) {
                return cookie;
            }
        }

        return null;
    }

    /**
     * Escapes special characters in a string for use in various contexts.
     * Handles escape sequences for backslashes, tabs, newlines, etc.
     *
     * @param raw The string to escape
     * @return The escaped string
     */
    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }

        int len = raw.length();
        if (len == 0) {
            return "";
        }

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
                case ':':
                case '#':
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

    /**
     * Removes all spaces from a string.
     *
     * @param raw The string to process
     * @return The string with spaces removed
     */
    public static String nospace(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(" ", "");
    }

    /**
     * Removes all occurrences of a specific character from a string.
     *
     * @param raw The string to process
     * @param ch The character to remove
     * @return The string with the specified character removed
     */
    public static String remove(String raw, char ch) {
        if (raw == null) {
            return "";
        }

        return raw.replace(String.valueOf(ch), "");
    }

    /**
     * Gets the raw string value.
     *
     * @return The raw string
     */
    public String getString() {
        return raw != null ? raw : "";
    }

    /**
     * Replaces all occurrences of a character with a string.
     *
     * @param target The character to replace
     * @param replacement The string to replace with
     * @return The string with replacements
     */
    public String replace(char target, String replacement) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        if (replacement == null) {
            replacement = "";
        }

        return raw.replace(String.valueOf(target), replacement);
    }

    /**
     * Checks if the string is safe (doesn't contain HTML special characters).
     *
     * @return true if the string is safe, false otherwise
     */
    public boolean safe() {
        if (raw == null || raw.isEmpty()) {
            return true;
        }

        for (char c : HTML) {
            if (raw.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Truncates the string to a specified length, ensuring not to cut in the middle of an HTML tag.
     * Adds ellipsis to indicate truncation.
     *
     * @param length The maximum length
     * @return The truncated string
     */
    public String leave(int length) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        if (raw.length() <= length) {
            return raw;
        }

        // If we're in the middle of an HTML tag, continue until we find the closing '>'
        int endPos = length;
        if (raw.substring(0, length).lastIndexOf('<') > raw.substring(0, length).lastIndexOf('>')) {
            // We're inside a tag, find the closing bracket
            int closingBracket = raw.indexOf('>', length);
            if (closingBracket != -1) {
                endPos = closingBracket + 1;
            }
        }

        return raw.substring(0, endPos) + "...";
    }

    /**
     * Checks if the string is null or empty after trimming.
     *
     * @return true if the string is null or empty after trimming, false otherwise
     */
    public boolean isNull() {
        return raw == null || raw.trim().isEmpty();
    }

    /**
     * Checks if the string is not a number.
     *
     * @return true if the string is not a number, false if it is a number
     */
    public boolean isNaN() {
        if (raw == null || raw.isEmpty()) {
            return true;
        }

        try {
            Double.parseDouble(raw);
            return false; // It's a number
        } catch (NumberFormatException e) {
            return true; // It's not a number
        }
    }

    /**
     * Checks if the string contains only letters.
     *
     * @return true if the string contains only letters, false otherwise
     */
    public boolean isLetter() {
        if (raw == null || raw.isEmpty()) {
            return false;
        }

        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isLetter(raw.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the position of a substring in the string, starting from a specified index.
     * The search is case-insensitive.
     *
     * @param substring The substring to find
     * @param startIndex The index to start searching from
     * @return The position of the substring or -1 if not found
     */
    public int stringAt(String substring, int startIndex) {
        if (raw == null || substring == null) {
            return -1;
        }

        if (startIndex < 0) {
            startIndex = 0;
        }

        if (startIndex >= raw.length()) {
            return -1;
        }

        return raw.toLowerCase().indexOf(substring.toLowerCase(), startIndex);
    }

    /**
     * Replaces carriage returns (\r\n) in the string with a specified delimiter string.
     *
     * @param delimiter The delimiter string to replace carriage returns with
     * @return The processed string
     */
    public String linefeed(String delimiter) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        // Handle both \r\n and \n line endings
        return raw.replace("\r\n", delimiter).replace("\n", delimiter);
    }

    /**
     * Replaces carriage returns (\r) in the string with a specified delimiter character.
     *
     * @param delimiter The delimiter character to replace carriage returns with
     * @return The processed string
     */
    public String linefeed(char delimiter) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\r') {
                result.append(delimiter);
                // Skip the following \n if this is a \r\n sequence
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\n') {
                result.append(delimiter);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Truncates HTML content to a specified length while preserving HTML structure.
     * Converts line breaks to HTML <BR /> tags and adds ellipsis.
     *
     * @param length The maximum length of visible text (excluding HTML tags)
     * @return The truncated HTML content
     */
    public String hideHTML(int length) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        if (length <= 0) {
            return "...";
        }

        // Normalize line breaks
        String normalizedText = raw.replaceAll("<br\\s*/?>", "\n").replaceAll("<BR\\s*/?>", "\n");

        StringBuilder text = new StringBuilder();
        int visibleCharCount = 0;
        boolean insideTag = false;

        for (int i = 0; i < normalizedText.length(); i++) {
            char c = normalizedText.charAt(i);

            if (c == '<') {
                insideTag = true;
                text.append(c);
            } else if (c == '>') {
                insideTag = false;
                text.append(c);
            } else if (insideTag) {
                text.append(c);
            } else {
                // We're not inside a tag, so this is visible text
                if (visibleCharCount >= length) {
                    break;
                }

                if (c == '\n') {
                    // Check if we're at the end or if the next char isn't the start of a tag
                    if (i + 1 >= normalizedText.length() || normalizedText.charAt(i + 1) != '<') {
                        text.append("<BR />");
                    }
                } else {
                    text.append(c);
                    visibleCharCount++;
                }
            }
        }

        text.append("...");
        return text.toString();
    }

    /**
     * Removes trailing slashes from a string.
     *
     * @return The string without trailing slashes
     */
    public String removeTrailingSlash() {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String result = raw;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
