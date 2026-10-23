/*******************************************************************************
 * Copyright  (c) 2013, 2026 James M. ZHOU
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
package org.tinystruct.mcp;

import org.tinystruct.data.component.Builder;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class for generating and merging MCP parameter and tool schemas.
 */
final class SchemaGenerator {

    private SchemaGenerator() {
    }

    private static Builder createPropertySchema(String type, String description, String[] enumValues) {
        Builder propSchema = new Builder();
        propSchema.put("type", type);
        propSchema.put("description", description);
        if (enumValues != null && enumValues.length > 0) {
            propSchema.put("enum", enumValues);
        }
        return propSchema;
    }

    private static Builder createObjectSchema(Builder properties, Collection<String> required) {
        Builder schema = new Builder();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required.toArray(new String[0]));
        }
        return schema;
    }

    private static void addParameter(MCPTool.ToolMethod.ParameterInfo param, Builder properties, List<String> required) {
        properties.put(param.getName(), createPropertySchema(param.getType(), param.getDescription(), param.getEnumValues()));
        if (param.isRequired() && !required.contains(param.getName())) {
            required.add(param.getName());
        }
    }

    /**
     * Extracts ParameterInfo list from an @Action annotation's arguments.
     *
     * @param action The Action annotation
     * @return List of ParameterInfo objects
     */
    static List<MCPTool.ToolMethod.ParameterInfo> extractParameters(Action action) {
        List<MCPTool.ToolMethod.ParameterInfo> parameters = new ArrayList<>();
        for (Argument arg : action.arguments()) {
            parameters.add(new MCPTool.ToolMethod.ParameterInfo(
                    arg.key(),
                    arg.type(),
                    arg.description(),
                    !arg.optional(),
                    null
            ));
        }
        return parameters;
    }

    /**
     * Generates a schema Builder for a collection of ParameterInfo items.
     *
     * @param parameters The collection of ParameterInfo items
     * @return The generated schema as a Builder
     */
    static Builder generateSchema(Collection<MCPTool.ToolMethod.ParameterInfo> parameters) {
        Builder properties = new Builder();
        List<String> required = new ArrayList<>();
        for (MCPTool.ToolMethod.ParameterInfo param : parameters) {
            addParameter(param, properties, required);
        }
        return createObjectSchema(properties, required);
    }

    /**
     * Generates a schema Builder for a tool class by inspecting @Action
     * and @Argument annotations.
     *
     * @param toolClass The tool class to inspect
     * @return The generated schema as a Builder
     */
    static Builder generateSchema(Class<?> toolClass) {
        Builder properties = new Builder();
        List<String> required = new ArrayList<>();
        List<String> operations = new ArrayList<>();

        for (Method method : toolClass.getDeclaredMethods()) {
            Action action = method.getAnnotation(Action.class);
            if (action != null) {
                if (!operations.contains(method.getName())) {
                    operations.add(method.getName());
                }
                for (MCPTool.ToolMethod.ParameterInfo param : extractParameters(action)) {
                    addParameter(param, properties, required);
                }
            }
        }

        // When the class exposes multiple @Action methods, execution is dispatched
        // by an "operation" parameter naming the method to invoke (see MCPTool
        // subclasses such as CalculatorTool#executeLocally), so it must be part
        // of the schema as well.
        if (!operations.isEmpty()) {
            properties.put("operation", createPropertySchema("string", "The operation to perform", operations.toArray(new String[0])));
            if (!required.contains("operation")) {
                required.add("operation");
            }
        }

        return createObjectSchema(properties, required);
    }

    /**
     * Merges multiple tool schemas (e.g. for overloaded tool methods) into a unified schema.
     * Parameters required across all overloads remain required. When overloads declare
     * the same property differently, the last declaration wins.
     *
     * @param schemas The schemas to merge
     * @return The merged schema Builder
     */
    static Builder mergeSchemas(Collection<Builder> schemas) {
        Builder mergedProperties = new Builder();
        List<String> allRequired = null;

        for (Builder schema : schemas) {
            if (schema.containsKey("properties")) {
                Builder props = (Builder) schema.get("properties");
                for (String key : props.keySet()) {
                    mergedProperties.put(key, props.get(key));
                }
            }

            List<String> currentRequired = schema.get("required") instanceof String[]
                    ? Arrays.asList((String[]) schema.get("required"))
                    : Collections.<String>emptyList();

            if (allRequired == null) {
                allRequired = new ArrayList<>(currentRequired);
            } else {
                allRequired.retainAll(currentRequired);
            }
        }

        return createObjectSchema(mergedProperties, allRequired != null ? allRequired : Collections.<String>emptyList());
    }
}
