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
package org.tinystruct.dom;

public class Escape {
    protected char[] value;
    protected int mappableLimit;
    protected boolean allowControlCharacters;
    protected boolean useCDATA;

    protected final char[] NUL = {'&', '#', 'x', '0', ';'};
    protected final char[] SOH = {'&', '#', 'x', '1', ';'};
    protected final char[] STX = {'&', '#', 'x', '2', ';'};
    protected final char[] ETX = {'&', '#', 'x', '3', ';'};
    protected final char[] EOT = {'&', '#', 'x', '4', ';'};
    protected final char[] ENQ = {'&', '#', 'x', '5', ';'};
    protected final char[] ACK = {'&', '#', 'x', '6', ';'};
    protected final char[] BEL = {'&', '#', 'x', '7', ';'};
    protected final char[] BS = {'&', '#', 'x', '8', ';'};
    protected final char[] TAB = {'&', '#', 'x', '9', ';'};
    protected final char[] LF = {'&', '#', 'x', 'A', ';'};
    protected final char[] VT = {'&', '#', 'x', 'B', ';'};
    protected final char[] FF = {'&', '#', 'x', 'C', ';'};
    protected final char[] CR = {'&', '#', 'x', 'D', ';'};
    protected final char[] SO = {'&', '#', 'x', 'E', ';'};
    protected final char[] SI = {'&', '#', 'x', 'F', ';'};
    protected final char[] DLE = {'&', '#', 'x', '1', '0', ';'};
    protected final char[] DC1 = {'&', '#', 'x', '1', '1', ';'};
    protected final char[] DC2 = {'&', '#', 'x', '1', '2', ';'};
    protected final char[] DC3 = {'&', '#', 'x', '1', '3', ';'};
    protected final char[] DC4 = {'&', '#', 'x', '1', '4', ';'};
    protected final char[] NAK = {'&', '#', 'x', '1', '5', ';'};
    protected final char[] SYN = {'&', '#', 'x', '1', '6', ';'};
    protected final char[] ETB = {'&', '#', 'x', '1', '7', ';'};
    protected final char[] CAN = {'&', '#', 'x', '1', '8', ';'};
    protected final char[] EM = {'&', '#', 'x', '1', '9', ';'};
    protected final char[] SUB = {'&', '#', 'x', '1', 'A', ';'};
    protected final char[] ESC = {'&', '#', 'x', '1', 'B', ';'};
    protected final char[] FS = {'&', '#', 'x', '1', 'C', ';'};
    protected final char[] GS = {'&', '#', 'x', '1', 'D', ';'};
    protected final char[] RS = {'&', '#', 'x', '1', 'E', ';'};
    protected final char[] US = {'&', '#', 'x', '1', 'F', ';'};

    protected final char[][] CONTROL_CHARACTERS =
            new char[][]
                    {
                            NUL,
                            SOH,
                            STX,
                            ETX,
                            EOT,
                            ENQ,
                            ACK,
                            BEL,
                            BS,
                            TAB,
                            LF,
                            VT,
                            FF,
                            CR,
                            SO,
                            SI,
                            DLE,
                            DC1,
                            DC2,
                            DC3,
                            DC4,
                            NAK,
                            SYN,
                            ETB,
                            CAN,
                            EM,
                            SUB,
                            ESC,
                            FS,
                            GS,
                            RS,
                            US,
                    };

    protected final char[] AMP = {'&', 'a', 'm', 'p', ';'};
    protected final char[] LESS = {'&', 'l', 't', ';'};
    protected final char[] GREATER = {'&', 'g', 't', ';'};
    protected final char[] QUOTE = {'&', 'q', 'u', 'o', 't', ';'};
    protected final char[] LINE_FEED = System.getProperty("line.separator").toCharArray();

    public Escape() {
        value = new char[100];
    }

    public void setMappingLimit(int mappingLimit) {
        mappableLimit = mappingLimit;
    }

    public void setAllowControlCharacters(boolean allowControlCharacters) {
        this.allowControlCharacters = allowControlCharacters;
    }

    public void setUseCDATA(boolean useCDATA) {
        this.useCDATA = useCDATA;
    }

