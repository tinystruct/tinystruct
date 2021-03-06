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
package org.tinystruct.system.template;

import org.tinystruct.Application;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.application.Template;
import org.tinystruct.application.Variables;
import org.tinystruct.system.template.variable.DataType;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.util.TextFileLoader;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PlainText implements Template {

    private ConcurrentHashMap<String, Variable<?>> variables = Variables.getInstance();
    private InputStream in;
    private Application app;
    private String text;

    public PlainText(Application app, InputStream in) {
        this.app = app;
        this.in = in;
    }

    public PlainText(Application app, String text) {
        this.app = app;
        this.text = text;
    }

    public String getName() {
        return "Textplain";
    }

    public Variable<?> getVariable(String arg0) {
        return this.variables.get(arg0);
    }

    public Map<String, Variable<?>> getVariables() {
        return this.variables;
    }

    public String parse() throws ApplicationException {

        Configuration<String> config = app.getConfiguration();
        String value;

        if (this.text == null) {
            TextFileLoader loader = new TextFileLoader(in);
            loader.setCharset(config.get("charset"));

            try {
                this.text = loader.getContent().toString();
            } catch (ApplicationException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        Set<Entry<String, Variable<?>>> sets = variables.entrySet();
        Iterator<Entry<String, Variable<?>>> iterator = sets
                .iterator();

        List<Variable<?>> list = new ArrayList<Variable<?>>();
        Variable<?> variable;
        Context ctx = app.getContext();

        while (iterator.hasNext()) {
            Entry<String, Variable<?>> v = iterator.next();
            variable = v.getValue();

            if (variable.getType() == DataType.ARRAY) {
                list.add(variable);
            } else {
                if (v.getKey().startsWith("[%LINK:")) {
                    String base_url;

                    if (ctx != null
                            && ctx.getAttribute("HTTP_HOST") != null)
                        base_url = ctx.getAttribute("HTTP_HOST").toString();
                    else
                        base_url = config.get("default.base_url");

                    value = base_url + variable.getValue();
                } else
                    value = variable.getValue().toString();

                value = value.replaceAll("&", "&amp;");
                this.text = this.text.replace(v.getKey(), value);
            }
        }

        return this.text;
    }

    public void setVariable(Variable<?> arg0) {
        this.variables.put(arg0.getName(), arg0);
    }

}
