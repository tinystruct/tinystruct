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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple calculator demonstration.
 */
public class CalculatorDemo {
    
    /**
     * Main method to run the calculator demonstration.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        CalculatorDemo calculator = new CalculatorDemo();
        
        System.out.println("Calculator Demonstration");
        System.out.println("----------------------");
        
        try {
            // Test addition
            Map<String, Object> addParams = new HashMap<>();
            addParams.put("operation", "add");
            addParams.put("a", 5.0);
            addParams.put("b", 3.0);
            System.out.println("5 + 3 = " + calculator.execute(addParams));
            
            // Test subtraction
            Map<String, Object> subtractParams = new HashMap<>();
            subtractParams.put("operation", "subtract");
            subtractParams.put("a", 10.0);
            subtractParams.put("b", 4.0);
            System.out.println("10 - 4 = " + calculator.execute(subtractParams));
            
            // Test multiplication
            Map<String, Object> multiplyParams = new HashMap<>();
            multiplyParams.put("operation", "multiply");
            multiplyParams.put("a", 6.0);
            multiplyParams.put("b", 7.0);
            System.out.println("6 * 7 = " + calculator.execute(multiplyParams));
            
            // Test division
            Map<String, Object> divideParams = new HashMap<>();
            divideParams.put("operation", "divide");
            divideParams.put("a", 20.0);
            divideParams.put("b", 4.0);
            System.out.println("20 / 4 = " + calculator.execute(divideParams));
            
            System.out.println("\nAll tests passed!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Executes a calculator operation.
     *
     * @param parameters The operation parameters
     * @return The result of the operation
     * @throws Exception If an error occurs
     */
    public Object execute(Map<String, Object> parameters) throws Exception {
        String operation = parameters.get("operation").toString();
        
        double a = (double) parameters.get("a");
        double b = (double) parameters.get("b");
        
        switch (operation) {
            case "add":
                return a + b;
            case "subtract":
                return a - b;
            case "multiply":
                return a * b;
            case "divide":
                if (b == 0) {
                    throw new Exception("Division by zero");
                }
                return a / b;
            default:
                throw new Exception("Unknown operation: " + operation);
        }
    }
}
