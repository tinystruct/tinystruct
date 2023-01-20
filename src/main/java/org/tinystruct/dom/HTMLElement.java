/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

@Deprecated
public class HTMLElement extends Element {

    private List<HTMLElement> resource = new ArrayList<>();

    public HTMLElement() {
        super();
    }

    public HTMLElement(String name) {
        super(name);
    }

    public HTMLElement(String name, List<Attribute> attributes) {
        super(name, attributes);
    }

    public List<HTMLElement> getChildElements(List<HTMLElement> elements) {
        Iterator<HTMLElement> iterator = elements.iterator();
        this.resource = new ArrayList<>();

        HTMLElement element;

        boolean start = false;
        int same_element_number = 0;

        String elementName = this.getName();
        if (elementName.indexOf('/') == 0)
            return this.resource;

        while (iterator.hasNext()) {
            // 获取每一个Element对象
            element = iterator.next();
            System.out.println("----------------------------[" + elementName
                    + "][" + element.getName()
                    + "]----------------------------");

            // 排除特殊的Element对象
            if (element.getName().equalsIgnoreCase("meta")) {
                continue;
            }

            if (start) {
                this.resource.add(element);
            }

            // 如果Element的对象名正好等于传入的TagName，进行处理，启动子对象获取功能
            if (element.getElementType() != ElementType.NORMAL
                    && element.getName().equalsIgnoreCase(elementName)
                    && same_element_number == 0
                    && element.getAttribute("id").equalsIgnoreCase(this.getAttribute("id"))) {
                same_element_number++;
                start = true;

                continue;
            }

            // 如果遇到Element名称相同的开始元素，那么对该相同对象的数目进行累加
            if (element.getElementType() != ElementType.TEXT
                    && element.getName().equalsIgnoreCase(elementName)) {
                same_element_number++;
            }

            // 如果遇到Element对象结束元素
            if (element.getName().equalsIgnoreCase("/" + elementName)
                    && same_element_number > 0) {
                same_element_number--;

                if (same_element_number == 0) {
                    start = false;
                }
            }

        }

        return this.resource;
    }

    private int brother_element_position = 0;

