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

import org.tinystruct.ApplicationException;
import org.tinystruct.system.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

    private static final Map<String, String> doctypeMap = new HashMap<String, String>();

    static {
        doctypeMap
                .put("-//development.tinystruct.org//DTD APPLICATION Configuration 2.0//EN",
                        DOCTYPE_CONFIGURATION);
        doctypeMap.put("http://development.tinystruct.org/dtd/application-1.0.dtd",
                DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD XHTML 1.0 Transitional//EN",
                XHTML_TRANSITIONAL_DOCTYPE_CONFIGURATION);
        doctypeMap.put("-//W3C//DTD XHTML 1.0 Strict//EN",
                XHTML_STRICT_DOCTYPE_CONFIGURATION);
    }

    // Top level element (Used to hold everything else)
    private Element rootElement;
    // The current element you are working on
    private Element currentElement;

    // Buffer for collecting data from
    // the "characters" SAX event.
    private final CharArrayWriter contents = new CharArrayWriter();
    private URL url = null;

    public Document(URL url) {
        super();
        this.url = url;
    }

    // setup and load constructor
    public Document() {
        this.currentElement = null;
        this.rootElement = null;
    }

    // setup and load constructor

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
        try {
            InputStream input = new FileInputStream(file);
            return load(input);
        } catch (FileNotFoundException FileNotFound) {
            throw new ApplicationException(FileNotFound.getMessage(),
                    FileNotFound);
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
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(input, this);
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            LOG.severe("XML config error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (SAXException ex) {
            LOG.severe("XML parse error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        } catch (IOException ex) {
            LOG.severe("I/O error while attempting to read from the input stream \n'"
                    + input + "'");
            LOG.severe(ex.getMessage());
            return false;
        }
        finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
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

    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attrs) throws SAXException {
        // Resetting contents buffer.
        // Assuming that tags either tag content or children, not both.
        // This is usually the case with XML that is representing
        // data strucutures in a programming language independant way.
        // This assumption is not typically valid where XML is being
        // used in the classical text mark up style where tagging
        // is used to style content and several styles may overlap
        // at once.
        try {
            contents.reset();
            String name = localName; // element name
            if (name.equals("")) {
                name = qName; // namespaceAware = false
            }

            if (this.rootElement == null) {
                this.rootElement = new Element(name);
                this.currentElement = this.rootElement;
            } else {
                this.currentElement = this.currentElement.addElement(name);
            }

            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String aName = attrs.getLocalName(i); // Attr name
                    if (aName.equals("")) {
                        aName = attrs.getQName(i);
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
            this.currentElement.setData(contents.toString().trim());
            contents.reset();
            this.currentElement = this.currentElement.getParent();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        // accumulate the contents into a buffer.
        contents.write(ch, start, length);
    }

    /**
     * Returns the root for the Element hierarchy.
     * <p>
     * Methods that want to retrieve elements from this root should use the
     * {@link Element} in order to get the wanted
     * element.
     *
     * @return a Element if it has been loaded or initialized with it; null
     * otherwise.
     */
    public Element getRoot() {
        return this.rootElement;
    }

    public void save() throws Exception {
        save(new FileOutputStream(url.getPath()));
    }

    /**
    * Writer interface
    **/
    public void save(OutputStream out) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(out,
                StandardCharsets.UTF_8));
        bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (rootElement.getChildNodes().size() > 0) {
            bufferedWriter.write(rootElement.toString());
        }
        bufferedWriter.flush();
        out.close();
    }

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        InputSource source = null;

        try {
            String path = (String) doctypeMap.get(publicId);
            source = getInputSource(path, source);
            if (source != null) {
                path = (String) doctypeMap.get(systemId);
                source = getInputSource(path, source);
            }
        } catch (Exception e) {
            throw new SAXException(e.toString());
        }

        return source;
    }

    private InputSource getInputSource(String path, InputSource source) {
        if (path != null) {
            InputStream in = null;
            try {
                in = Resources.getResourceAsStream(path);
                source = new InputSource(in);
            } catch (IOException e) {
                LOG.severe(e.getMessage());
            }
        }
        return source;
    }

}