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
package org.tinystruct.mcp.examples;

import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.MCPException;
import org.tinystruct.mcp.MCPResource;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple calculator example that implements the MCPResource interface.
 */
public class SimpleCalculatorExample implements MCPResource {
    
    public static void main(String[] args) {
        try {
            // Create a calculator
            SimpleCalculatorExample calculator = new SimpleCalculatorExample();
            
            // Test the calculator
            System.out.println("Testing calculator:");
            
            // Test addition
            Builder addParams = new Builder();
            addParams.put("operation", "add");
            addParams.put("a", 5);
            addParams.put("b", 3);
            Object addResult = calculator.execute(addParams);
            System.out.println("5 + 3 = " + addResult);
            
            // Test subtraction
            Builder subtractParams = new Builder();
            subtractParams.put("operation", "subtract");
            subtractParams.put("a", 10);
            subtractParams.put("b", 4);
            Object subtractResult = calculator.execute(subtractParams);
            System.out.println("10 - 4 = " + subtractResult);
            
            // Test multiplication
            Builder multiplyParams = new Builder();
            multiplyParams.put("operation", "multiply");
            multiplyParams.put("a", 6);
            multiplyParams.put("b", 7);
            Object multiplyResult = calculator.execute(multiplyParams);
            System.out.println("6 * 7 = " + multiplyResult);
            
            // Test division
            Builder divideParams = new Builder();
            divideParams.put("operation", "divide");
            divideParams.put("a", 20);
            divideParams.put("b", 4);
            Object divideResult = calculator.execute(divideParams);
            System.out.println("20 / 4 = " + divideResult);
            
            System.out.println("\nAll tests passed!");
        } catch (MCPException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "A simple calculator that performs basic arithmetic operations";
    }
    
    @Override
    public ResourceType getType() {
        return ResourceType.TOOL;
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
