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
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Resource;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.template.DefaultTemplate;
import org.tinystruct.system.template.PlainText;
import org.tinystruct.system.template.variable.ObjectVariable;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.StringUtilities;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Logger;

/**
 * AbstractApplication provides some common methods for a standard {link:Application}.
 *
 * @author James Zhou
 */
public abstract class AbstractApplication implements Application, Cloneable {

    private static final Logger logger = Logger.getLogger(AbstractApplication.class.getName());
    /**
     * Application instances container.
     */
    private static final Container CONTAINER = Container.getInstance();

    protected final Map<String, CommandLine> commandLines;
    private final Actions actions = Actions.getInstance();
    private final String name;
    private final Variables variables;
    private final Map<String, Variable<?>> shared;
    /**
     * Context of application
     */
    protected Context context;

    /**
     * Configuration
     */
    protected Configuration<String> config;
    private String output;
    private boolean templateRequired = true;

    /**
     * Abstract application constructor.
     */
    public AbstractApplication() {
        this.name = getClass().getName();
        this.shared = Variables.getInstance();
        this.variables = new Variables();
        this.commandLines = new HashMap<String, CommandLine>();
    }

    /**
     * Return if template is required or not.
     *
     * @return templateRequired boolean
     */
    public boolean isTemplateRequired() {
        return templateRequired;
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
        if (this.context.getAttribute(LANGUAGE) != null) {
            this.setLocale(this.context.getAttribute(LANGUAGE).toString());
        } else {
            this.setLocale(this.config.get(DEFAULT_LANGUAGE));
        }

        try {
            String key = context.getId() + this.getVariable(LANGUAGE).getValue() + File.separatorChar + this.getName();
            if (!CONTAINER.containsKey(key)) {
                AbstractApplication clone = (AbstractApplication) this.clone();
                CONTAINER.put(key, clone);
            }
        } catch (CloneNotSupportedException e) {
            throw new ApplicationRuntimeException(e.toString(), e.getCause());
        }
    }

    public Application getInstance(String contextId) {
        return CONTAINER.get(contextId + this.getVariable(LANGUAGE).getValue() + File.separatorChar + this.getName());
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

    public String setTemplate(Template template) throws ApplicationException {
        // When the template has not been disabled or the locale does not
        // compared.
        return template.parse();
    }

    public Configuration<String> getConfiguration() {
        return this.config;
    }

    public void setConfiguration(Configuration<String> config) {
        config.set(CLSID, this.name);
        config.set(DEFAULT_LANGUAGE, "zh_CN");
        config.set(LANGUAGE, config.get(DEFAULT_LANGUAGE));
        config.set(CHARSET, "utf-8");
        config.set(DEFAULT_BASE_URL, "/?q=");

        this.setLocale(config.get(DEFAULT_LANGUAGE));

        this.config = config;

        // Only to be initialized once.
        this.init();

        this.setAction("--help", "help");

        if (this.commandLines.get("--help") != null)
            this.commandLines.get("--help").setDescription("Help command");
    }

    public String getName() {
        return this.name;
    }

    public Object invoke(String path) throws ApplicationException {
        return this.invoke(path, null);
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

        if (parameters == null) {
            return action.execute();
        }

        return action.execute(parameters);
    }

    public Context getContext() {
        return this.context;
    }

    public void setVariable(Variable<?> variable, boolean force) {
        this.variables.setVariable(variable, force);
    }

    public void setVariable(String name, String value) {
        this.setVariable(name, value, true);
    }

    public void setVariable(String name, String value, boolean force) {
        if (value == null) value = "";
        StringVariable variable = new StringVariable(name, value);
        this.setVariable(variable, force);
    }

    public Variable<?> getVariable(String variable) {
        return this.variables.getVariable(variable);
    }

    public void setSharedVariable(String name, String value) {
        this.variables.setSharedVariable(new StringVariable(name, value), true);
    }

    public String setText(String fieldName) {
        String text = this.getProperty(fieldName);
        String key = "[%" + fieldName + "%]";
        this.shared.put(key, new StringVariable(key, text));
        return text;
    }

    public String setText(String fieldName, Object... args) {
        String text = String.format(this.getProperty(fieldName), args);
        String key = "[%" + fieldName + "%]";
        this.shared.put(key, new StringVariable(key, text));
        return text;
    }

    private void setLink(String name) {
        String key = "[%LINK:" + name + "%]";
        if (!this.shared.containsKey(key)) {
            this.shared.put(key, new StringVariable(key, name));
        }
    }

    /**
     * Get a link.
     *
     * @param variable variable
     * @return link string
     */
    public String getLink(String variable) {
        String linkName = "[%LINK:" + variable + "%]";
        if (this.shared.get(linkName) != null) {
            String baseUrl;
            if (this.getContext() != null && this.getContext().getAttribute("HTTP_HOST") != null)
                baseUrl = this.getContext().getAttribute("HTTP_HOST").toString();
            else
                baseUrl = this.config.get(DEFAULT_BASE_URL);
            return baseUrl + this.shared.get(linkName).getValue();
        }
        return "#";
    }

    /**
     * Get the specific configuration with property name.
     *
     * @param propertyName property name
     * @return A string value of the property
     */
    public String getConfiguration(String propertyName) {
        return this.config.get(propertyName);
    }

    public String getProperty(String propertyName) {
        Resource resource = Resource.getInstance(getLocale());
        return resource.getLocaleString(propertyName);
    }

    public String getProperty(String propertyName, Locale locale) {
        Resource resource = Resource.getInstance(locale);
        return resource.getLocaleString(propertyName);
    }

    public Locale getLocale() {
        return (Locale) this.getVariable("locale").getValue();
    }

    private void setLocale(String locale) {
        String[] local = locale.split("_");
        Locale _locale = new Locale(local[0], local[1]);
        this.setLocale(_locale);
    }

    public void setLocale(Locale locale) {
        this.setVariable(LANGUAGE_CODE, locale.getLanguage());
        this.setVariable(LANGUAGE, locale.toString());
        this.setVariable(new ObjectVariable("locale", locale), true);
    }

    @Override
    public String toString() {
        if (!this.templateRequired) return this.name + "@" + Integer.toHexString(hashCode());

        InputStream in = null;
        String simpleName = this.getName().substring(this.getName().lastIndexOf('.') + 1);
        Locale locale = this.getLocale();
        String templatePath = "UNKNOWN";
        if (locale != Locale.CHINA) {
            templatePath = "themes" + File.separatorChar + simpleName + "_" + locale.toString() + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(templatePath);
        }

        if (null == in) {
            templatePath = "themes" + File.separatorChar + simpleName + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(templatePath);
        }

        if (null != in) {
            try {
                return this.setTemplate(new DefaultTemplate(this, in, this.variables.getVariables()));
            } catch (ApplicationException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e);
            }
        } else {
            if (this.output != null && this.output.trim().length() > 0) {
                try {
                    String output = this.setTemplate(new PlainText(this, this.output, this.variables.getVariables()));
                    return output.replace("[%", "").replace("%]", "");
                } catch (ApplicationException e) {
                    throw new ApplicationRuntimeException(e.getMessage(), e);
                }
            }
        }

        throw new ApplicationRuntimeException("The template " + templatePath + " could not be found and the output has not been set.");
    }

    public CommandLine setCommandLine(CommandLine command) {
        return this.commandLines.put(command.getCommand(), command);
    }

    public void run() {
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