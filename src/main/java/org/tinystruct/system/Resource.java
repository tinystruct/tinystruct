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
package org.tinystruct.system;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class Resource {
    private final static Logger logger = Logger.getLogger(Resource.class.getName());

    private ResourceBundle resource;
    private static final Map<String, Resource> map = new ConcurrentHashMap<>();

    private Resource() {
    }

    private Resource(Locale locale) {
        this.resource = ResourceBundle.getBundle("languages/lang", locale);
    }

    private Resource(String language, String country) {
        this.resource = ResourceBundle.getBundle("languages/lang", new Locale(language, country));
    }

    public static Resource getInstance(Locale locale) {
        if (map.containsKey(locale.toString())) {
            return map.get(locale.toString());
        }

        // System.setProperty("file.encoding", "utf-8");
        Resource instance = new Resource(locale);
        map.put(locale.toString(), instance);

        return instance;
    }

    public static Resource getInstance(String language) {
        if (map.containsKey(language)) {
            return map.get(language);
        }

        String[] lang = language.split("_");

        Resource instance = new Resource(lang[0], lang[1]);
        map.put(language, instance);

        return instance;
    }

    public String getLocaleString(String field) {
        try {
            return this.resource.getString(field);
        } catch (Exception e) {
            logger.info(e.getMessage() + " key:" + field);
        }

        return "";
    }

}
