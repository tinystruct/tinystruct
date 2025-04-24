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
import java.util.Scanner;

/**
 * A simple calculator example.
 */
public class SimpleCalculator {
    
    /**
     * Main method to run the calculator example.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SimpleCalculator calculator = new SimpleCalculator();
        
        System.out.println("Simple Calculator");
        System.out.println("----------------");
        System.out.println("1. Addition");
        System.out.println("2. Subtraction");
        System.out.println("3. Multiplication");
        System.out.println("4. Division");
        System.out.println("5. Exit");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\nEnter your choice (1-5): ");
            String choice = scanner.nextLine();
            
            if ("5".equals(choice)) {
                System.out.println("Goodbye!");
                break;
            }
            
            System.out.print("Enter first number: ");
            String aStr = scanner.nextLine();
            
            System.out.print("Enter second number: ");
            String bStr = scanner.nextLine();
            
            try {
                double a = Double.parseDouble(aStr);
                double b = Double.parseDouble(bStr);
                
                Map<String, Object> params = new HashMap<>();
                
                switch (choice) {
                    case "1":
                        params.put("operation", "add");
                        params.put("a", a);
                        params.put("b", b);
                        System.out.println("Result: " + calculator.execute(params));
                        break;
                    case "2":
                        params.put("operation", "subtract");
                        params.put("a", a);
                        params.put("b", b);
                        System.out.println("Result: " + calculator.execute(params));
                        break;
                    case "3":
                        params.put("operation", "multiply");
                        params.put("a", a);
                        params.put("b", b);
                        System.out.println("Result: " + calculator.execute(params));
                        break;
                    case "4":
                        params.put("operation", "divide");
                        params.put("a", a);
                        params.put("b", b);
                        System.out.println("Result: " + calculator.execute(params));
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please try again.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
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
