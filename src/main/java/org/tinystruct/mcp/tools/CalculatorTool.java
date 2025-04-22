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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple calculator tool that can perform basic arithmetic operations.
 */
public class CalculatorTool extends MCPTool {
    
    /**
     * Constructs a new CalculatorTool.
     */
    public CalculatorTool() {
        super("calculator", "A simple calculator that performs basic arithmetic operations", createSchema(), null);
    }
    
    /**
     * Constructs a new CalculatorTool with a client.
     *
     * @param client The MCP client
     */
    public CalculatorTool(MCPClient client) {
        super("calculator", "A simple calculator that performs basic arithmetic operations", createSchema(), client);
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
        operation.put("description", "The operation to perform (add, subtract, multiply, divide)");
        operation.put("enum", new String[]{"add", "subtract", "multiply", "divide"});
        
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
    
    @Override
    public Object execute(Builder builder) throws MCPException {
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
            default:
                throw new MCPException("Unknown operation: " + operation);
        }
    }
}