    /*
     *  Convert attribute values:
     *  & to &amp;
     *  < to &lt;
     *  " to &quot;
     *  \t to &#x9;
     *  \n to &#xA;
     *  \r to &#xD;
     */
    public String replaceInvalidCharacters(String input, char[] chars) {
        boolean changed = false;
        int inputLength = input.length();
        grow(inputLength);
        int outputPos = 0;
        int inputPos = 0;
        char ch = 0;
        while (inputLength-- > 0) {
            ch = input.charAt(inputPos++); // value[outputPos];
            switch (ch) {
                case 0x0:
                case 0x1:
                case 0x2:
                case 0x3:
                case 0x4:
                case 0x5:
                case 0x6:
                case 0x7:
                case 0x8:
                case 0xB:
                case 0xC:
                case 0xE:
                case 0xF:
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17:
                case 0x18:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F: {
                    if (!allowControlCharacters) {
                        outputPos = replaceChars(outputPos, chars, inputLength);
                        changed = true;
                    } else {
                        throw new RuntimeException("An invalid XML character (Unicode: 0x" + Integer.toHexString(ch) + ") was found in the element content:" + input);
                    }
                    break;
                }
                case '&': {
                    outputPos = replaceChars(outputPos, AMP, inputLength);
                    changed = true;
                    break;
                }
                case '\n': {
                    outputPos = replaceChars(outputPos, LF, inputLength);
                    changed = true;
                    break;
                }
                case '\r': {
                    outputPos = replaceChars(outputPos, CR, inputLength);
                    changed = true;
                    break;
                }
                case '\t': {
                    outputPos = replaceChars(outputPos, TAB, inputLength);
                    changed = true;
                    break;
                }
                default: {
                    // Normal (BMP) unicode code point. See if we know for a fact that the encoding supports it:
                    value[outputPos++] = ch;

                    break;
                }
            }
        }
        return changed ? new String(value, 0, outputPos) : input;
    }


    /*
     *  Convert attribute values:
     *  & to &amp;
     *  < to &lt;
     *  " to &quot;
     *  \t to &#x9;
     *  \n to &#xA;
     *  \r to &#xD;
     */
    public String convert(String input) {
        boolean changed = false;
        int inputLength = input.length();
        grow(inputLength);
        int outputPos = 0;
        int inputPos = 0;
        char ch = 0;
        while (inputLength-- > 0) {
            ch = input.charAt(inputPos++); // value[outputPos];
            switch (ch) {
                case 0x1:
                case 0x2:
                case 0x3:
                case 0x4:
                case 0x5:
                case 0x6:
                case 0x7:
                case 0x8:
                case 0xB:
                case 0xC:
                case 0xE:
                case 0xF:
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17:
                case 0x18:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F: {
                    if (allowControlCharacters) {
                        outputPos = replaceChars(outputPos, CONTROL_CHARACTERS[ch], inputLength);
                        changed = true;
                    } else {
                        throw new RuntimeException("An invalid XML character (Unicode: 0x" + Integer.toHexString(ch) + ") was found in the element content:" + input);
                    }
                    break;
                }
                case '&': {
                    outputPos = replaceChars(outputPos, AMP, inputLength);
                    changed = true;
                    break;
                }
                case '<': {
                    outputPos = replaceChars(outputPos, LESS, inputLength);
                    changed = true;
                    break;
                }
                case '"': {
                    outputPos = replaceChars(outputPos, QUOTE, inputLength);
                    changed = true;
                    break;
                }
                case '\n': {
                    outputPos = replaceChars(outputPos, LF, inputLength);
                    changed = true;
                    break;
                }
                case '\r': {
                    outputPos = replaceChars(outputPos, CR, inputLength);
                    changed = true;
                    break;
                }
                case '\t': {
                    outputPos = replaceChars(outputPos, TAB, inputLength);
                    changed = true;
                    break;
                }
                default: {
                    // Normal (BMP) unicode code point. See if we know for a fact that the encoding supports it:
                    if (ch <= mappableLimit) {
                        value[outputPos++] = ch;
                    } else {
                        // We not sure the encoding supports this code point, so we write it as a character entity reference.
                        outputPos = replaceChars(outputPos, ("&#x" + Integer.toHexString(ch) + ";").toCharArray(), inputLength);
                        changed = true;
                    }
                    break;
                }
            }
        }
        return changed ? new String(value, 0, outputPos) : input;
    }

