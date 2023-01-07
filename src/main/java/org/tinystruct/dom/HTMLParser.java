/*******************************************************************************
 * Copyright  (c) 2017 James Mover Zhou
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

import org.tinystruct.ApplicationException;
import org.tinystruct.system.util.TextFileLoader;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class HTMLParser {
    private final List<HTMLElement> resources = new ArrayList<>();
    private final String HTML;
    private int position = 0;
    private int length = 0;

    private int current_position = -1;
    public HTMLParser(StringBuffer HTML) {
        this.HTML = HTML.toString();
        this.length = this.HTML.length();
    }

    public HTMLParser(String FileName) throws ApplicationException {
        this.HTML = new TextFileLoader(FileName).getContent().toString();
        this.length = this.HTML.length();
    }

    /**
     * @return the resources
     */
    public List<HTMLElement> getResources() {
        return resources;
    }

    /**
     * Get the position for start tag.
     *
     * @param text text
     * @return position
     */
    public int getPositionForStartTag(String text) {
        return this.getPositionForChar(text, '<');
    }

    /**
     * Get the position for end tag.
     *
     * @param text text
     * @return position
     */
    public int getPositionForEndTag(String text) {
        return this.getPositionForChar(text, '>');
    }

    public int getPositionForChar(String text, char c) {
        if (text.indexOf(c) == -1) {
            return -1;
        }

        char[] _text = text.toCharArray();
        //初始化当前位置
        for (int i = this.current_position + 1; i < _text.length; i++) {
            if (_text[i] == c) {
                int last_position = this.current_position;
                this.current_position = i;
                this.position = this.current_position;
                return i;
            }
        }

        this.position = -1;
        return -1;
    }

    public String getTagName() {
        String read_content = "";
        return read_content.split(" ")[0];
    }

    public String getTagContent() {
        String current_tag_attributes = "";
        return current_tag_attributes;
    }

    public void process() {
        int beginIndex = 0;
        int endIndex = 0;

        String node;
        String elementName;
        String[] elementAttribute, attribute_text;

        HTMLElement element;
        Attribute attribute;

        while (true) {
            if (this.position >= this.length) break;
            beginIndex = this.getPositionForStartTag(this.HTML);

            if (this.position == -1) break;
            node = this.HTML.substring(endIndex, beginIndex);

            if (node.trim().length() > 0) {
                if (node.indexOf('<') == -1 || node.indexOf('>') == -1) {
                    element = new HTMLElement(node);
                    element.setElementType(ElementType.TEXT);
                } else {
                    elementAttribute = node.split(">")[0].split(" ");
                    elementName = node.split(">")[0].split(" ")[0].replace('<', ' ').trim();

                    element = new HTMLElement(elementName);
                    for (int i = 1; i < elementAttribute.length; i++) {
                        if (elementAttribute[i].indexOf('=') != -1) {
                            attribute_text = elementAttribute[i].split("=");
                            attribute = new Attribute(attribute_text[0], attribute_text[1].replace('"', ' ').trim());
                            element.setAttribute(attribute.name, attribute.value);
                        }
                    }
                }

                this.resources.add(element);
            }

            endIndex = this.getPositionForEndTag(this.HTML) + 1;

            if (this.position == -1) break;
            node = this.HTML.substring(beginIndex, endIndex);

            if (node.trim().length() > 0) {
                if (node.indexOf('<') == -1 || node.indexOf('>') == -1) {
                    element = new HTMLElement(node);
                    element.setElementType(ElementType.TEXT);
                } else {
                    elementAttribute = node.split(">")[0].split(" ");
                    elementName = node.split(">")[0].split(" ")[0].replace('<', ' ').trim();

                    element = new HTMLElement(elementName);

                    for (int i = 1; i < elementAttribute.length; i++) {
                        if (elementAttribute[i].indexOf('=') != -1) {
                            attribute_text = elementAttribute[i].split("=");
                            attribute = new Attribute(attribute_text[0], attribute_text[1].replace('"', ' ').trim());
                            element.setAttribute(attribute.name, attribute.value);
                        }
                    }
                }

                this.resources.add(element);
            }
        }
    }

    public void parse() {
        System.out.println("Parser is starting...");

        this.process();
    }


}