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
import org.tinystruct.mcp.tools.CalculatorTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple JUnit test for CalculatorTool.
 */
public class SimpleCalculatorToolTest {

    /**
     * Test that the calculator tool can perform basic arithmetic operations.
     */
    @Test
    public void testBasicOperations() throws MCPException {
        // Create a calculator tool
        CalculatorTool calculator = new CalculatorTool();

        // Test addition
        Builder addParams = new Builder();
        addParams.put("operation", "add");
        addParams.put("a", 5);
        addParams.put("b", 3);
        Object addResult = calculator.execute(addParams);
        assertEquals(8.0, addResult, "5 + 3 should equal 8");

        // Test subtraction
        Builder subtractParams = new Builder();
        subtractParams.put("operation", "subtract");
        subtractParams.put("a", 10);
        subtractParams.put("b", 4);
        Object subtractResult = calculator.execute(subtractParams);
        assertEquals(6.0, subtractResult, "10 - 4 should equal 6");

        // Test multiplication
        Builder multiplyParams = new Builder();
        multiplyParams.put("operation", "multiply");
        multiplyParams.put("a", 6);
        multiplyParams.put("b", 7);
        Object multiplyResult = calculator.execute(multiplyParams);
        assertEquals(42.0, multiplyResult, "6 * 7 should equal 42");

        // Test division
        Builder divideParams = new Builder();
        divideParams.put("operation", "divide");
        divideParams.put("a", 20);
        divideParams.put("b", 4);
        Object divideResult = calculator.execute(divideParams);
        assertEquals(5.0, divideResult, "20 / 4 should equal 5");
    }

    /**
     * Test that the calculator tool can perform advanced operations.
     */
    @Test
    public void testAdvancedOperations() throws MCPException {
        // Create a calculator tool
        CalculatorTool calculator = new CalculatorTool();

        // Test power operation
        Builder powerParams = new Builder();
        powerParams.put("operation", "power");
        powerParams.put("a", 2);
        powerParams.put("b", 3);
        Object powerResult = calculator.execute(powerParams);
        assertEquals(8.0, powerResult, "2^3 should equal 8");

        // Test modulo operation
        Builder moduloParams = new Builder();
        moduloParams.put("operation", "modulo");
        moduloParams.put("a", 10);
        moduloParams.put("b", 3);
        Object moduloResult = calculator.execute(moduloParams);
        assertEquals(1.0, moduloResult, "10 % 3 should equal 1");
    }

    /**
     * Test that the calculator tool handles errors correctly.
     */
    @Test
    public void testErrorHandling() {
        // Create a calculator tool
        CalculatorTool calculator = new CalculatorTool();

        // Test division by zero
        Builder divideByZeroParams = new Builder();
        divideByZeroParams.put("operation", "divide");
        divideByZeroParams.put("a", 10);
        divideByZeroParams.put("b", 0);

        MCPException exception = assertThrows(MCPException.class, () -> {
            calculator.execute(divideByZeroParams);
        });

        assertTrue(exception.getMessage().contains("Division by zero"),
                   "Exception should mention division by zero");

        // Test invalid operation
        Builder invalidOpParams = new Builder();
        invalidOpParams.put("operation", "invalid");
        invalidOpParams.put("a", 5);
        invalidOpParams.put("b", 3);

        exception = assertThrows(MCPException.class, () -> {
            calculator.execute(invalidOpParams);
        });

        assertTrue(exception.getMessage().contains("Unknown operation") ||
                   exception.getMessage().contains("invalid value") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention unknown operation");

        // Test missing parameter
        Builder missingParams = new Builder();
        missingParams.put("operation", "add");
        missingParams.put("a", 5);
        // Missing "b" parameter

        exception = assertThrows(MCPException.class, () -> {
            calculator.execute(missingParams);
        });

        assertTrue(exception.getMessage().contains("Missing required parameter") ||
                   exception.getMessage().contains("Parameter validation failed"),
                   "Exception should mention missing parameter");
    }
}
