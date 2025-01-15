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
package org.tinystruct;

import org.tinystruct.application.*;
import org.tinystruct.system.AnnotationProcessor;
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

import static org.tinystruct.http.Constants.HTTP_HOST;

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

    /**
     * Collection of command line.
     */
    protected Map<String, CommandLine> commandLines;

    /**
     * Registry for actions associated with the application
     */
    private final ActionRegistry actionRegistry = ActionRegistry.getInstance();

    /**
     * Class simple name
     */
    private final String name;

    /**
     * Context of application
     */
    protected ThreadLocal<Context> threadLocalContext = ThreadLocal.withInitial(() -> null);

    /**
     * Configuration
     */
    private Configuration<String> config;

    /**
     * Output string
     */
    private String output;

    /**
     * Template required by default
     */
    private boolean templateRequired = true;

    /**
     * Locale information
     */
    private Locale locale;

    /**
     * Abstract application constructor.
     */
    public AbstractApplication() {
        this.name = getClass().getName();
        this.commandLines = new HashMap<>();
    }

    /**
     * Return if template is required or not.
     *
     * @return templateRequired boolean
     */
    @Override
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

    /**
     * Initializes the application with the provided context.
     *
     * @param context The context to initialize the application.
     */
    @Override
    public void init(Context context) {
        this.setContext(context);

        String language = context.getAttribute(LANGUAGE) != null ? context.getAttribute(LANGUAGE).toString() : config.get(DEFAULT_LANGUAGE);

        // Unique key for the instance based on context, language, and name
        String key = context.getId() + language + File.separatorChar + this.getName();

        if (!CONTAINER.containsKey(key)) {
            try {
                // Clone the current instance and set the locale
                Application clone = (Application) this.clone();
                // Initialize the context
                clone.setContext(context).setLocale(language);
                CONTAINER.put(key, clone);
            } catch (CloneNotSupportedException e) {
                // Handle clone not supported exception
                throw new ApplicationRuntimeException(e.toString(), e.getCause());
            }
        } else {
            // If the instance already exists in the container, update its locale
            CONTAINER.get(key).setContext(context).setLocale(language);
        }
    }

    /**
     * Creates and returns an instance of the application based on the context ID.
     *
     * @param context The context for which to retrieve the application instance.
     * @return The application instance.
     */
    @Override
    public Application getInstance(Context context) {
        String language = context.getAttribute(LANGUAGE) != null ? context.getAttribute(LANGUAGE).toString() : config.get(DEFAULT_LANGUAGE);

        // Retrieve the instance from the container based on context, language, and name
        return CONTAINER.get(context.getId() + language + File.separatorChar + this.getName());
    }

    /**
     * Sets the action for the given path and associates it with the provided action object.
     *
     * @param path   The path for which to set the action.
     * @param action The action object to be associated with the path.
     * @deprecated Use the {@link org.tinystruct.system.annotation.Action} annotation instead.
     */
    @Deprecated
    public void setAction(String path, Action action) {
        // Set the action in the action registry
        this.actionRegistry.set(action);
    }

    /**
     * Sets the action for the given path and associates it with the provided action object.
     *
     * @param path     The path for which to set the action.
     * @param function The action object to be associated with the path.
     * @deprecated Use the {@link org.tinystruct.system.annotation.Action} annotation instead.
     */
    @Deprecated
    public void setAction(String path, String function) {
        // Set the action in the action registry
        this.actionRegistry.set(this, path, function);
    }

    /**
     * Sets the template for the class and returns the parsed result.
     *
     * @param template The template to be set.
     * @return The parsed result of the template.
     * @throws ApplicationException If there is an issue parsing the template.
     */
    @Override
    public String setTemplate(Template template) throws ApplicationException {
        // When the template has not been disabled or the locale does not match.
        return template.parse();
    }

    /**
     * Gets the configuration for the class.
     *
     * @return The configuration for the class.
     */
    @Override
    public Configuration<String> getConfiguration() {
        return this.config;
    }

    /**
     * Sets the configuration for the class.
     *
     * @param config The configuration to be set.
     */
    @Override
    public void setConfiguration(Configuration<String> config) {
        // Set specific configuration values
        config.set(CLSID, this.name);
        config.set(DEFAULT_LANGUAGE, "zh_CN");
        config.set(LANGUAGE, config.get(DEFAULT_LANGUAGE));
        config.set(CHARSET, "utf-8");
        config.set(DEFAULT_BASE_URL, "/?q=");

        // Set locale and assign the configuration
        this.setLocale(config.get(DEFAULT_LANGUAGE));
        this.config = config;

        // Initialize only once
        this.init();

        AnnotationProcessor annotationProcessor = new AnnotationProcessor(this);
        annotationProcessor.processActionAnnotations();
    }

    /**
     * Gets the name of the class.
     *
     * @return The name of the class.
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Invokes an action with the given path.
     *
     * @param path The path of the action to invoke.
     * @return The result of invoking the action.
     * @throws ApplicationException If there is an issue invoking the action.
     */
    @Override
    public Object invoke(String path) throws ApplicationException {
        return this.invoke(path, null);
    }

    /**
     * Invokes an action with the given path and parameters.
     *
     * @param path       The path of the action to invoke.
     * @param parameters The parameters to be passed to the action.
     * @return The result of invoking the action.
     * @throws ApplicationException If there is an issue invoking the action.
     */
    @Override
    public Object invoke(String path, Object[] parameters) throws ApplicationException {
        String method = null;
        Context context = threadLocalContext.get();

        if (context != null && context.getAttribute(METHOD) != null) {
            method = context.getAttribute(METHOD).toString();
        }

        // Get the action from the registry based on path and method
        Action action = this.actionRegistry.getAction(path, method);
        if (action == null) throw new ApplicationException("Action " + path + " path does not registered.");

        // Execute the action with or without parameters
        if (parameters == null) {
            return action.execute();
        }

        return action.execute(parameters);
    }

    /**
     * Retrieves the context of the request.
     *
     * @return the context of the request
     */
    @Override
    public Context getContext() {
        return this.threadLocalContext.get();
    }

    /**
     * Sets a variable with a given name and value.
     *
     * @param name  the name of the variable
     * @param value the value of the variable
     */
    public void setVariable(String name, String value) {
        this.setVariable(name, value, true);
    }

    /**
     * Sets a variable with a given name and value, optionally overwriting an existing variable.
     *
     * @param name  the name of the variable
     * @param value the value of the variable
     * @param force whether to overwrite an existing variable
     */
    public void setVariable(String name, String value, boolean force) {
        if (value == null) value = "";
        StringVariable variable = new StringVariable(name, value);
        this.setVariable(variable, force);
    }

    /**
     * Sets a variable with a given name and value, optionally overwriting an existing variable.
     *
     * @param variable the variable to set
     * @param force    whether to overwrite an existing variable
     */
    public void setVariable(Variable<?> variable, boolean force) {
        Variables.getInstance(getLocale().toString()).setVariable(variable, force);
    }

    /**
     * Retrieves a variable with a given name.
     *
     * @param variable the name of the variable to retrieve
     * @return the variable with the given name
     */
    public Variable<?> getVariable(String variable) {
        return Variables.getInstance(getLocale().toString()).getVariable(variable);
    }

    /**
     * Sets a shared variable with a given name and value.
     *
     * @param name  the name of the shared variable
     * @param value the value of the shared variable
     */
    public void setSharedVariable(String name, String value) {
        this.setSharedVariable(name, value, getLocale().toString());
    }

    /**
     * Sets a shared variable with a given name, value and locale.
     *
     * @param name   the name of the shared variable
     * @param value  the value of the shared variable
     * @param locale the value of the locale
     */
    public void setSharedVariable(String name, String value, String locale) {
        SharedVariables.getInstance(locale).setVariable(new StringVariable(name, value), true);
    }

    /**
     * Sets the text of a field with a given name and locale.
     *
     * @param fieldName the name of the field
     * @param locale    the locale for which to retrieve the text
     * @return the text of the field for the given locale
     */
    public String setText(String fieldName, Locale locale) {
        String text = this.getProperty(fieldName, locale);
        Variables.getInstance(locale.toString()).setVariable(new StringVariable(fieldName, text), false);
        return text;
    }

    /**
     * Sets the text of a field with a given name.
     *
     * @param fieldName the name of the field
     * @return the text of the field for the default locale
     */
    public String setText(String fieldName) {
        String text = this.getProperty(fieldName);
        Variables.getInstance(locale.toString()).setVariable(new StringVariable(fieldName, text), false);
        return text;
    }

    /**
     * Sets the text of a field with a given name and arguments.
     *
     * @param fieldName the name of the field
     * @param args      the arguments to use for formatting the text
     * @return the text of the field for the default locale
     */
    public String setText(String fieldName, Object... args) {
        String text = String.format(this.getProperty(fieldName), args);
        Variables.getInstance(locale.toString()).setVariable(new StringVariable(fieldName, text), true);
        return text;
    }

    /**
     * Get a link.
     *
     * @param path path
     * @return link string
     */
    public String getLink(String path) {
        return this.getLink(path, getLocale());
    }

    /**
     * Get a link with language.
     *
     * @param path   path
     * @param locale locale
     * @return link string
     */
    public String getLink(String path, Locale locale) {
        String baseUrl;
        if (this.getContext() != null && this.getContext().getAttribute(HTTP_HOST) != null) {
            baseUrl = this.getContext().getAttribute(HTTP_HOST).toString();
        } else {
            baseUrl = this.config.get(DEFAULT_BASE_URL);
        }

        if (actionRegistry.paths().contains(path)) {
            if (locale != null) {
                return baseUrl + path + "&lang=" + locale.toLanguageTag();
            } else {
                return baseUrl + path;
            }
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

    /**
     * Retrieves the value of a property from a resource bundle file.
     *
     * @param propertyName the name of the property to retrieve
     * @return the value of the property
     */
    public String getProperty(String propertyName) {
        // Get the resource bundle for the default locale
        Resource resource = Resource.getInstance(getLocale());
        // Retrieve the value of the property from the resource bundle
        return resource.getLocaleString(propertyName);
    }

    /**
     * Retrieves the value of a property from a resource bundle file for a specified locale.
     *
     * @param propertyName the name of the property to retrieve
     * @param locale       the locale for which to retrieve the property
     * @return the value of the property for the specified locale
     */
    public String getProperty(String propertyName, Locale locale) {
        // Get the resource bundle for the specified locale
        Resource resource = Resource.getInstance(locale);
        // Retrieve the value of the property from the resource bundle
        return resource.getLocaleString(propertyName);
    }

    /**
     * Get current locale setting.
     *
     * @return locale
     */
    @Override
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Sets the locale of the context.
     *
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        String[] local = locale.split("_");
        Locale _locale = new Locale(local[0], local[1]);
        this.setLocale(_locale);
    }

    /**
     * Sets the locale of the context.
     *
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;

        this.setVariable(LANGUAGE_CODE, locale.getLanguage());
        this.setVariable(LANGUAGE, locale.toString());
        this.setVariable(LANGUAGE_TAG, locale.toLanguageTag());
        this.setVariable(new ObjectVariable("locale", locale), true);
    }

    /**
     * Returns a string representation of the context.
     *
     * @return a string representation of the context
     */
    @Override
    public String toString() {
        if (!this.templateRequired) return this.name + "@" + Integer.toHexString(hashCode());

        InputStream in = null;
        String simpleName = this.getName().substring(this.getName().lastIndexOf('.') + 1);
        String templatePath = "UNKNOWN";
        Locale locale = this.getLocale();
        if (locale != null) {
            if (locale != Locale.CHINA) {
                templatePath = "themes" + File.separatorChar + simpleName + "_" + locale + ".view";
                in = AbstractApplication.class.getClassLoader().getResourceAsStream(templatePath);
            }
        }

        if (null == in) {
            templatePath = "themes" + File.separatorChar + simpleName + ".view";
            in = AbstractApplication.class.getClassLoader().getResourceAsStream(templatePath);
        }

        if (null != in) {
            try {
                if (locale != null) {
                    return this.setTemplate(new DefaultTemplate(this, in, Variables.getInstance(locale.toString()).getVariables()));
                } else {
                    return this.setTemplate(new DefaultTemplate(this, in));
                }
            } catch (ApplicationException e) {
                throw new ApplicationRuntimeException(e.getMessage(), e);
            }
        } else {
            if (this.output != null && !this.output.trim().isEmpty()) {
                try {
                    String output;
                    if (locale != null) {
                        output = this.setTemplate(new PlainText(this, this.output, Variables.getInstance(locale.toString()).getVariables()));
                    } else {
                        output = this.setTemplate(new PlainText(this, this.output));
                    }

                    return output.replace("[%", "").replace("%]", "");
                } catch (ApplicationException e) {
                    throw new ApplicationRuntimeException(e.getMessage(), e);
                }
            }
        }

        throw new ApplicationRuntimeException("The template " + templatePath + " could not be found and the output has not been set. Reminder: If you don't need a template for this application, please setTemplateRequired(false) in init();");
    }

    /**
     * Implements runnable interface.
     */
    public void run() {
    }

    /**
     * Generates a help message for the command dispatcher.
     *
     * @return a String containing the help message
     */
    @org.tinystruct.system.annotation.Action(value = "--help", description = "Print help information")
    @Override
    public String help() {
        // Create a StringBuilder to hold the help message
        StringBuilder builder = new StringBuilder("Usage: bin" + File.separator + "dispatcher COMMAND [OPTIONS]\n");

        // Create two StringBuilder objects to hold the commands and options
        StringBuilder commands = new StringBuilder("Commands: \n");
        StringBuilder options = new StringBuilder("Options: \n");
        StringBuilder examples = new StringBuilder("Example(s): \n");
        int length = examples.length();
        int optionsLength = options.length();

        // Find the maximum length of the command names
        OptionalInt longSizeCommand = this.commandLines.keySet().stream().mapToInt(String::length).max();
        int max = longSizeCommand.orElse(0);

        // Iterate over the command lines and add them to the appropriate StringBuilder
        this.commandLines.forEach((s, commandLine) -> {
            String command = commandLine.getCommand();
            String description = commandLine.getDescription();
            String example = commandLine.getExample();

            if (command.startsWith("--")) {
                options.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            } else if (command.isEmpty()) {
                builder.append(description).append("\n");
            } else {
                commands.append("\t").append(StringUtilities.rightPadding(command, max, ' ')).append("\t").append(description).append("\n");
            }

            if (example != null && !example.isEmpty()) {
                examples.append(example).append("\n");
            }
        });

        // Add the commands and options to the StringBuilder
        builder.append(commands).append("\n");
        if (optionsLength < options.length()) builder.append(options);

        if (length < examples.length()) builder.append(examples);

        // Return the help message as a String
        return builder.toString();
    }

    /**
     * Set context for the application.
     *
     * @param context A context to be set
     */
    @Override
    public Application setContext(Context context) {
        threadLocalContext.set(context);
        return this;
    }

    /**
     * Retrieves the command line arguments and their corresponding
     * CommandLine objects stored in a Map object.
     *
     * @return a Map object containing the command line arguments and their
     * corresponding CommandLine objects
     */
    @Override
    public Map<String, CommandLine> getCommandLines() {
        return this.commandLines;
    }

    @Override
    public void destroy() {
        threadLocalContext.remove();
    }
}