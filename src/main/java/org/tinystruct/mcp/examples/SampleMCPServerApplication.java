package org.tinystruct.mcp.examples;

import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPPrompt;
import org.tinystruct.mcp.MCPServerApplication;
import org.tinystruct.mcp.tools.CalculatorTool;

/**
 * Sample MCP Server Application that demonstrates how to use MCPServerApplication
 * with sample tools and prompts using the new tool method registration system.
 */
public class SampleMCPServerApplication extends MCPServerApplication {

    @Override
    public void init() {
        super.init();
        
        // Register calculator tool methods as individual tools
        CalculatorTool calculator = new CalculatorTool();
        this.registerToolMethods(calculator);

        // Register a sample prompt
        Builder promptSchema = new Builder();
        Builder properties = new Builder();

        Builder nameParam = new Builder();
        nameParam.put("type", "string");
        nameParam.put("description", "The name to greet");

        properties.put("name", nameParam);
        promptSchema.put("type", "object");
        promptSchema.put("properties", properties);
        promptSchema.put("required", new String[]{"name"});

        MCPPrompt greetingPrompt = new MCPPrompt(
            "greeting",
            "A simple greeting prompt",
            "Hello, {{name}}! Welcome to the MCP server.",
            promptSchema,
            null
        ) {
            @Override
            protected boolean supportsLocalExecution() {
                return true;
            }
        };

        this.registerPrompt(greetingPrompt);
    }
} 