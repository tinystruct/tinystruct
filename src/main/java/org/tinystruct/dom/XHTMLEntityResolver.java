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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XHTMLEntityResolver implements EntityResolver {

    private static final Map<String, String> mapping = new HashMap<String, String>();

    static {
        mapping.put(XHTML.Transitional.PUBLIC,
                "org/mover/services/application/xhtml1-transitional.dtd");
        mapping.put(XHTML.Strict.PUBLIC,
                "org/mover/services/application/xhtml1-strict.dtd");
        mapping.put(XHTML.Frameset.PUBLIC,
                "org/mover/services/application/xhtml1-frameset.dtd");
        mapping.put(XHTML.Latin.PUBLIC,
                "org/mover/services/application/xhtml-lat1.ent");
        mapping.put(XHTML.Symbols.PUBLIC,
                "org/mover/services/application/xhtml-symbol.ent");
        mapping.put(XHTML.Special.PUBLIC,
                "org/mover/services/application/xhtml-special.ent");
    }

    /**
     * Converts a public DTD into a local one
     *
     * @param publicId Unused but required by EntityResolver interface
     * @param systemId The DTD that is being requested
     * @return The InputSource for the DTD
     * @throws org.xml.sax.SAXException If anything goes wrong
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {

        if (mapping.containsKey(publicId))
            try {
                return this.getInputSource(mapping.get(publicId));
            } catch (ApplicationException e) {
                System.out.println(e.getMessage());
            }

        return null;
    }

    private InputSource getInputSource(String path) throws ApplicationException {

        InputSource source = null;
        if (path != null) {
            try {
                if (path.startsWith("http://")) {
                    source = new InputSource(Resources.getUrlAsStream(path));
                } else
                    source = new InputSource(Resources
                            .getResourceAsStream(path));
            } catch (IOException e) {

                e.printStackTrace();
                throw new ApplicationException(e.getMessage(), e);
            }
        }
        return source;
    }
}
