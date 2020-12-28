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

public interface Application {
    String LANGUAGE_CODE = "language_code";
    String LANGUAGE = "language";
    String DEFAULT_BASE_URL = "default.base_url";
    String CHARSET = "charset";
    String DEFAULT_LANGUAGE = "default.language";
    String CLSID = "clsid";
    String METHOD = "METHOD";
    String REQUEST_ACTION = "REQUEST_ACTION";

    void init();

    void init(Context context);

    void setTemplate(Template template) throws ApplicationException;

    void setAction(String action, String methodName);

    void setConfiguration(Configuration<String> config);

    Configuration<String> getConfiguration();

    Actions actions();

    Object invoke(String action) throws ApplicationException;

    Object invoke(String action, Object[] parameters) throws ApplicationException;

    String toString();

    String version();

    Context getContext();

    String getName();
}