    /*
     * public HTMLElement getDocumentElements(Vector<HTMLElement> resource) {
     * this.resource=resource; if(this.resource.size()==0) return this;
     *
     * HTMLElement childElement=this.resource.firstElement(); HTMLElement
     * currentElement;
     *
     * int elements_amount=0; boolean accept=false; Iterator<HTMLElement>
     * html=this.resource.iterator(); while(html.hasNext()) {
     * currentElement=html.next();
     *
     * this.brother_element_position++;
     *
     *
     * System.out.println(this.brother_element_position+"->"+currentElement.getName
     * ()); //排除特殊标记 if(currentElement.getName().equalsIgnoreCase("meta")) {
     * continue; }
     *
     * //如果当前的元素名等于子元素名
     * if(currentElement.getName().equalsIgnoreCase(childElement.getName()) &&
     * !accept ) { if(currentElement.name.indexOf('/')==0) {
     * System.out.println("[Jump the Element /]"+currentElement.getName());
     * continue; }
     *
     * if(currentElement.type.equalsIgnoreCase("TEXT")) {
     * System.out.println("[Jump the Element]"+currentElement); continue; }
     *
     * elements_amount++;
     *
     * if(!accept) { //标记的开始 accept=true;
     *
     * //把当前的元素赋给childElement childElement=new
     * HTMLElement(currentElement.getName(),currentElement.attributes);
     * childElement.setElementType(currentElement.type); } else {
     * childElement.getDocumentElement(childElement.resource); }
     *
     * continue; } else
     * if(currentElement.getName().equalsIgnoreCase("/"+childElement.getName())
     * && accept ) { elements_amount--;
     *
     * if(elements_amount==0) { //标记的结束 accept=false;
     *
     * System.out.println("获取当前节点信息");
     * System.out.println(this.name+"["+this.brother_element_position
     * +"]["+this.resource.size()+"]");
     * System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\r\n"
     * );
     *
     * if( this.brother_element_position < this.resource.size() ) {
     * childElement.getDocumentElement(childElement.resource); //作为当前元素的子元素
     * this.addElement(childElement);
     *
     * currentElement=this.resource.get(this.brother_element_position);
     *
     * //下一个平级节点
     * System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
     * System.out.println("Next["+currentElement+"]");
     *
     * childElement=new
     * HTMLElement(currentElement.getName(),currentElement.attributes);
     * childElement.setElementType(currentElement.type); }
     *
     * continue; } }
     *
     * if(accept) { childElement.resource.add(currentElement); } }
     *
     * childElement.getDocumentElement(childElement.resource); //作为当前元素的子元素
     * this.addElement(childElement);
     *
     * System.out.println("["+this.name+
     * "]___________________________________________"); Iterator<HTMLElement>
     * ets=this.resource.iterator(); while(ets.hasNext()) {
     * System.out.print(ets.next()); }System.out.println("["+this.name+
     * "]-------------------------------------------\r\n");
     *
     * return this; }
     */
    public HTMLElement getDocumentElement(List<HTMLElement> resource) {
        this.resource = resource;
        if (this.resource.size() == 0)
            return this;

        HTMLElement childElement = this.resource.get(0);
        HTMLElement currentElement;

        int elements_amount = 0;
        boolean accept = false;
        for (HTMLElement htmlElement : this.resource) {
            currentElement = htmlElement;
            this.brother_element_position++;

            System.out.println(this.brother_element_position + "->[" + accept
                    + "]" + currentElement.getName());
            // 排除特殊标记
            if (currentElement.getName().equalsIgnoreCase("meta")) {
                continue;
            }

            // 判断是否为结束标记
            if (currentElement.getName().equalsIgnoreCase(
                    "/" + childElement.getName())
                    && accept) {
                elements_amount--;

                if (elements_amount == 0) {
                    // 标记的结束
                    accept = false;

                    System.out.println("获取当前节点信息");
                    System.out.println(this.getName() + "["
                            + this.brother_element_position + "]["
                            + this.resource.size() + "]");
                    System.out
                            .println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\r\n");

                    if (this.brother_element_position < this.resource.size()) {
                        // 作为当前元素的子元素
                        childElement.getDocumentElement(childElement.resource);
                        this.addElement(childElement);

                        currentElement = this.resource
                                .get(this.brother_element_position);

                        if (currentElement.getName().indexOf('/') != 0) {
                            // 下一个平级节点
                            System.out
                                    .println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                            System.out.println("Next[" + currentElement + "]");

                            childElement = new HTMLElement(currentElement
                                    .getName(), currentElement.getAttributes());
                            childElement.setElementType(currentElement.getElementType());
                        }
                    }

                    System.out.println("[" + this.getName()
                            + "]___________________________________________");
                    for (HTMLElement element : this.resource) {
                        System.out.print(element);
                    }
                    System.out
                            .println("["
                                    + this.getName()
                                    + "]-------------------------------------------\r\n");

                    continue;
                }
            }

            // 判断是否为开始标记
            if (currentElement.getName().equalsIgnoreCase(
                    childElement.getName())
                    && !accept) {
                if (currentElement.getName().indexOf('/') == 0) {
                    System.out.println("[Jump the Element /][<"
                            + this.brother_element_position + ">]"
                            + currentElement.getName());

                    continue;
                }

                if (currentElement.getElementType() == ElementType.TEXT) {
                    System.out.println("[Jump the Element][<"
                            + this.brother_element_position + ">]"
                            + currentElement.getName());
                    continue;
                }

                elements_amount++;

                if (!accept) {
                    // 标记的开始
                    accept = true;

                    // 把当前的元素赋给 childElement
                    childElement = new HTMLElement(currentElement.getName(),
                            currentElement.getAttributes());
                    childElement.setElementType(currentElement.getElementType());
                }
                // else
                // {
                // childElement.getDocumentElement(childElement.resource);
                // }

                continue;
            }

            if (accept) {
                childElement.resource.add(currentElement);
            }
        }

        childElement.getDocumentElement(childElement.resource);
        // 作为当前元素的子元素
        this.addElement(childElement);

        return this;
    }

