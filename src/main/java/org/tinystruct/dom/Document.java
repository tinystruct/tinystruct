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
package org.tinystruct.dom;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML IO reading and writing utility.
 *
 * @author James Zhou
 */
public class Document extends DefaultHandler {
    private static final Logger LOG = Logger.getLogger(Document.class.getName());
    private static final String DOCTYPE_CONFIGURATION = "org/tinystruct/application/application-1.0.dtd";
    private static final String XHTML_TRANSITIONAL_DOCTYPE_CONFIGURATION = "org/tinystruct/application/application-1.0.dtd";
    private static final String XHTML_STRICT_DOCTYPE_CONFIGURATION = "org/tinystruct/application/application-1.0.dtd";
    private static final Logger logger = Logger.getLogger(Document.class.getName());

    // HTML DOCTYPE declarations
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

    private static final Map<String, String> doctypeMap = new HashMap<>();
    private static final Set<String> voidElements = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    ));

    static {
        doctypeMap.put("-//development.tinystruct.org//DTD APPLICATION Configuration 2.0//EN",
                DOCTYPE_CONFIGURATION);
        doctypeMap.put("http://development.tinystruct.org/dtd/application-1.0.dtd",
                DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD XHTML 1.0 Transitional//EN",
                XHTML_TRANSITIONAL_DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD XHTML 1.0 Strict//EN",
                XHTML_STRICT_DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD HTML 4.01 Transitional//EN",
                XHTML_TRANSITIONAL_DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD HTML 4.01//EN",
                XHTML_STRICT_DOCTYPE_CONFIGURATION);
    }

    // Buffer for collecting data from
    // the "characters" SAX event.
    private final CharArrayWriter contents = new CharArrayWriter();
    // Top level element (Used to hold everything else)
    private Element rootElement;
    // The current element you are working on
    private Element currentElement;
    private URL url = null;
    private DocumentType documentType = DocumentType.XML;
    private boolean preserveWhitespace = false;

    public Document(URL url) {
        super();
        this.url = url;
    }

    // setup and load constructor
    public Document() {
        this.currentElement = null;
        this.rootElement = null;
    }

    /**
     * Creates a XmlIO object with the specified element at the top.
     *
     * @param element the element at the top.
     */
    public Document(Element element) {
        this.rootElement = element;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public boolean load() {
        return load(url);
    }

    public boolean load(String file) throws ApplicationException {
        try (InputStream input = new FileInputStream(file)) {
            return load(input);
        } catch (FileNotFoundException notFoundException) {
            throw new ApplicationException(notFoundException.getMessage(),
                    notFoundException);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Loads from the InputStream into the root Xml Element.
     *
     * @param input the input stream to load from.
     * @return boolean
     */
    public boolean load(InputStream input) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            // Configure parser for HTML/XML handling
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            // Set features for more lenient HTML parsing
            if (documentType != DocumentType.XML) {
                factory.setFeature("http://xml.org/sax/features/namespaces", false);
                factory.setFeature("http://xml.org/sax/features/validation", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }

            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(input, this);
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            LOG.severe("Parser configuration error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (SAXException ex) {
            LOG.severe("Parse error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (IOException ex) {
            LOG.severe("I/O error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        return true;
    }

    /**
     * Load a file. This is what starts things off.
     *
     * @param inputURL the URL to load XML from.
     * @return boolean
     */
    public boolean load(URL inputURL) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputURL.toString(), this);
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            LOG.severe("XML config error while attempting to read XML file \n'"
                    + inputURL + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (SAXException ex) {
            LOG.severe("XML parse error while attempting to read XML file \n'"
                    + inputURL + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (IOException ex) {
            LOG.severe("I/O error while attempting to read XML file \n'"
                    + inputURL + "'");
            LOG.severe(ex.getMessage());
            return false;
        }

        return true;
    }

    public boolean read(String text) throws ApplicationException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new ByteArrayInputStream(text.getBytes()), this);
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            LOG.severe("XML config ParserConfigurationException error while attempting to read XML text");
            LOG.severe(ex.getMessage());
            throw new ApplicationException(ex.getMessage(), ex);
        } catch (SAXException ex) {
            LOG.severe("XML config SAXException error while attempting to read XML text");
            LOG.severe(ex.getMessage());
            throw new ApplicationException(ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.severe("XML config IOException error while attempting to read XML text");
            LOG.severe(ex.getMessage());
            throw new ApplicationException(ex.getMessage(), ex);
        }

        return true;

    }

    /**
     * Set the document type for proper DOCTYPE handling and formatting.
     *
     * @param type The document type to use
     */
    public void setDocumentType(DocumentType type) {
        this.documentType = type;
    }

    /**
     * Set whether to preserve whitespace in text content.
     *
     * @param preserve True to preserve whitespace, false to trim
     */
    public void setPreserveWhitespace(boolean preserve) {
        this.preserveWhitespace = preserve;
    }

    /**
     * Get the appropriate DOCTYPE declaration for the current document type.
     *
     * @return The DOCTYPE declaration string
     */
    private String getDocTypeDeclaration() {
        switch (documentType) {
            case HTML5:
                return HTML5_DOCTYPE;
            case HTML4_TRANSITIONAL:
                return HTML4_TRANSITIONAL_DOCTYPE;
            case HTML4_STRICT:
                return HTML4_STRICT_DOCTYPE;
            case XHTML1_TRANSITIONAL:
                return XHTML1_TRANSITIONAL_DOCTYPE;
            case XHTML1_STRICT:
                return XHTML1_STRICT_DOCTYPE;
            default:
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attrs) throws SAXException {
        try {
            contents.reset();
            String name = localName;
            if ("".equals(name)) {
                name = qName; // namespaceAware = false
            }

            // Convert tag names to lowercase for HTML
            if (documentType != DocumentType.XML) {
                name = name.toLowerCase();
            }

            // Handle special HTML elements
            if (documentType != DocumentType.XML && name.equals("html")) {
                // Add default HTML attributes if not present
                if (this.rootElement == null) {
                    this.rootElement = new Element(name);
                    this.currentElement = this.rootElement;

                    // Add default HTML attributes
                    if (attrs == null || attrs.getLength() == 0) {
                        switch (documentType) {
                            case XHTML1_STRICT:
                            case XHTML1_TRANSITIONAL:
                                this.currentElement.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
                                this.currentElement.setAttribute("xml:lang", "en");
                                this.currentElement.setAttribute("lang", "en");
                                break;
                            case HTML5:
                                this.currentElement.setAttribute("lang", "en");
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    this.currentElement = this.currentElement.addElement(name);
                }
            } else if (this.rootElement == null) {
                this.rootElement = new Element(name);
                this.currentElement = this.rootElement;
            } else {
                this.currentElement = this.currentElement.addElement(name);
            }

            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String aName = attrs.getLocalName(i);
                    if ("".equals(aName)) {
                        aName = attrs.getQName(i);
                    }
                    // Convert attribute names to lowercase for HTML
                    if (documentType != DocumentType.XML) {
                        aName = aName.toLowerCase();
                    }
                    this.currentElement.setAttribute(aName, attrs.getValue(i));
                }
            }
        } catch (java.lang.NullPointerException ex) {
            LOG.severe(ex.getMessage());
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (this.currentElement != null) {
            String content = preserveWhitespace ? contents.toString() : contents.toString().trim();

            // Special handling for script and style tags in HTML
            if (documentType != DocumentType.XML &&
                    (this.currentElement.getName().equalsIgnoreCase("script") ||
                            this.currentElement.getName().equalsIgnoreCase("style"))) {
                content = contents.toString(); // Preserve exact content
            }

            this.currentElement.setData(content);
            contents.reset();
            this.currentElement = this.currentElement.getParent();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        // Accumulate the contents into a buffer
        if (documentType != DocumentType.XML &&
                this.currentElement != null &&
                (this.currentElement.getName().equalsIgnoreCase("script") ||
                        this.currentElement.getName().equalsIgnoreCase("style"))) {
            // For script and style tags, preserve exact content
            contents.write(ch, start, length);
        } else {
            // For other tags, normalize whitespace if not preserving
            String text = new String(ch, start, length);
            if (!preserveWhitespace) {
                text = text.replaceAll("\\s+", " ");
            }
            contents.write(text.toCharArray(), 0, text.length());
        }
    }

    /**
     * Returns the root for the Element hierarchy.
     * <p>
     * Methods that want to retrieve elements from this root should use the
     * {@link Element} in order to get the wanted
     * element.
     *
     * @return an Element if it has been loaded or initialized with it; null
     * otherwise.
     */
    public Element getRoot() {
        return this.rootElement;
    }

    public void save() throws Exception {
        try (FileOutputStream out = new FileOutputStream(url.getPath())) {
            save(out);
        }
    }

    /**
     * Save the contents of this Element with appropriate formatting
     *
     * @param out the output stream
     */
    public void save(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        // Write DOCTYPE declaration
        writer.write(getDocTypeDeclaration());
        writer.write('\n');

        if (!rootElement.getChildNodes().isEmpty()) {
            if (documentType != DocumentType.XML) {
                writer.write(rootElement.toHtml(documentType, voidElements));
            } else {
                writer.write(rootElement.toString());
            }
        }

        writer.flush();
    }

    /**
     * Save as HTML file with specified document type
     *
     * @param file The file to save to
     * @param type The HTML document type
     * @throws ApplicationException If there is an error saving the file
     */
    public void saveAsHtml(File file, DocumentType type) throws ApplicationException {
        this.documentType = type;
        try (FileOutputStream out = new FileOutputStream(file)) {
            save(out);
        } catch (IOException e) {
            throw new ApplicationException("Failed to save HTML file: " + file.getPath(), e);
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        InputSource source;

        try {
            String path = doctypeMap.get(publicId);
            source = getInputSource(path, null);
            if (source != null) {
                path = doctypeMap.get(systemId);
                source = getInputSource(path, source);
            }
        } catch (Exception e) {
            throw new SAXException(e.toString());
        }

        return source;
    }

    private InputSource getInputSource(String path, InputSource source) {
        if (path != null) {
            try (InputStream in = Resources.getResourceAsStream(path)) {
                source = new InputSource(in);
            } catch (IOException e) {
                LOG.severe(e.getMessage());
            }
        }

        return source;
    }

    /**
     * Load HTML content from a string.
     *
     * @param html The HTML string to parse
     * @param type The HTML document type
     * @return true if parsing was successful
     * @throws ApplicationException if parsing fails
     */
    public String loadHtml(String html, DocumentType type) throws ApplicationException {
        this.documentType = type;

        try {
            // Pre-process HTML to make it more XML-like
            html = preprocessHtml(html);

            // Configure parser
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);

            // Disable DTD loading and validation
            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException e) {
                // Ignore if features are not supported
                logger.log(Level.WARNING, "Could not configure parser features", e);
            }

            // Parse the HTML
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(html)), this);
            return html;
        } catch (Exception ex) {
            throw new ApplicationException("Failed to parse HTML: " + ex.getMessage(), ex);
        }
    }

    /**
     * Preprocess HTML to make it more XML-like
     *
     * @param html The HTML string to process
     * @return Processed HTML string
     */
    private String preprocessHtml(String html) {
        // Auto-close unclosed tags
        String[] tags = html.split("<|</");
        StringBuilder autoClosedHtml = new StringBuilder();
        Set<String> openTags = new HashSet<>();

        for (String tag : tags) {
            if (tag.trim().isEmpty()) continue;
            int endIndex = tag.indexOf(">");
            if (endIndex == -1) {
                autoClosedHtml.append("<").append(tag);
                continue;
            }

            String tagName = tag.substring(0, endIndex).split(" |/>")[0].toLowerCase();
            boolean isSelfClosing = tag.endsWith("/>") || voidElements.contains(tagName);

            if (!tagName.isEmpty() && !tag.startsWith("/") && !isSelfClosing) {
                if (tagName.indexOf(' ') != -1)
                    openTags.add(tagName.substring(0, tagName.indexOf(' ')));
                else
                    openTags.add(tagName);
            } else if (!tagName.isEmpty() && tag.startsWith("/")) {
                openTags.remove(tagName.substring(1));
            }

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
        boolean hasHtmlTag = html.toLowerCase().contains("<html>");
        boolean hasBodyTag = html.toLowerCase().contains("<body>");

        if (!hasHtmlTag) {
            html = "<html>" + html + "</html>";
        }
        if (!hasBodyTag) {
            html = html.replaceFirst("(<html[^>]*>)", "$1<body>")
                    .replaceFirst("(</html>)", "</body>$1");
        }

        // Add HTML5 doctype if not present
        if (!html.toLowerCase().contains("<!doctype")) {
            html = HTML5_DOCTYPE + "\n" + html;
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

            processed.append(html.substring(pos, tagStart));
            int tagEnd = html.indexOf(">", tagStart);
            if (tagEnd == -1) {
                processed.append(html.substring(tagStart));
                break;
            }

            String tag = html.substring(tagStart, tagEnd + 1);
            String tagName = tag.substring(1).split("[ >]")[0].toLowerCase();

            if (voidElements.contains(tagName)) {
                // Ensure void element is self-closing
                if (!tag.endsWith("/>")) {
                    processed.append(tag.substring(0, tag.length() - 1)).append("/>");
                } else {
                    processed.append(tag);
                }
            } else {
                processed.append(tag);
            }

            pos = tagEnd + 1;
        }
        // Convert special characters to XML entities, avoiding double encoding
        return escapeTextOnly(processed.toString());
    }

    private String escapeTextOnly(String html) {
        StringBuilder result = new StringBuilder();
        boolean insideTag = false;

        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);

            if (c == '<') {
                insideTag = true;
                result.append(c);
            } else if (c == '>') {
                insideTag = false;
                result.append(c);
            } else if (insideTag) {
                result.append(c);
            } else {
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
                    case '"':
                        result.append("&quot;");
                        break;
                    case '\'':
                        result.append("&apos;");
                        break;
                    default:
                        result.append(c);
                }
            }
        }

        return result.toString();
    }
}