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
public final class SchemaGenerator {

    private SchemaGenerator() {
    }

    /**
     * Creates a property schema Builder for an individual parameter.
     *
     * @param type        The parameter type (e.g., string, number, boolean)
     * @param description The parameter description
     * @param enumValues  Allowed enum values, or null if unconstrained
     * @return The parameter schema Builder
     */
    public static Builder createPropertySchema(String type, String description, String[] enumValues) {
        Builder propSchema = new Builder();
        propSchema.put("type", type != null ? type : "string");
        if (description != null) {
            propSchema.put("description", description);
        }
        if (enumValues != null && enumValues.length > 0) {
            propSchema.put("enum", enumValues);
        }
        return propSchema;
    }

    /**
     * Creates a root object schema Builder wrapping properties and required parameters.
     *
     * @param properties The properties Builder
     * @param required   The list of required parameter keys
     * @return The object schema Builder
     */
    public static Builder createObjectSchema(Builder properties, List<String> required) {
        Builder schema = new Builder();
        schema.put("type", "object");
        schema.put("properties", properties != null ? properties : new Builder());
        if (required != null && !required.isEmpty()) {
            schema.put("required", required.toArray(new String[0]));
        }
        return schema;
    }

    /**
     * Extracts ParameterInfo list from an @Action annotation's arguments.
     *
     * @param action The Action annotation
     * @return List of ParameterInfo objects
     */
    public static List<MCPTool.ToolMethod.ParameterInfo> extractParameters(Action action) {
        List<MCPTool.ToolMethod.ParameterInfo> parameters = new ArrayList<>();
        if (action != null && action.arguments() != null) {
            for (Argument arg : action.arguments()) {
                parameters.add(new MCPTool.ToolMethod.ParameterInfo(
                        arg.key(),
                        arg.type(),
                        arg.description(),
                        !arg.optional(),
                        null
                ));
            }
        }
        return parameters;
    }

    /**
     * Generates a schema Builder for a collection of ParameterInfo items.
     *
     * @param parameters The collection of ParameterInfo items
     * @return The generated schema as a Builder
     */
    public static Builder generateSchema(Collection<MCPTool.ToolMethod.ParameterInfo> parameters) {
        Builder properties = new Builder();
        List<String> required = new ArrayList<>();

        if (parameters != null) {
            for (MCPTool.ToolMethod.ParameterInfo param : parameters) {
                properties.put(param.getName(), createPropertySchema(param.getType(), param.getDescription(), param.getEnumValues()));
                if (param.isRequired() && !required.contains(param.getName())) {
                    required.add(param.getName());
                }
            }
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
    public static Builder generateSchema(Class<?> toolClass) {
        if (toolClass == null) {
            throw new IllegalArgumentException("toolClass cannot be null");
        }

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
                    properties.put(param.getName(), createPropertySchema(param.getType(), param.getDescription(), param.getEnumValues()));
                    if (param.isRequired() && !required.contains(param.getName())) {
                        required.add(param.getName());
                    }
                }
            }
        }

        // When the class exposes multiple @Action methods, execution is dispatched
        // by an "operation" parameter naming the method to invoke (see MCPTool
        // subclasses such as CalculatorTool#executeLocally), so it must be part
        // of the schema as well.
        if (!operations.isEmpty()) {
            Builder operationSchema = createPropertySchema("string", "The operation to perform", operations.toArray(new String[0]));
            properties.put("operation", operationSchema);
            if (!required.contains("operation")) {
                required.add("operation");
            }
        }

        return createObjectSchema(properties, required);
    }

    /**
     * Merges multiple tool schemas (e.g. For overloaded tool methods) into a unified schema.
     * Parameters required across all overloads remain required.
     *
     * @param schemas The schemas to merge
     * @return The merged schema Builder
     */
    public static Builder mergeSchemas(Collection<Builder> schemas) {
        Builder mergedProperties = new Builder();
        Set<String> allRequired = null;

        if (schemas != null) {
            for (Builder schema : schemas) {
                if (schema == null) continue;

                if (schema.containsKey("properties")) {
                    Builder props = (Builder) schema.get("properties");
                    for (String key : props.keySet()) {
                        mergedProperties.put(key, props.get(key));
                    }
                }

                Set<String> currentRequired = new HashSet<>();
                if (schema.containsKey("required")) {
                    Object req = schema.get("required");
                    if (req instanceof String[]) {
                        for (String r : (String[]) req) {
                            currentRequired.add(r);
                        }
                    }
                }

                if (allRequired == null) {
                    allRequired = currentRequired;
                } else {
                    allRequired.retainAll(currentRequired);
                }
            }
        }

        List<String> requiredList = allRequired != null ? new ArrayList<>(allRequired) : new ArrayList<>();
        return createObjectSchema(mergedProperties, requiredList);
    }
}
