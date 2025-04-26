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
package org.tinystruct.mcp;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.component.Builder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MCPTool class.
 */
public class MCPToolTest {

    /**
     * Creates a schema for testing.
     *
     * @return A Builder object representing a schema
     */
    private Builder createTestSchema() {
        Builder schema = new Builder();

        Builder properties = new Builder();

        Builder operation = new Builder();
        operation.put("type", "string");
        operation.put("description", "The operation to perform");
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

    /**
     * Test that validation passes with valid parameters.
     */
    @Test
    public void testValidParameters() {
        // Create a tool with a schema
        MCPTool tool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, true) {
            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                // Mock implementation for testing
                return null;
            }
        };

        // Create valid parameters
        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", 5);
        params.put("b", 3);

        // Validation should not throw an exception
        assertDoesNotThrow(() -> {
            tool.execute(params);
        });
    }

    /**
     * Test that validation fails when a required parameter is missing.
     */
    @Test
    public void testMissingRequiredParameter() {
        // Create a tool with a schema
        MCPTool tool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, true) {
            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                // Mock implementation for testing
                return null;
            }
        };

        // Create parameters with a missing required parameter
        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", 5);
        // Missing "b" parameter

        // Validation should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            tool.execute(params);
        });

        // Check that the exception message mentions the missing parameter
        assertTrue(exception.getMessage().contains("Missing required parameter: b"));
    }

    /**
     * Test that validation fails when a parameter has an invalid type.
     */
    @Test
    public void testInvalidParameterType() {
        // Create a tool with a schema
        MCPTool tool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, true) {
            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                // Mock implementation for testing
                return null;
            }
        };

        // Create parameters with an invalid type
        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", "not a number"); // Should be a number
        params.put("b", 3);

        // Validation should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            tool.execute(params);
        });

        // Check that the exception message mentions the invalid type
        assertTrue(exception.getMessage().contains("has invalid type"));
    }

    /**
     * Test that validation fails when a parameter has an invalid enum value.
     */
    @Test
    public void testInvalidEnumValue() {
        // Create a tool with a schema
        MCPTool tool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, true) {
            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                // Mock implementation for testing
                return null;
            }
        };

        // Create parameters with an invalid enum value
        Builder params = new Builder();
        params.put("operation", "invalid"); // Not in the enum
        params.put("a", 5);
        params.put("b", 3);

        // Validation should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            tool.execute(params);
        });

        // Check that the exception message mentions the invalid enum value
        assertTrue(exception.getMessage().contains("has invalid value"));
    }

    /**
     * Test that local execution is supported when specified.
     */
    @Test
    public void testLocalExecutionSupport() {
        // Create a tool with local execution support
        MCPTool localTool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, true) {
            @Override
            protected Object executeLocally(Builder builder) throws MCPException {
                // Mock implementation for testing
                return "local execution";
            }
        };

        // Create a tool without local execution support
        MCPTool nonLocalTool = new MCPTool("calculator", "A test calculator", createTestSchema(), null, false);

        // Create valid parameters
        Builder params = new Builder();
        params.put("operation", "add");
        params.put("a", 5);
        params.put("b", 3);

        // Local tool should execute locally
        assertDoesNotThrow(() -> {
            Object result = localTool.execute(params);
            assertEquals("local execution", result);
        });

        // Non-local tool should throw an exception
        MCPException exception = assertThrows(MCPException.class, () -> {
            nonLocalTool.execute(params);
        });

        // Check that the exception message mentions local execution not implemented
        assertTrue(exception.getMessage().contains("Local execution not implemented"));
    }
}
