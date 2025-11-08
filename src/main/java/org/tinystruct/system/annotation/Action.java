package org.tinystruct.system.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Action annotation for CLI and web applications
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
    String value(); // The URI path or command name

    String description() default ""; // Description of the action

    Argument[] arguments() default {}; // Arguments expected by the action

    Argument[] options() default {}; // Command-line options

    String example() default ""; // Description of the action

    Mode mode() default Mode.DEFAULT; // Mark the functionality only available to the specified mode

    enum Mode {
        CLI("CLI"),
        HTTP_GET("GET"),
        HTTP_POST("POST"),
        HTTP_PUT("PUT"),
        HTTP_DELETE("DELETE"),
        HTTP_PATCH("PATCH"),
        HTTP_HEAD("HEAD"),
        HTTP_OPTIONS("OPTIONS"),
        DEFAULT("DEFAULT");

        final String name;

        Mode(String name) {
            this.name = name;
        }

        // Custom method to get enum by 'name' field with default fallback
        public static Mode fromName(String name) {
            // Handle null input by returning default
            if (name == null) {
                return DEFAULT;
            }
            // Iterate through all enum constants to find a match
            for (Mode mode : Mode.values()) {
                if (mode.name.equalsIgnoreCase(name)) { // Case-sensitive comparison
                    return mode;
                }
            }
            // Return default if no match found
            return DEFAULT;
        }
    }
}

