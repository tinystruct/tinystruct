package org.tinystruct.system;

import org.tinystruct.Application;
import org.tinystruct.application.ActionRegistry;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.cli.CommandArgument;
import org.tinystruct.system.cli.CommandLine;
import org.tinystruct.system.cli.CommandOption;

import java.util.*;

public class AnnotationProcessor {
    private final Application app;
    private final ActionRegistry actionRegistry = ActionRegistry.getInstance();

    public AnnotationProcessor(Application app) {
        this.app = app;
    }

    public void processActionAnnotations() {
        Action annotation = this.app.getClass().getAnnotation(Action.class);

        if (annotation != null) {
            Argument[] optionAnnotations = annotation.options();
            List<CommandOption> options = new ArrayList<>();
            CommandLine commandLine = new CommandLine(this.app, annotation.value(), annotation.description());
            this.app.getCommandLines().put(annotation.value(), commandLine);

            for (Argument optionAnnotation : optionAnnotations) {
                String key = optionAnnotation.key();
                String optionDescription = optionAnnotation.description();
                CommandOption option = new CommandOption(key, null, optionDescription);
                options.add(option);
            }
            this.app.getCommandLines().get(annotation.value()).setOptions(options);
        }

        for (java.lang.reflect.Method method : this.app.getClass().getDeclaredMethods()) {
            Action actionAnnotation = method.getAnnotation(Action.class);
            if (actionAnnotation != null) {
                String commandName = actionAnnotation.value();
                String description = actionAnnotation.description();
                String example = actionAnnotation.example();
                String path = actionAnnotation.value();

                CommandLine commandLine = new CommandLine(this.app, commandName, description);
                this.app.getCommandLines().put(commandName, commandLine);

                Argument[] argumentAnnotations = actionAnnotation.arguments();
                Set<CommandArgument<String, Object>> arguments = new HashSet<>();
                for (Argument argumentAnnotation : argumentAnnotations) {
                    String key = argumentAnnotation.key();
                    String argDescription = argumentAnnotation.description();
                    CommandArgument<String, Object> argument = new CommandArgument<>(key, null, argDescription);
                    arguments.add(argument);
                }
                this.app.getCommandLines().get(commandName).setArguments(arguments);

                Argument[] optionAnnotations = actionAnnotation.options();
                List<CommandOption> options = new ArrayList<>();
                for (Argument optionAnnotation : optionAnnotations) {
                    String key = optionAnnotation.key();
                    String optionDescription = optionAnnotation.description();
                    CommandOption option = new CommandOption(key, null, optionDescription);
                    options.add(option);
                }
                this.app.getCommandLines().get(commandName).setOptions(options);

                if (!example.equals(""))
                    this.app.getCommandLines().get(commandName).setExample(example);

                // Register the action handler method
                // Set the action in the action registry
                this.actionRegistry.set(this.app, path, method.getName());

                // Exclude the command starting with '-'
                if (path.indexOf("-") != 0)
                    this.app.setLink(path);
            }
        }

    }
}