    /*
     *  Convert element values:
     *  & to &amp;
     *  < to &lt;
     *  " to &quot;
     *  \n to line separator
     *  \r should be escaped to &xD;
     */
    public String convertText(String input) {
        boolean changed = false;
        boolean cdataCloseBracket = false;
        int inputLength = input.length();
        grow(inputLength);
        int outputPos = 0;
        int inputPos = 0;
        char ch;
        while (inputLength-- > 0) {
            ch = input.charAt(inputPos++); // value[outputPos];
            switch (ch) {
                case 0x1:
                case 0x2:
                case 0x3:
                case 0x4:
                case 0x5:
                case 0x6:
                case 0x7:
                case 0x8:
                case 0xB:
                case 0xC:
                case 0xE:
                case 0xF:
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17:
                case 0x18:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F: {
                    if (allowControlCharacters) {
                        outputPos = replaceChars(outputPos, CONTROL_CHARACTERS[ch], inputLength);
                        changed = true;
                    } else {
                        throw new RuntimeException("An invalid XML character (Unicode: 0x" + Integer.toHexString(ch) + ") was found in the element content:" + input);
                    }
                    break;
                }
                case '&': {
                    outputPos = replaceChars(outputPos, AMP, inputLength);
                    changed = true;
                    break;
                }
                case '<': {
                    outputPos = replaceChars(outputPos, LESS, inputLength);
                    changed = true;
                    break;
                }
                case '"': {
                    outputPos = replaceChars(outputPos, QUOTE, inputLength);
                    changed = true;
                    break;
                }
                case '\n': {
                    outputPos = replaceChars(outputPos, LINE_FEED, inputLength);
                    changed = true;
                    break;
                }
                case '\r': {
                    outputPos = replaceChars(outputPos, CR, inputLength);
                    changed = true;
                    break;
                }
                case '>': {
                    if (inputPos >= 3 && input.charAt(inputPos - 2) == ']' && input.charAt(inputPos - 3) == ']') {
                        outputPos = replaceChars(outputPos, GREATER, inputLength);
                        cdataCloseBracket = true;
                        changed = true;
                        break;
                    }

                    // continue with default processing
                }
                default: {
                    {
                        // Normal (BMP) unicode code point. See if we know for a fact that the encoding supports it:
                        if (ch <= mappableLimit) {
                            value[outputPos++] = ch;
                        } else {
                            // We not sure the encoding supports this code point, so we write it as a character entity reference.
                            outputPos = replaceChars(outputPos, ("&#x" + Integer.toHexString(ch) + ";").toCharArray(), inputLength);
                            changed = true;
                        }
                    }
                    break;
                }
            }
        }
        return changed ? !useCDATA || cdataCloseBracket ? new String(value, 0, outputPos) : "<![CDATA[" + input + "]]>" : input;
    }

    /*
     *  Convert:
     *  \n to line separator
     */
    public String convertLines(String input) {
        boolean changed = false;
        int inputLength = input.length();
        grow(inputLength);
        int outputPos = 0;
        int inputPos = 0;
        char ch;
        while (inputLength-- > 0) {
            ch = input.charAt(inputPos++);
            switch (ch) {
                case '\n': {
                    outputPos = replaceChars(outputPos, LINE_FEED, inputLength);
                    changed = true;
                    break;
                }
                default: {
                    value[outputPos++] = ch;
                    break;
                }
            }
        }
        return changed ? new String(value, 0, outputPos) : input;
    }

    protected int replaceChars(int pos, char[] replacement, int inputLength) {
        int rlen = replacement.length;
        int newPos = pos + rlen;
        grow(newPos + inputLength);
        System.arraycopy(replacement, 0, value, pos, rlen);
        return newPos;
    }

    protected void grow(int newSize) {
        int vlen = value.length;
        if (vlen < newSize) {
            char[] newValue = new char[newSize + newSize / 2];
            System.arraycopy(value, 0, newValue, 0, vlen);
            value = newValue;
        }
    }
}
