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

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;
import org.tinystruct.system.annotation.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example application that demonstrates a calculator tool.
 */
public class CalculatorToolExample extends AbstractApplication {
    private static final Logger LOGGER = Logger.getLogger(CalculatorToolExample.class.getName());
    
    @Override
    public void init() {
        // Set template not required for CLI operations
        this.setTemplateRequired(false);
    }
    
    @Override
    public String version() {
        return "1.0.0";
    }
    
    /**
     * Executes a calculator operation.
     *
     * @return The result of the operation
     */
    @Action(value = "calculator/execute",
            description = "Execute a calculator operation",
            mode = org.tinystruct.application.Action.Mode.CLI)
    public String execute() {
        try {
            // Get operation parameters from context
            String operation = getContext().getAttribute("--operation").toString();
            String aStr = getContext().getAttribute("--a").toString();
            String bStr = getContext().getAttribute("--b").toString();
            
            if (operation == null || aStr == null || bStr == null) {
                return "Error: Missing parameters. Usage: calculator/execute --operation <add|subtract|multiply|divide> --a <number> --b <number>";
            }
            
            double a;
            double b;
            
            try {
                a = Double.parseDouble(aStr);
                b = Double.parseDouble(bStr);
            } catch (NumberFormatException e) {
                return "Error: Invalid number format";
            }
            
            double result;
            
            switch (operation) {
                case "add":
                    result = a + b;
                    return a + " + " + b + " = " + result;
                case "subtract":
                    result = a - b;
                    return a + " - " + b + " = " + result;
                case "multiply":
                    result = a * b;
                    return a + " * " + b + " = " + result;
                case "divide":
                    if (b == 0) {
                        return "Error: Division by zero";
                    }
                    result = a / b;
                    return a + " / " + b + " = " + result;
                default:
                    return "Error: Unknown operation: " + operation;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing calculator operation", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Main method to run the calculator example.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Initialize the application manager with default settings
            Settings settings = new Settings();
            settings.set("default.base_url", "/?q=");
            settings.set("default.language", "en_US");
            settings.set("charset", "utf-8");
            
            // Register the calculator application
            ApplicationManager.install(new CalculatorToolExample(), settings);
            
            // Test the calculator
            System.out.println("Testing calculator:");
            
            // Test addition
            Context addContext = new ApplicationContext();
            addContext.setAttribute("--operation", "add");
            addContext.setAttribute("--a", "5");
            addContext.setAttribute("--b", "3");
            String addResult = (String) ApplicationManager.call("calculator/execute", addContext);
            System.out.println(addResult);
            
            // Test subtraction
            Context subtractContext = new ApplicationContext();
            subtractContext.setAttribute("--operation", "subtract");
            subtractContext.setAttribute("--a", "10");
            subtractContext.setAttribute("--b", "4");
            String subtractResult = (String) ApplicationManager.call("calculator/execute", subtractContext);
            System.out.println(subtractResult);
            
            // Test multiplication
            Context multiplyContext = new ApplicationContext();
            multiplyContext.setAttribute("--operation", "multiply");
            multiplyContext.setAttribute("--a", "6");
            multiplyContext.setAttribute("--b", "7");
            String multiplyResult = (String) ApplicationManager.call("calculator/execute", multiplyContext);
            System.out.println(multiplyResult);
            
            // Test division
            Context divideContext = new ApplicationContext();
            divideContext.setAttribute("--operation", "divide");
            divideContext.setAttribute("--a", "20");
            divideContext.setAttribute("--b", "4");
            String divideResult = (String) ApplicationManager.call("calculator/execute", divideContext);
            System.out.println(divideResult);
            
            System.out.println("\nAll tests passed!");
            
            // Wait for user input before exiting
            System.out.println("\nPress Enter to exit...");
            new Scanner(System.in).nextLine();
            
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, "Error running calculator example", e);
        }
    }
}
