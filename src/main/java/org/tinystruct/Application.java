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

import org.tinystruct.application.Actions;
import org.tinystruct.application.Context;
import org.tinystruct.application.Template;
import org.tinystruct.system.Configuration;

/**
 * Application interface used to indicate that an application should provide the standard methods.
 *
 * @author James Zhou
 * @since 0.1.0
 */
public interface Application {
    /**
     * Constants for Language code.
     */
    String LANGUAGE_CODE = "language_code";

    /**
     * Constant for language.
     */
    String LANGUAGE = "language";

    /**
     * Constant for default base url.
     */
    String DEFAULT_BASE_URL = "default.base_url";

    /**
     * Constant for charset.
     */
    String CHARSET = "charset";

    /**
     * Constant for default language.
     */
    String DEFAULT_LANGUAGE = "default.language";

    /**
     * Constant for class id.
     */
    String CLSID = "clsid";

    /**
     * Constant for method.
     */
    String METHOD = "METHOD";

    /**
     * Constant for request action.
     */
    String REQUEST_ACTION = "REQUEST_ACTION";

    /**
     * Initialize for an application once it's loaded.
     */
    void init();

    /**
     * Initialize for an application with {@link Context} once it's loaded.
     *
     * @param context context
     */
    void init(Context context);

    /**
     * Set a template for the current application.
     *
     * @param template {@link Template}
     * @throws ApplicationException application exception
     */
    void setTemplate(Template template) throws ApplicationException;

    /**
     * Set up an action mapping for method trigger.
     *
     * @param action action name
     * @param methodName method name
     */
    void setAction(String action, String methodName);

    /**
     * Set configuration for application.
     *
     * @param config configuration
     */
    void setConfiguration(Configuration<String> config);

    /**
     * Return the configuration was set.
     *
     * @return configuration
     */
    Configuration<String> getConfiguration();

    /**
     * Return a list of action.
     *
     * @return actions
     */
    Actions actions();

    /**
     * To invoke the action with action name specified.
     *
     * @param action action name
     * @return the result after method invoked
     * @throws ApplicationException application exception
     */
    Object invoke(String action) throws ApplicationException;

    /**
     * To invoke the action with action name specified.
     *
     * @param action action name
     * @param parameters parameters
     * @return the result after method invoked
     * @throws ApplicationException application exception
     */
    Object invoke(String action, Object[] parameters) throws ApplicationException;

    /**
     * Convert the application object to be a string for viewable.
     *
     * @return serialized for the application
     */
    String toString();

    /**
     * Return the version of the application.
     *
     * @return version
     */
    String version();

    /**
     * Return the context of the application.
     *
     * @return context
     */
    Context getContext();

    /**
     * Get the name of the application.
     *
     * @return name
     */
    String getName();
}
