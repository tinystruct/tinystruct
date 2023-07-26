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
package org.tinystruct.system.template;

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.application.SharedVariables;
import org.tinystruct.application.Template;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.TextFileLoader;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DefaultTemplate implements Template {

    // create a script engine manager
    // create a JavaScript engine
    private static final String engineName = "JavaScript";
    private final Application app;
    private final ScriptEngine engine;
    private Map<String, Variable<?>> variables;
    private InputStream in;
    private String view;

    public DefaultTemplate(Application app, InputStream in) {
        this.app = app;
        this.engine = SingletonHolder.manager.getEngineByName(engineName);
        if (null != this.engine)
            this.engine.put("self", this.app);
        this.in = in;

        this.variables = SharedVariables.getInstance().getVariables();
    }

    public DefaultTemplate(Application app, InputStream in, Map<String, Variable<?>> variables) {
        this(app, in);
        Map<String, Variable<?>> tmp = new HashMap<>();
        tmp.putAll(this.variables);
        tmp.putAll(variables);

        this.variables = tmp;
    }

    public DefaultTemplate(Application app, String view) {
        this.app = app;
        this.view = view;
        this.engine = SingletonHolder.manager.getEngineByName(engineName);
        if (null != this.engine) {
            this.engine.put("self", this.app);
            this.engine.put("self.toString", "Please don't execute like this.");
        }

        this.variables = SharedVariables.getInstance().getVariables();
    }

    private static void stripEmptyTextNode(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            boolean condition = child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty();
			if (condition) {
			    child.getParentNode().removeChild(child);
			    i--;
			}
            stripEmptyTextNode(child);
        }
    }

    @Override
    public String getName() {
        return engine.NAME;
    }

    @Override
    public Variable<?> getVariable(String arg0) {
        return this.variables.get(arg0);
    }

    @Override
    public Map<String, Variable<?>> getVariables() {
        return this.variables;
    }

    @Override
    public String parse() throws ApplicationException {
        Configuration<String> config = app.getConfiguration();
        if(config == null) {
            throw new ApplicationException("The configuration for the app has not been set.");
        }
        String value;

        if (this.view == null) {
            TextFileLoader loader = new TextFileLoader(in);
            loader.setCharset(config.get("charset"));

            try {
                this.view = loader.getContent().toString();
            } catch (ApplicationException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        if (this.view.trim().length() > 0) {
            Document doc;
            try (InputStream in = new ByteArrayInputStream(this.view.getBytes(StandardCharsets.UTF_8))) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant  
                dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
                dbf.setValidating(false);
                DocumentBuilder builder = dbf.newDocumentBuilder();

                doc = builder.parse(in);
                stripEmptyTextNode(doc);

                if (this.engine != null) {
                    NodeList js = doc.getElementsByTagName("javascript");
                    int length = js.getLength();

                    if (length > 0) {
                        // evaluate JavaScript code from String
                        String result;
                        ScriptContext context = engine.getContext();
                        StringWriter sw = new StringWriter();
                        Writer writer = new PrintWriter(sw, true);
                        context.setWriter(writer);

                        for (int i = 0; i < length; i++) {
                            Node node = js.item(i);
                            Object r = engine.eval(node.getTextContent());

                            if (r != null) {
                                result = String.valueOf(r);
                            } else {
                                result = sw.toString();
                            }

                            Text t = doc.createTextNode(result);

                            Node p = node.getParentNode();
                            p.appendChild(t);
                            p.replaceChild(t, node);
                        }
                        writer.close();
                    }
                }
            } catch (ParserConfigurationException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (SAXException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (DOMException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (ScriptException e) {
                throw new ApplicationException(e.getMessage(), e);
            }

            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            writer.write("<!DOCTYPE html>\r\n");
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // Compliant
            Transformer transformer;
            try {
                transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
                transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "xml");
                transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "script");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.transform(domSource, result);
            } catch (TransformerConfigurationException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (TransformerException e) {
                throw new ApplicationException(e.getMessage(), e);
            }

            this.view = writer.toString();

            Set<Entry<String, Variable<?>>> sets = variables.entrySet();
            Iterator<Entry<String, Variable<?>>> iterator = sets
                    .iterator();

            Variable<?> variable;
            Context ctx = app.getContext();

            while (iterator.hasNext()) {
                Entry<String, Variable<?>> v = iterator.next();
                variable = v.getValue();

                if (variable.getType() == DataType.ARRAY) {
                    // TODO
                } else {
                    if (v.getKey().startsWith("[%LINK:")) {
                        String baseUrl;

                        if (ctx != null
                                && ctx.getAttribute("HTTP_HOST") != null)
                            baseUrl = ctx.getAttribute("HTTP_HOST").toString();
                        else
                            baseUrl = config.get("default.base_url");

                        value = baseUrl + variable.getValue();
                    } else
                        value = variable.getValue().toString();

                    value = value.replace("&", "&amp;");
                    this.view = this.view.replace(v.getKey(), value);
                }
            }

            return this.view;
        }

        return this.view;
    }

    @Override
	public void setVariable(Variable<?> arg0) {
        this.variables.put(arg0.getName(), arg0);
    }

    private static final class SingletonHolder {
        static final ScriptEngineManager manager = new ScriptEngineManager();
    }
}