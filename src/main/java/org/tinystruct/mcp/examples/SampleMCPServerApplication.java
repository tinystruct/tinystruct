package org.tinystruct.mcp.examples;

import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPPrompt;
import org.tinystruct.mcp.MCPServer;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

/**
 * Sample MCP Server Application that demonstrates how to use MCPServerApplication
 * with sample tools and prompts using the new tool method registration system.
 */
public class SampleMCPServerApplication extends MCPServer {

    @Override
    public void init() {
        super.init();
        
        // Register a plain Java object (POJO) as a tool
        // This is the recommended approach for exposing existing class methods
        this.registerTool(new StringHelper());

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

    /**
     * A simple POJO to demonstrate tool registration without extending MCPTool.
     * The @Action annotations are sufficient to expose these methods as MCP tools.
     */
    public static class StringHelper {
        @Action(
                value = "string/uppercase",
                description = "Convert a string to uppercase",
                arguments = {
                        @Argument(key = "text", description = "The text to convert", type = "string")
                }
        )
        public String toUpperCase(String text) {
            return text == null ? null : text.toUpperCase();
        }

        @Action(
                value = "string/length",
                description = "Get the length of a string",
                arguments = {
                        @org.tinystruct.system.annotation.Argument(key = "text", description = "The text to measure", type = "string")
                }
        )
        public int length(String text) {
            return text == null ? 0 : text.length();
        }
    }
} 