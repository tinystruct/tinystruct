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
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

/**
 * A calculator tool that can perform arithmetic operations.
 * This tool demonstrates the use of individual method registration for MCP tools.
 */
public class CalculatorTool extends MCPTool {

    /**
     * Constructs a new CalculatorTool with local execution support.
     */
    public CalculatorTool() {
        // Note the true parameter at the end to enable local execution
        super("calculator", "A calculator that performs arithmetic operations");
    }

    /**
     * Constructs a new CalculatorTool with a client.
     *
     * @param client The MCP client
     */
    public CalculatorTool(MCPClient client) {
        // Note the true parameter at the end to enable local execution
        super("calculator", "A calculator that performs arithmetic operations",null, client, true);
    }

    /**
     * Adds two numbers.
     * @param a The first operand
     * @param b The second operand
     * @return The sum of a and b
     */
    @Action(value = "calculator/add", description = "Add two numbers", arguments = {
            @Argument(key = "a", description = "The first operand", type = "number"),
            @Argument(key = "b", description = "The second operand", type = "number")
    })
    public double add(double a, double b) {
        return a + b;
    }

    /**
     * Subtracts the second number from the first.
     * @param a The first operand
     * @param b The second operand
     * @return The result of a - b
     */
    @Action(value = "calculator/subtract", description = "Subtract two numbers", arguments = {
            @Argument(key = "a", description = "The first operand", type = "number"),
            @Argument(key = "b", description = "The second operand", type = "number")
    })
    public double subtract(double a, double b) {
        return a - b;
    }

    /**
     * Multiplies two numbers.
     * @param a The first operand
     * @param b The second operand
     * @return The product of a and b
     */
    @Action(value = "calculator/multiply", description = "Multiply two numbers", arguments = {
            @Argument(key = "a", description = "The first operand", type = "number"),
            @Argument(key = "b", description = "The second operand", type = "number")
    })
    public double multiply(double a, double b) {
        return a * b;
    }

    /**
     * Divides the first number by the second.
     * @param a The numerator
     * @param b The denominator
     * @return The result of a / b
     * @throws MCPException If division by zero
     */
    @Action(value = "calculator/divide", description = "Divide two numbers", arguments = {
            @Argument(key = "a", description = "The numerator", type = "number"),
            @Argument(key = "b", description = "The denominator", type = "number")
    })
    public double divide(double a, double b) throws MCPException {
        if (b == 0) {
            throw new MCPException("Division by zero");
        }
        return a / b;
    }

    /**
     * Raises the first number to the power of the second.
     * @param a The base
     * @param b The exponent
     * @return The result of a raised to the power of b
     */
    @Action(value = "calculator/power", description = "Raise a number to a power", arguments = {
            @Argument(key = "a", description = "The base", type = "number"),
            @Argument(key = "b", description = "The exponent", type = "number")
    })
    public double power(double a, double b) {
        return Math.pow(a, b);
    }

    /**
     * Computes the remainder of dividing the first number by the second.
     * @param a The dividend
     * @param b The divisor
     * @return The remainder of a divided by b
     */
    @Action(value = "calculator/modulo", description = "Modulo operation", arguments = {
            @Argument(key = "a", description = "The dividend", type = "number"),
            @Argument(key = "b", description = "The divisor", type = "number")
    })
    public double modulo(double a, double b) {
        return a % b;
    }

    /**
     * Executes the tool locally.
     * This method is called when the tool is executed with a null client.
     * This is kept for backward compatibility with the old system.
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
        try {
            java.lang.reflect.Method method = this.getClass().getMethod(operation, double.class, double.class);
            return method.invoke(this, a, b);
        } catch (NoSuchMethodException e) {
            throw new MCPException("Unknown operation: " + operation);
        } catch (Exception e) {
            if (e.getCause() instanceof MCPException) {
                throw (MCPException) e.getCause();
            }
            throw new MCPException("Error executing operation: " + e.getMessage());
        }
    }
}
