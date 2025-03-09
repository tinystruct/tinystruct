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
package org.tinystruct.system.template;

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.application.ActionRegistry;
import org.tinystruct.application.SharedVariables;
import org.tinystruct.application.Template;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.TextFileLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import static org.tinystruct.Application.DEFAULT_BASE_URL;

public class DefaultTemplate implements Template {

    private static final String HTML5_DOCTYPE = "<!DOCTYPE html>";
    private static final String HTML4_TRANSITIONAL_DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
    private static final String HTML4_STRICT_DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">";
    private static final String XHTML1_TRANSITIONAL_DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
    private static final String XHTML1_STRICT_DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";

    // Document type enum
    public enum DocumentType {
        XML,
        HTML5,
        HTML4_TRANSITIONAL,
        HTML4_STRICT,
        XHTML1_TRANSITIONAL,
        XHTML1_STRICT
    }

    private static final Set<String> voidElements = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr", "path"
    ));

    // create a script engine manager
    // create a JavaScript engine
    private static final String engineName = "JavaScript";
    private final ScriptEngine engine;
    final Application app;
    final ActionRegistry registry = ActionRegistry.getInstance();
    Map<String, Variable<?>> variables;
    InputStream in;
    String view;

    public DefaultTemplate(Application app, InputStream in) {
        this.app = app;
        this.engine = SingletonHolder.manager.getEngineByName(engineName);
        if (null != this.engine)
            this.engine.put("self", this.app);
        this.in = in;

        this.variables = SharedVariables.getInstance(this.app.getLocale().toString()).getVariables();
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

        this.variables = SharedVariables.getInstance(this.app.getLocale().toString()).getVariables();
    }

    static void stripEmptyTextNode(Node node) {
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

    private Document parseDocument(String content) throws ApplicationException {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
            dbf.setValidating(false);
            DocumentBuilder builder = dbf.newDocumentBuilder();

            Document doc = builder.parse(in);
            stripEmptyTextNode(doc);
            return doc;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    private void processJavaScript(Document doc) throws ApplicationException {
        if (this.engine != null) {
            NodeList js = doc.getElementsByTagName("javascript");
            int length = js.getLength();

            if (length > 0) {
                try {
                    ScriptContext context = engine.getContext();
                    StringWriter sw = new StringWriter();
                    Writer writer = new PrintWriter(sw, true);
                    context.setWriter(writer);

                    for (int i = 0; i < length; i++) {
                        Node node = js.item(i);
                        Object r = engine.eval(node.getTextContent());

                        String result = (r != null) ? String.valueOf(r) : sw.toString();
                        Text t = doc.createTextNode(result);

                        Node p = node.getParentNode();
                        p.appendChild(t);
                        p.replaceChild(t, node);
                    }
                    writer.close();
                } catch (ScriptException e) {
                    throw new ApplicationException(e.getMessage(), e);
                } catch (IOException e) {
                    throw new ApplicationRuntimeException(e);
                }
            }
        }
    }

    @Override
    public String parse() throws ApplicationException {
        Configuration<String> config = app.getConfiguration();
        if (config == null) {
            throw new ApplicationException("The configuration for the app has not been set.");
        }

        if (this.view == null) {
            TextFileLoader loader = new TextFileLoader(in);
            loader.setCharset(config.get("charset"));

            try {
                this.view = loader.getContent().toString();
            } catch (ApplicationException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        if (!this.view.trim().isEmpty()) {
            String view = preprocess(this.view);
            if(view.contains("<javascript>")) {
                Document doc = parseDocument(view);
                processJavaScript(doc);

                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                writer.write("<!DOCTYPE html>\r\n");
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // Compliant
                try {
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "no");
                    transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "xml");
                    transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "script");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                    transformer.transform(domSource, result);
                } catch (TransformerException e) {
                    throw new ApplicationException(e.getMessage(), e);
                }

                this.view = writer.toString();
            } else {
                this.view = view;
            }

            Set<Entry<String, Variable<?>>> sets = variables.entrySet();
            for (Entry<String, Variable<?>> v : sets) {
                Variable<?> variable = v.getValue();
                if (variable.getType() != DataType.ARRAY) {
                    String value = variable.getValue().toString().replace("&", "&amp;");
                    this.view = this.view.replace(v.getKey(), value);
                }
            }

            registry.paths().forEach(path -> {
                final String v = this.generateLink(path);
                this.view = this.view.replace("[%LINK:" + path + "%]", v);
            });

            return this.view;
        }

        return this.view;
    }

    /**
     * Get a link.
     *
     * @param path path
     * @return link string
     */
    public String generateLink(String path) {
        String baseUrl;
        if (app.getContext() != null && app.getContext().getAttribute("HTTP_HOST") != null) {
            baseUrl = app.getContext().getAttribute("HTTP_HOST").toString();
        } else {
            baseUrl = app.getConfiguration().get(DEFAULT_BASE_URL);
        }

        if (path != null) {
            return baseUrl + path + "&lang=" + app.getLocale().toLanguageTag();
        }

        return "#";
    }

    @Override
    public void setVariable(Variable<?> arg0) {
        this.variables.put(arg0.getName(), arg0);
    }

    /**
     * Preprocess HTML to make it more XML-like
     *
     * @param html The HTML string to process
     * @return Processed HTML string
     */
    private static String preprocess(String html) {
        String[] tags = html.split("<|</");
        StringBuilder autoClosedHtml = new StringBuilder();
        Set<String> openTags = new LinkedHashSet<>();

        boolean skip = false;
        for (String tag : tags) {
            if (tag.startsWith("/script>")) skip = false;
            if (skip) {
                autoClosedHtml.append("<").append(tag);
                continue;
            }
            if (tag.trim().isEmpty()) continue;
            int endIndex = tag.indexOf(">");
            if (endIndex == -1) {
                autoClosedHtml.append("<").append(tag);
                continue;
            }

            String tagName = tag.substring(0, endIndex).split("\\s|/>")[0].toLowerCase();
            boolean isSelfClosing = tag.endsWith("/>") || voidElements.contains(tagName);

            if (!tagName.isEmpty() && !tag.startsWith("!") && !tag.startsWith("/") && !isSelfClosing) {
                if (tagName.indexOf(' ') != -1) {
                    openTags.add(tagName.substring(0, tagName.indexOf(' ')));
                } else if (tagName.indexOf('/') != -1) {
                    openTags.add(tagName.substring(0, tagName.indexOf('/')));
                } else
                    openTags.add(tagName);
            } else if (!tagName.isEmpty() && tag.startsWith("/")) {
                openTags.remove(tagName.substring(1));
            }
            if (tag.startsWith("script")) skip = true;
            autoClosedHtml.append("<").append(tag);
        }

        // Close any remaining open tags, excluding void elements
        for (String openTag : openTags) {
            if (!voidElements.contains(openTag)) {
                autoClosedHtml.append("</").append(openTag).append(">");
            }
        }

        html = autoClosedHtml.toString();

        // Ensure proper HTML structure
        boolean hasHtmlTag = html.toLowerCase().contains("<html");
        boolean hasBodyTag = html.toLowerCase().contains("<body");

        if (!hasHtmlTag) {
            html = "<html>" + html + "</html>";
        }
        if (!hasBodyTag) {
            html = html.replaceFirst("(<html[^>]*>)", "$1<body>")
                    .replaceFirst("(</html>)", "</body>$1");
        }

        // Handle void elements
        StringBuilder processed = new StringBuilder();
        int pos = 0;
        while (pos < html.length()) {
            int tagStart = html.indexOf("<", pos);
            if (tagStart == -1) {
                processed.append(html.substring(pos));
                break;
            }

            processed.append(html, pos, tagStart);
            int tagEnd = html.indexOf(">", tagStart);
            if (tagEnd == -1) {
                processed.append(html.substring(tagStart));
                break;
            }

            String tag = html.substring(tagStart, tagEnd + 1);
            String tagName = tag.substring(1).split("\\s|>|/>")[0].toLowerCase();
            if (tagName.equalsIgnoreCase("script")) {
                String end = "</script>";
                String tmp = html.substring(tagEnd + 1);
                String value = tmp.substring(0, tmp.indexOf(end));
                processed.append(tag);
                if (!value.trim().isEmpty()) {
                    if (!value.trim().startsWith("//<![CDATA[")) {
                        processed.append("\n").append("//<![CDATA[\n");
                    }
                    processed.append(value.trim());
                    if (!value.trim().endsWith("//]]>")) {
                        processed.append("\n").append("//]]>\n");
                    }
                }
                processed.append(end);
                pos = tagEnd + 1 + value.length() + end.length();
                continue;
            }

            if (voidElements.contains(tagName)) {
                // Ensure void element is self-closing
                if (!tag.endsWith("/>")) {
                    processed.append(tag, 0, tag.length() - 1).append(" />");
                } else {
                    processed.append(tag, 0, tag.length() - 2).append(" />");
                }
            } else {
                processed.append(tag);
            }

            pos = tagEnd + 1;
        }

        // Add HTML5 doctype if not present
        if (!html.toLowerCase().contains("<!doctype")) {
            return HTML5_DOCTYPE + "\n" + processed;
        }

        // Convert special characters to XML entities, avoiding double encoding
        return processed.toString();
    }

    private static final class SingletonHolder {
        static final ScriptEngineManager manager = new ScriptEngineManager();
    }
}