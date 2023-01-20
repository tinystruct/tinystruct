/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
package org.tinystruct.system.template.variable;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.template.ElementExpression;
import org.w3c.dom.*;

public class Array<T> implements ElementExpression<Variable<T[]>> {

    private String id = null;

    public String parse(String expression, Variable<T[]> variable)
            throws ApplicationException {

        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Element parse(Document doc, Variable<T[]> variable)
            throws ApplicationException {

        if (this.id == null)
            throw new ApplicationException(
                    "You have to specify the variable id before you use the expression.");

        T[] values = variable.getValue();
        boolean founded = false;
        String tagName = null;

        NodeList nodes = doc.getElementsByTagName("list");
        Node current = null;
        int n = 0, j = 0;
        while (j < nodes.getLength()) {
            Node nod = nodes.item(j++);

            Element e = (Element) nod;
            if (e.getAttribute("id").equalsIgnoreCase(this.id)) {
                founded = true;
                tagName = e.getAttribute("tag");
                current = nod;
                break;
            }
        }

        if (!founded)
            throw new ApplicationException(
                    "The element has not been defined in your template: id = " + this.id);

        Element list = doc.createElement(tagName);
        while (n < values.length) {
            String v = String.valueOf(values[n++]);

            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).hasChildNodes()) {
                    int len = nodes.item(i).getChildNodes().getLength();
                    for (j = 0; j < len; j++) {
                        if (nodes.item(i).getChildNodes().item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Node oldChild = nodes.item(i).getChildNodes().item(
                                    j);
                            Node clone = oldChild.cloneNode(true);
                            clone.setTextContent(v);

                            if (list.getChildNodes().getLength() == 0) {
                                Attr attr = doc.createAttribute("class");
                                attr.setValue("first");
                                clone.getAttributes().setNamedItem(attr);
                            } else if (list.getChildNodes().getLength() == values.length - 1) {
                                Attr attr = doc.createAttribute("class");
                                attr.setValue("last");
                                clone.getAttributes().setNamedItem(attr);
                            }

                            list.appendChild(clone);
                        }
                    }
                }
            }
        }

        current.getParentNode().appendChild(list);
        current.getParentNode().replaceChild(list, current);

        for (j = 0; j < nodes.getLength(); j++) {
            Element e = (Element) nodes.item(j);
            e.removeAttribute("item");
            e.removeAttribute("tag");
        }

        return list;
    }
}
