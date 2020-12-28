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
package org.tinystruct;

import org.tinystruct.application.*;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Resource;
import org.tinystruct.system.Settings;
import org.tinystruct.system.template.DefaultTemplate;
import org.tinystruct.system.template.PlainText;
import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractApplication implements Application {

    private final Actions actions = Actions.getInstance();
    protected Context context;
    private String name;
    private final Map<String, Variable<?>> variables;
    private Locale locale;
    private Resource resource;
    private final static Logger logger = Logger
            .getLogger("AbstractApplication.class");
    private String output;
    private Document document;

    public AbstractApplication() {
        this.name = getClass().getName();
        this.variables = Variables.getInstance();

        this.setConfiguration(new Settings());
    }

    private void setLocale(String locale) {
        String[] local = locale.split("_");
        this.locale = new Locale(local[0], local[1]);
        this.setVariable(LANGUAGE_CODE, local[0]);
        this.setVariable(LANGUAGE, locale);
        this.init();
    }

    public void init(Context context) {
        this.context = context;
        if (this.context.getAttribute(LANGUAGE) != null
                && !this.context.getAttribute(LANGUAGE).toString()
                .equalsIgnoreCase(this.config.get(DEFAULT_LANGUAGE).toString())) {
            this.config.set(LANGUAGE, this.context.getAttribute(LANGUAGE).toString());
        } else {
            this.config.set(LANGUAGE, this.config.get(DEFAULT_LANGUAGE).toString());
        }

        this.setLocale(this.config.get(LANGUAGE).toString());
    }

    public Actions actions() {
        return this.actions;
    }

    public void setAction(String action, String function) {
        this.actions.set(this, action, function);
        this.setLink(action);
    }

    public void setAction(String action, String function, String method) {
        this.actions.set(this, action, function, method);
        this.setLink(action);
    }

    protected Configuration<String> config;

    private String template_path;

    public String getOutputText() {
        return this.output;
    }

    public void setOutputText(String buffer) {
        this.output = buffer;
    }

    public Document getDocument() {
        return this.document;
    }

    public void setTemplate(Template template) throws ApplicationException {
        // When the template has not been disabled or the locale does not
        // compared.
        this.output = template.parse();
    }

    public void setConfiguration(Configuration<String> config) {
        this.config = config;
        this.config.set(CLSID, this.name);
        this.config.set(DEFAULT_LANGUAGE, "zh_CN");
        this.config.set(LANGUAGE, this.config.get(DEFAULT_LANGUAGE));
        this.config.set(CHARSET, "utf-8");
        this.config.set(DEFAULT_BASE_URL, "/?q=");
        this.setLocale(this.config.get(LANGUAGE));
    }

    public Configuration<String> getConfiguration() {
        return this.config;
    }

    public String getName() {
        return this.name;
    }

    public Object invoke(String path) throws ApplicationException {
        String method = null;
        if (context != null && context.getAttribute(METHOD) != null) {
            method = context.getAttribute(METHOD).toString();
        }
        Action action = this.actions.getAction(path, method);
        if (action == null) {
            int pos = -1;
            String tpath = path;
            while ((pos = tpath.lastIndexOf('/')) != -1) {
                tpath = tpath.substring(0, pos);
                action = this.actions.getAction(tpath, method);
                if (action != null) {
                    String arg = path.substring(pos + 1);
                    String[] args = arg.split("/");
                    if (context != null) {
                        context.setAttribute(REQUEST_ACTION, path);
                        action.setContext(context);
                    }

                    return action.execute(args);
                }
            }
            throw new ApplicationException("Action " + action
                    + " path does not registered.");
        }
        if (context != null) {
            context.setAttribute(REQUEST_ACTION, path);
            action.setContext(context);
        }
        return action.execute();
    }

    public Object invoke(String path, Object[] parameters)
            throws ApplicationException {
        String method = null;
        if (context != null && context.getAttribute(METHOD) != null) {
            method = context.getAttribute(METHOD).toString();
        }
        Action action = this.actions.getAction(path, method);
        if (action == null)
            throw new ApplicationException("Action " + path
                    + " path does not registered.");
        return action.execute(parameters);
    }

    public Context getContext() {
        return this.context;
    }

    public void setVariable(Variable<?> variable, boolean forcely) {
        String variableName = "{%" + variable.getName() + "%}";
        if (forcely || !this.variables.containsKey(variableName)) {
            if (variable.getType() == DataType.OBJECT) {
                Builder builder = new Builder();
                try {
                    builder.parse(variable.getValue().toString());
                    Set<String> elements = builder.keySet();
                    Iterator<String> list = elements.iterator();
                    String key;
                    while (list.hasNext()) {
                        key = list.next();
                        this.variables.put("{%" + variable.getName() + "."
                                + key + "%}", new StringVariable("{%"
                                + variable.getName() + "." + key + "%}",
                                (String) builder.get(key)));
                    }
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
            }
            this.variables.put(variableName, variable);
        }
    }

    public void setVariable(String name, String value) {
        this.setVariable(name, value, true);
    }

    public void setVariable(String name, String value, boolean forced) {
        if (value == null) value = "";
        StringVariable variable = new StringVariable(name, value);
        this.setVariable(variable, forced);
    }

    public Variable getVariable(String variable) {
        return this.variables.get("{%" + variable + "%}");
    }

    public String setText(String fieldName) {
        String text = this.getProperty(fieldName);
        String key = "[%" + fieldName + "%]";
        if (this.variables.containsKey(key)) {
            this.variables.remove(key);
        }
        this.variables.put(key, new StringVariable(key, text));
        return text;
    }

    public String setText(String fieldName, Object... args) {
        String text = String.format(this.getProperty(fieldName), args);
        String key = "[%" + fieldName + "%]";
        if (this.variables.containsKey(key)) {
            this.variables.remove(key);
        }
        this.variables.put(key, new StringVariable(key, text));
        return text;
    }

    private void setLink(String name) {
        String key = "[%LINK:" + name + "%]";
        if (!this.variables.containsKey(key)) {
            this.variables.put(key, new StringVariable(key, name));
        }
    }

    public String getLink(String variable) {
        String linkName = "[%LINK:" + variable + "%]";
        if (this.variables.get(linkName) != null) {
            String baseUrl;
            if (this.getContext() != null && this.getContext().getAttribute("HTTP_HOST") != null)
                baseUrl = this.getContext().getAttribute("HTTP_HOST").toString();
            else
                baseUrl = this.config.get(DEFAULT_BASE_URL);
            return baseUrl + this.variables.get(linkName).getValue();
        }
        return "#";
    }

    public String getConfiguration(String propertyName) {
        return this.config.get(propertyName);
    }

    public String getProperty(String propertyName) {
        try {
            this.resource = Resource.getInstance(this.locale);
            return this.resource.getLocaleString(propertyName);
        } catch (Exception e) {
            logger.severe("Application view getProperty():" + e.getMessage());
        }
        return "";
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String toString() {
        InputStream in = null;
        String name = this.getName().substring(this.getName().lastIndexOf('.') + 1);
        if (locale != Locale.CHINA) {
            this.template_path = "themes" + File.separatorChar + name + "_" + locale.toString() + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(this.template_path);
        }

        if (null == in) {
            this.template_path = "themes" + File.separatorChar + name + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(this.template_path);
        }

        if (null != in) {
            try {
                this.setTemplate(new DefaultTemplate(this, in));
                return this.output;
            } catch (ApplicationException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e);
            }
        } else {
            if (this.output != null && this.output.trim().length() > 0) {
                try {
                    this.setTemplate(new PlainText(this, this.output));
                } catch (ApplicationException e) {
                    throw new ApplicationRuntimeException(e.getMessage(), e);
                }
                this.output = this.output.replace("[%", "").replace("%]", "");
                return this.output;
            }
        }

        throw new ApplicationRuntimeException("The template " + this.template_path + " could not be found and the output has not been set.");
    }

    public void run() {

    }
}