    public HTMLElement getDocumentFromResource(List<HTMLElement> resource) {
        if (resource == null || resource.size() == 0)
            return new HTMLElement("NULL_ELEMENT");

        boolean element_start = false;
        int same_element_number = 0;

        this.resource = resource;

        // 遍历子元素，从第一个子元素开始遍历，将第一个子元素记录下来，并寻找此元素的闭合元素
        HTMLElement childElement = this.resource.get(0);
        HTMLElement currentElement;
        Iterator<HTMLElement> iter = this.resource.iterator();

        while (iter.hasNext()) {
            currentElement = iter.next();

            // 统计遍历次数，即当前的元素索引

            // 排除元素
            if (currentElement.getName().equalsIgnoreCase("meta")) {
                continue;
            }

            // 将所有子元素写到一个新的资源文件当中
            if (element_start) {
                childElement.resource.add(currentElement);
                System.out.print("ChildElement:" + currentElement);
            }

            // 如果Element的对象名正好等于传入的TagName，进行处理，启动子对象获取功能
            if (currentElement.getName().equalsIgnoreCase(childElement.getName())
                    && same_element_number == 0) {
                // 将已经读完的子元素保存到本级子元素集合中
                // this.addElement(childElement);

                System.out.print("ParentElement->" + currentElement);

                same_element_number++;
                element_start = true;

                continue;
            }

            // 寻找闭合元素，目前条件只判断元素名
            if (currentElement.getName().equalsIgnoreCase(
                    "/" + childElement.getName())) {
                same_element_number--;

                if (same_element_number == 0) {
                    element_start = false;
                }
            }

        }

        return this;
    }

    public HTMLElement getDocument(List<HTMLElement> resource) {
        // 获取当前的HTMLElement的子资源内容,如果当前的HTMLElement为HTML，那么它的子资源的第一个元素为HEAD
        this.resource = this.getChildElements(resource);

        // 打印元素名及子资源长度
        System.out.println(this.getElementType() + "#" + this.getName() + ".length:"
                + this.resource.size());

        // 打印出元素名的子集信息
        /*
         * Iterator<HTMLElement> res=this.resource.iterator();
         * while(res.hasNext()) { System.out.print(res.next()); }
         * System.out.println("#--------------------------------#");
         */

        // 如果子资源长度不为空，对该子元素进行遍历
        if (this.resource.size() > 0) {
            // 获取子资源内容的第一个子元素，如HEAD，作为将要操作的对象
            HTMLElement element = this.resource.get(0);
            element.getDocument(this.resource);
            this.addElement(element);

            System.out.println("---Information for details---");
            System.out.println("---" + this.getName() + " size: "
                    + this.resource.size() + ".\t " + element.getName() + " size: "
                    + element.resource.size());

            // 如果第一个子元素的长度为0，表示第一个子元素的资源不存在
            if (element.resource.size() + 2 < this.resource.size()) {
                return this.resource.get(element.resource.size() + 2)
                        .getDocument(this.resource);
            }
        }

        return this;
    }

    @Override
    public String toString() {
        if (this.getElementType() == ElementType.TEXT) {
            return this.getName() + "\r\n";
        }

        StringBuffer buffer = new StringBuffer();
        boolean invalidName = this.getName() != null && this.getName().trim().length() != 0;

        if (invalidName) {
            buffer.append(this.getSpace());
            buffer.append("<");
            buffer.append(this.getName());
        }

        List<Attribute> attributes = this.getAttributes();
        Attribute currentAttribute;
        Iterator<Attribute> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            currentAttribute = iterator.next();
            buffer.append(" " + currentAttribute.name + "=\""
                    + currentAttribute.value + "\"");
        }

        if (this.getChildNodes().size() > 0) {
            if (invalidName) {
                buffer.append(">\r\n");
            }

            this.setData(null);

            Iterator<Element> childList = this.getChildNodes().iterator();
            while (childList.hasNext()) {
                buffer.append(childList.next().toString());
            }
        }

        if (invalidName) {
            if ((this.getData() != null && this.getData().trim().length() > 0)) {
                buffer.append(this.getData() + "\r\n");
                buffer.append("</");
                buffer.append(this.getName());
                buffer.append(">\r\n");
            } else if (this.getChildNodes().size() > 0) {
                buffer.append(this.getSpace());
                buffer.append("</");
                buffer.append(this.getName());
                buffer.append(">\r\n");
            } else {
                buffer.append(">\r\n");
            }
        }

        return buffer.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + brother_element_position;
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof HTMLElement))
            return false;
        HTMLElement other = (HTMLElement) obj;
        if (brother_element_position != other.brother_element_position)
            return false;
        if (resource == null) {
            return other.resource == null;
        } else return resource.equals(other.resource);
    }
}
