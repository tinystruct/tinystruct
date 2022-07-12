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
package org.tinystruct.system.template.parser;

import org.tinystruct.ApplicationException;
import org.tinystruct.application.Template;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.template.Parser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ArrayParser<T> implements Parser {

    private final Variable<T[]> variable;
    private final Document doc;

    public ArrayParser(Variable<T[]> variable, Document doc) {
        this.variable = variable;
        this.doc = doc;
    }

    public void parse(Template template) throws ApplicationException {

        String elementId = this.variable.getName();

        T[] values = this.variable.getValue();

        Node current = doc.getElementById(elementId);
        int n = 0, j = 0;

        if (current == null)
            throw new ApplicationException(
                    "The element has not been defined in your template: id = " + elementId);

        Element list = doc.createElement(current.getNodeName());
        while (n < values.length) {
            String v = String.valueOf(values[n++]);

            if (current.hasChildNodes()) {
                int len = current.getChildNodes().getLength();
                for (j = 0; j < len; j++) {
                    if (current.getChildNodes().item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Node oldChild = current.getChildNodes().item(
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

        current.getParentNode().appendChild(list);
        current.getParentNode().replaceChild(list, current);

    }

}
