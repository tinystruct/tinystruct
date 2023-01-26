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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.Set;

public class Listing<T> implements ElementExpression<Variable<Set<T>>> {

    public Element parse(Element element, Variable<Set<T>> variable) {

        NodeList childNodes = element.getChildNodes();

        Iterator<T> iterator = variable.getValue().iterator();
        while (iterator.hasNext()) {

            int i = 0, length = childNodes.getLength();
            while (i++ < length) {
                Node node = childNodes.item(i);
                String nodeValue = node.getNodeValue();
                nodeValue = nodeValue.replaceAll("\\{%" + variable.getName() + "%\\}",
                        String.valueOf(iterator.next()));
                node.setNodeValue(nodeValue);
            }
        }

        element.removeAttribute("item");
        element.removeAttribute("tag");

        return element;
    }

    @Override
	public String parse(String expression, Variable<Set<T>> value)
            throws ApplicationException {

        return null;
    }

    @Override
	public Element parse(Document document, Variable<Set<T>> value) {

        return null;
    }

    @Override
	public void setId(String id) {


    }

}
