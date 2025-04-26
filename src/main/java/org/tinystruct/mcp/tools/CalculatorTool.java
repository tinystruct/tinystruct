/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
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
package org.tinystruct.mcp.tools;

import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPClient;
import org.tinystruct.mcp.MCPException;
import org.tinystruct.mcp.MCPTool;

/**
 * A calculator tool that can perform arithmetic operations.
 * This tool demonstrates the use of local execution support in MCPTool.
 */
public class CalculatorTool extends MCPTool {

    /**
     * Constructs a new CalculatorTool with local execution support.
     */
    public CalculatorTool() {
        // Note the true parameter at the end to enable local execution
        super("calculator", "A calculator that performs arithmetic operations", createSchema(), null, true);
    }

    /**
     * Constructs a new CalculatorTool with a client.
     *
     * @param client The MCP client
     */
    public CalculatorTool(MCPClient client) {
        // Note the true parameter at the end to enable local execution
        super("calculator", "A calculator that performs arithmetic operations", createSchema(), client, true);
    }

    /**
     * Creates the schema for the calculator tool.
     *
     * @return The schema
     */
    private static Builder createSchema() {
        Builder schema = new Builder();

        Builder properties = new Builder();

        Builder operation = new Builder();
        operation.put("type", "string");
        operation.put("description", "The operation to perform (add, subtract, multiply, divide, power, modulo)");
        operation.put("enum", new String[]{"add", "subtract", "multiply", "divide", "power", "modulo"});

        Builder a = new Builder();
        a.put("type", "number");
        a.put("description", "The first operand");

        Builder b = new Builder();
        b.put("type", "number");
        b.put("description", "The second operand");

        properties.put("operation", operation);
        properties.put("a", a);
        properties.put("b", b);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"operation", "a", "b"});

        return schema;
    }

    /**
     * Executes the tool locally.
     * This method is called when the tool is executed with a null client.
     *
     * @param builder The parameters to use for execution
     * @return The result of the execution
     * @throws MCPException If an error occurs during execution
     */
    @Override
    protected Object executeLocally(Builder builder) throws MCPException {
        String operation = builder.get("operation").toString();

        double a;
        double b;

        try {
            a = Double.parseDouble(builder.get("a").toString());
            b = Double.parseDouble(builder.get("b").toString());
        } catch (NumberFormatException e) {
            throw new MCPException("Invalid number format: " + e.getMessage());
        }

        switch (operation) {
            case "add":
                return a + b;
            case "subtract":
                return a - b;
            case "multiply":
                return a * b;
            case "divide":
                if (b == 0) {
                    throw new MCPException("Division by zero");
                }
                return a / b;
            case "power":
                return Math.pow(a, b);
            case "modulo":
                return a % b;
            default:
                throw new MCPException("Unknown operation: " + operation);
        }
    }
}
