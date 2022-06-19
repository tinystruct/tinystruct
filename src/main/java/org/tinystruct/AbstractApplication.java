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
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Resource;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.template.DefaultTemplate;
import org.tinystruct.system.template.PlainText;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.StringUtilities;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * AbstractApplication provides some common methods for a standard {link:Application}.
 *
 * @author James Zhou
 */
public abstract class AbstractApplication implements Application {

    private final static Logger logger = Logger.getLogger(AbstractApplication.class.getName());
    private final Actions actions = Actions.getInstance();
    private final String name;
    private final Map<String, Variable<?>> variables;
    /**
     * Context of application
     */
    protected Context context;
    protected Map<String, CommandLine> commandLines;
    /**
     * Configuration
     */
    protected Configuration<String> config;
    private Locale locale;
    private String output;
    private boolean templateRequired = true;
    private String templatePath;

    /**
     * Abstract application constructor.
     */
    public AbstractApplication() {
        this.name = getClass().getName();
        this.variables = Variables.getInstance();
        this.commandLines = new HashMap<String, CommandLine>();
    }

    /**
     * Set template to be required or not.
     *
     * @param templateRequired boolean
     */
    public void setTemplateRequired(boolean templateRequired) {
        this.templateRequired = templateRequired;
    }

    public void init(Context context) {
        this.context = context;
        if (this.context.getAttribute(LANGUAGE) != null
                && !this.context.getAttribute(LANGUAGE).toString()
                .equalsIgnoreCase(this.config.get(DEFAULT_LANGUAGE))) {
            this.config.set(LANGUAGE, this.context.getAttribute(LANGUAGE).toString());
        } else {
            this.config.set(LANGUAGE, this.config.get(DEFAULT_LANGUAGE));
        }

        this.setLocale(this.config.get(LANGUAGE));
    }

    public Actions actions() {
        return this.actions;
    }

    public void setAction(String path, Action action) {
        this.actions.set(action);

        // Exclude the command start with '-'
        if (path.indexOf("-") != 0)
            this.setLink(path);
    }

    public void setAction(String path, String function) {
        this.actions.set(this, path, function);

        // Exclude the command start with '-'
        if (path.indexOf("-") != 0)
            this.setLink(path);
    }

    public void setAction(String path, String function, String method) {
        this.actions.set(this, path, function, method);

        // Exclude the command start with '-'
        if (path.indexOf("-") != 0)
            this.setLink(path);
    }

    public String getOutputText() {
        return this.output;
    }

    public void setOutputText(String buffer) {
        this.output = buffer;
    }

    public void setTemplate(Template template) throws ApplicationException {
        // When the template has not been disabled or the locale does not
        // compared.
        this.output = template.parse();
    }

    public Configuration<String> getConfiguration() {
        return this.config;
    }

    public void setConfiguration(Configuration<String> config) {
        this.config = config;
        this.config.set(CLSID, this.name);
        this.config.set(DEFAULT_LANGUAGE, "zh_CN");
        this.config.set(LANGUAGE, this.config.get(DEFAULT_LANGUAGE));
        this.config.set(CHARSET, "utf-8");
        this.config.set(DEFAULT_BASE_URL, "/?q=");

        // Only to be initialized once.
        this.init();

        this.setLocale(this.config.get(LANGUAGE));
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
            throw new ApplicationException("Action [" + path + "] has not been registered.");
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

    public Variable<?> getVariable(String variable) {
        return this.variables.get("{%" + variable + "%}");
    }

    public String setText(String fieldName) {
        String text = this.getProperty(fieldName);
        String key = "[%" + fieldName + "%]";
        this.variables.put(key, new StringVariable(key, text));
        return text;
    }

    public String setText(String fieldName, Object... args) {
        String text = String.format(this.getProperty(fieldName), args);
        String key = "[%" + fieldName + "%]";
        this.variables.put(key, new StringVariable(key, text));
        return text;
    }

    private void setLink(String name) {
        String key = "[%LINK:" + name + "%]";
        if (!this.variables.containsKey(key)) {
            this.variables.put(key, new StringVariable(key, name));
        }
    }

    /**
     * Get a link.
     *
     * @param variable variable
     * @return
     */
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

    /**
     * Get the specific configuration with property name.
     *
     * @param propertyName property name
     * @return
     */
    public String getConfiguration(String propertyName) {
        return this.config.get(propertyName);
    }

    public String getProperty(String propertyName) {
        Resource resource = Resource.getInstance(this.locale);
        return resource.getLocaleString(propertyName);
    }

    public Locale getLocale() {
        return this.locale;
    }

    private void setLocale(String locale) {
        String[] local = locale.split("_");
        this.setLocale(new Locale(local[0], local[1]));
        this.setVariable(LANGUAGE_CODE, local[0]);
        this.setVariable(LANGUAGE, this.locale.toString());
    }

    public void setLocale(Locale locale) {
        this.locale = locale;

        this.setAction("--help", "help");

        if (this.commandLines.get("--help") != null)
            this.commandLines.get("--help").setDescription("Help command");
    }

    public String toString() {
        if (!this.templateRequired) return null;

        InputStream in = null;
        String simpleName = this.getName().substring(this.getName().lastIndexOf('.') + 1);
        if (locale != Locale.CHINA) {
            this.templatePath = "themes" + File.separatorChar + simpleName + "_" + locale.toString() + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(this.templatePath);
        }

        if (null == in) {
            this.templatePath = "themes" + File.separatorChar + simpleName + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(this.templatePath);
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

        throw new ApplicationRuntimeException("The template " + this.templatePath + " could not be found and the output has not been set.");
    }

    public CommandLine setCommandLine(CommandLine command) {
        return this.commandLines.put(command.getCommand(), command);
    }

    public void run() {
        throw new ApplicationRuntimeException("The method has not been implemented yet.");
    }

    @Override
    public String help() {
        StringBuilder builder = new StringBuilder("Usage: bin/dispatcher COMMAND [OPTIONS]\n");

        StringBuilder commands = new StringBuilder("Commands: \n");
        StringBuilder options = new StringBuilder("Options: \n");

        OptionalInt longSizeCommand = this.commandLines.keySet().stream().mapToInt(String::length).max();
        int max = longSizeCommand.orElse(0);

        this.commandLines.forEach((s, commandLine) -> {
            String command = commandLine.getCommand();
            String description = commandLine.getDescription();
            if (command.startsWith("--")) {
                options.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            } else {
                commands.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }
        });

        builder.append(commands).append("\n");
        builder.append(options);

        return builder.toString();
    }

    public Map<String, CommandLine> getCommandLines() {
        return this.commandLines;
    }
}