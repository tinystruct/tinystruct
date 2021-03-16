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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Element implements Cloneable {
    private String name;
    private String data;
    private ElementType elementType = ElementType.NORMAL;
    private List<Attribute> attributes;
    private List<Element> childNodes;
    private List<Element> foundElementList;
    private Element parent;
    private boolean haveData = false;

    /**
     * Default Constructor
     */
    public Element() {
        this.name = "";

        this.childNodes = new Vector<Element>();
        this.attributes = new Vector<Attribute>();

        this.data = "";
    }

    /**
     * Constructor
     *
     * @param name name
     */
    public Element(String name) {
        this.name = name;

        this.attributes = new Vector<Attribute>();
        this.childNodes = new Vector<Element>();

        this.data = "";
    }

    /**
     * Constructor
     *
     * @param  name name
     * @param  attributes list of attribute
     */
    public Element(String name, List<Attribute> attributes) {
        this.name = name;

        this.attributes = attributes;
        this.childNodes = new Vector<Element>();

        this.data = "";
    }

    /**
     * Constructor
     *
     * @param  name name
     * @param  data data
     */
    public Element(String name, String data) {
        this.name = name;

        this.attributes = new Vector<Attribute>();
        this.childNodes = new Vector<Element>();

        this.data = data;
    }

    public void setElementType(ElementType type) {
        this.elementType = type;
    }

    public ElementType getElementType() {
        return this.elementType;
    }

    /**
     * Add attribute to this xml element.
     *
     * @param keyName name of key
     * @param value   new attribute value
     * @return old attribute value
     */
    public Attribute setAttribute(String keyName, Object value) {
        if ((value != null) && (keyName != null)) {
            boolean exists = false;
            Iterator<Attribute> iterator = this.attributes.iterator();
            Attribute attribute;
            while (iterator.hasNext()) {
                attribute = iterator.next();
                if (attribute.name.equalsIgnoreCase(keyName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                Attribute returnValue = new Attribute(keyName, String.valueOf(value));
                this.attributes.add(returnValue);

                return returnValue;
            }

        }
        return null;
    }

    /**
     * String getAttribute(String keyName)
     *
     * @param attributeName attribute name
     * @return attribute
     */
    public String getAttribute(String attributeName) {
        Iterator<Attribute> iterator = this.attributes.iterator();
        Attribute attribute;
        while (iterator.hasNext()) {
            attribute = iterator.next();
            if (attribute.name.equalsIgnoreCase(attributeName)) {
                return attribute.value;
            }
        }

        return "";
    }


    public void removeAttribute(String attributeName) {

        Iterator<Attribute> iterator = this.attributes.iterator();
        Attribute attribute;
        while (iterator.hasNext()) {
            attribute = iterator.next();
            if (attribute.name.equalsIgnoreCase(attributeName)) {
                this.attributes.remove(attribute);
                break;
            }
        }

    }

    /**
     * Get attributes.
     *
     * @return attributes
     */
    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    /**
     * @param attributes List to use as the attributes
     */
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return Enumeration
     */
    public Vector<String> getAttributeNames() {
        int i = 0, size = this.attributes.size();
        Vector<String> keys = new Vector<String>(size);

        while (i < size) {
            keys.add(this.attributes.get(i++).name);
        }
        return keys;
    }

    /**
     * @param element element
     * @return boolean
     */
    public boolean addElement(Element element) {
        element.setParent(this);
        element.level = this.level + 1;

        return this.childNodes.add(element);
    }

    public Element addElement(String elementName) {
        Element element = new Element(elementName);

        if (this.addElement(element)) {
            return element;
        }
        return null;
    }

    public boolean removeElement(Element element) {
        return this.childNodes.remove(element);
    }

    public boolean removeElement(String elementName) {
        int i = 0;
        Element the_target_element = null;
        while (i < this.childNodes.size()) {
            the_target_element = this.childNodes.get(i);
            if (the_target_element.name.equalsIgnoreCase(elementName))
                break;
        }
        return this.childNodes.remove(the_target_element);
    }

    public Element removeElement(int index) {
        return this.childNodes.remove(index);
    }

    public void removeAllElements() {
        this.childNodes = new Vector<Element>();
    }

    public void removeFromParent() {
        if (this.parent != null) {
            this.parent.removeElement(this);
            this.parent = null;
        }
    }

    public boolean contains(Element element) {
        return this.childNodes.contains(element);
    }

    public void append(Element element) {
        if (this.parent != null && this.parent.contains(element)) {
            this.parent.removeElement(element);
        }

        this.addElement(element);
    }

    /**
     * Insert element to a specific position
     *
     * @param e element
     * @param index index
     */
    public void insertElement(Element e, int index) {
        e.removeFromParent();
        this.childNodes.add(index, e);
        e.setParent(this);
    }

    /**
     * @return child nodes
     */
    public List<Element> getChildNodes() {
        return this.childNodes;
    }

    public int size() {
        return this.childNodes.size();
    }

    public Vector<String> getElementsTagNames() {
        Vector<String> keys = new Vector<String>(this.size());
        int i = 0;

        while (i < this.size()) {
            keys.add(this.getElementByIndex(i++).name);
        }
        return keys;
    }

    public Element getElementByIndex(int index) {
        return this.childNodes.get(index);
    }

    private boolean containsAttribute(List<Attribute> attributes, String attributeName) {
        int i = 0, size = attributes.size();

        while (i < size) {
            if (this.attributes.get(i++).name.equalsIgnoreCase(attributeName))
                return true;
        }
        return false;
    }

    private String getAttribute(List<Attribute> attributes, String attributeName) {
        Iterator<Attribute> iterator = attributes.iterator();
        Attribute attribute;
        while (iterator.hasNext()) {
            attribute = iterator.next();
            if (attribute.name.equalsIgnoreCase(attributeName)) {
                return attribute.value;
            }
        }

        return "";
    }

    public Element getElementById(String Id) {
        int i = 0;
        Element currentElement = null;
        while (i < this.childNodes.size()) {
            if (containsAttribute(this.childNodes.get(i).attributes, "id")) {
                if (getAttribute(this.childNodes.get(i).attributes, "id")
                        .equalsIgnoreCase(Id)) {
                    currentElement = this.childNodes.get(i);
                    break;
                } else
                    currentElement = this.childNodes.get(i++).getElementById(Id);
            } else
                currentElement = this.childNodes.get(i++).getElementById(Id);
        }

        return currentElement;
    }

    private List<Element> getChildElementsByTagName(String tagName) {
        this.foundElementList = new Vector<Element>();
        Iterator<Element> iterator = this.childNodes.iterator();

        while (iterator.hasNext()) {
            Element currentElement = iterator.next();
            currentElement.getChildElementsByTagName(tagName);

            if (currentElement.name.equalsIgnoreCase(tagName)) {
                this.foundElementList.add(currentElement);
            }
        }

        return this.foundElementList;
    }

    public List<Element> getElementsByTagName(String tagName) {
        return this.getChildElementsByTagName(tagName);
    }

    /**
     * Set the parent element.
     *
     * @param parent The element that contains this one
     */
    public void setParent(Element parent) {
        this.parent = parent;
    }

    /**
     * Gives the element containing the current element
     *
     * @return parent
     */
    public Element getParent() {
        return parent;
    }

    /**
     * Set the data for this element
     *
     * @param data The String representation of the data
     * @return this
     */
    public Element setData(String data) {
        this.data = data;
        this.haveData = true;
        return this;
    }

    /**
     * Return the data associated with the current Xml element
     *
     * @return data
     */
    public String getData() {
        return this.data;
    }

    /**
     * Return the name of the current element
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    public int level = 0;

    public String getSpace() {
        StringBuffer space = new StringBuffer();
        int n = this.level;
        while (n-- > 0) {
            space.append(" ");
        }

        if (space.length() == 0)
            return space + "\r\n";

        return "\r\n" + space;
    }

    public String toString() {
        if (this.elementType == ElementType.TEXT) {
            return this.name;
        }

        boolean valid = this.name != null && this.name.trim().length() != 0;

        if (!valid)
            return "Invalid Tag Name";

        StringBuffer buffer = new StringBuffer(), nodes = new StringBuffer();
        buffer.append(this.getSpace());
        buffer.append("<");
        buffer.append(this.name);

        Attribute currentAttribute;
        Iterator<Attribute> iterator = this.attributes.iterator();
        while (iterator.hasNext()) {
            currentAttribute = iterator.next();
            buffer.append(" " + currentAttribute.name + "=\""
                    + currentAttribute.value + "\"");
        }

        if (this.data != null) {
            nodes.append(this.data);
        }

        if (this.childNodes.size() > 0) {
            Iterator<Element> childList = this.childNodes.iterator();

            while (childList.hasNext()) {
                nodes.append(childList.next().toString());
            }

            nodes.append(this.getSpace());
        }

        if (this.haveData || nodes.length() > 0) {
            buffer.append(">");
            buffer.append(nodes);
            buffer.append("</");
            buffer.append(this.name);
            buffer.append(">");

            nodes.delete(0, nodes.length());
        } else {
            buffer.append("/>");
        }

        String s = buffer.toString();
        buffer.delete(0, buffer.length());

        return s;
    }

    public static void printNode(Element node, String indent) {
        String data = node.getData();
        if ((data == null) || data.equals("")) {
            System.out.println(indent + node.getName());
        } else {
            System.out.println(indent + node.getName() + " = '" + data + "'");
        }
        // print attributes
        List<Attribute> attributes = node.getAttributes();

        Iterator<Attribute> iterator = attributes.iterator();

        while (iterator.hasNext()) {
            Attribute attribute = iterator.next();
            System.out.println(indent + attribute.name + ":" + attribute.value);
        }

        List<Element> subs = node.getChildNodes();
        for (Iterator<Element> it = subs.iterator(); it.hasNext(); ) {
            printNode(it.next(), indent + "    ");
            // for (i = 0; i < subs.size(); i++) {
            // printNode((XmlElement) subs.get(i), indent + " ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            Element clone = (Element) super.clone(); // creates a shallow
            // copy of this object
            if (this.attributes != null) {
                clone.attributes = new Vector<Attribute>();
                List<Attribute> attribs = this.attributes;
                Attribute attribute;
                Iterator<Attribute> it = attribs.iterator();
                while (it.hasNext()) {
                    attribute = it.next();
                    clone.setAttribute((Attribute) attribute.clone());
                }
            }

            if (this.data != null) {
                clone.setData(this.data);
            }

            if (this.childNodes != null) {
                clone.childNodes = new Vector<Element>();
                List<Element> childs = this.childNodes;
                Element child;
                Iterator<Element> it = childs.iterator();
                while (it.hasNext()) {
                    child = it.next();
                    clone.addElement((Element) child.clone());
                }
            }

            return clone;
        } catch (CloneNotSupportedException cnse) {
            throw new InternalError("Could not clone Element: " + cnse);
        }
    }

    public Attribute setAttribute(Attribute attribute) {

        return this.setAttribute(attribute.name, attribute.value);
    }

    /**
     * Sets the name.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if the specified objects are equal. They are equal if they
     * are both null OR if the <code>equals()</code> method return true. (
     * <code>obj1.equals(obj2)</code>).
     *
     * @param obj1 first object to compare with.
     * @param obj2 second object to compare with.
     * @return true if they represent the same object; false if one of them is
     * null or the <code>equals()</code> method returns false.
     */
    private boolean equals(Object obj1, Object obj2) {
        boolean equal = false;
        if ((obj1 == null) && (obj2 == null)) {
            equal = true;
        } else if ((obj1 != null) && (obj2 != null)) {
            equal = obj1.equals(obj2);
        }
        return equal;
    }

    /**
     * {@inheritDoc} Recursive comparison.
     */
    @Override
    public boolean equals(Object obj) {
        boolean equal = false;
        if ((obj != null) && (obj instanceof Element)) {
            Element other = (Element) obj;
            if (equals(attributes, other.attributes)
                    && equals(data, other.data) && equals(name, other.name)
                    && equals(childNodes, other.childNodes)) {
                equal = true;
            }
        }
        return equal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // Hashcode value should be buffered.
        int hashCode = 23;
        if (attributes != null) {
            hashCode += (attributes.hashCode() * 13);
        }
        if (data != null) {
            hashCode += (data.hashCode() * 17);
        }
        if (name != null) {
            hashCode += (name.hashCode() * 29);
        }
        if (childNodes != null) {
            hashCode += (childNodes.hashCode() * 57);
        }
        return hashCode;
    }

}