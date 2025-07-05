package org.tinystruct.system.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Argument annotation for specifying argument details
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Argument {
    String key(); // Argument key
    String type() default "string"; // Argument type (e.g., string, number, boolean)
    String description(); // Argument description
    boolean optional() default false; // Whether the argument is optional
}
