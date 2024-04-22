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

    org.tinystruct.application.Action.Mode mode() default org.tinystruct.application.Action.Mode.All; // Mark the functionality only available to the specified mode
}

