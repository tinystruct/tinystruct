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
            // set mode on commandLine and store in nested map
            commandLine.setMode(annotation.mode());
            this.app.getCommandLines().computeIfAbsent(annotation.value(), k -> new HashMap<>()).put(annotation.mode(), commandLine);

            for (Argument optionAnnotation : optionAnnotations) {
                String key = optionAnnotation.key();
                String optionDescription = optionAnnotation.description();
                CommandOption option = new CommandOption(key, null, optionDescription);
                options.add(option);
            }
            this.app.getCommandLines().get(annotation.value()).get(annotation.mode()).setOptions(options);
        }

        for (java.lang.reflect.Method method : this.app.getClass().getMethods()) {
            Action actionAnnotation = method.getAnnotation(Action.class);
            if (actionAnnotation != null) {
                String commandName = actionAnnotation.value();
                String description = actionAnnotation.description();
                String example = actionAnnotation.example();
                String path = actionAnnotation.value();
                Action.Mode mode = actionAnnotation.mode();

                CommandLine commandLine = new CommandLine(this.app, commandName, description);
                commandLine.setMode(mode);
                this.app.getCommandLines().computeIfAbsent(commandName, k -> new HashMap<>()).put(mode, commandLine);

                Set<CommandArgument<String, Object>> arguments = getCommandArguments(actionAnnotation);
                // Set arguments on the stored commandLine
                this.app.getCommandLines().get(commandName).get(mode).setArguments(arguments);

                Argument[] optionAnnotations = actionAnnotation.options();
                List<CommandOption> options = new ArrayList<>();
                for (Argument optionAnnotation : optionAnnotations) {
                    String key = optionAnnotation.key();
                    String optionDescription = optionAnnotation.description();
                    CommandOption option = new CommandOption(key, null, optionDescription);
                    options.add(option);
                }
                this.app.getCommandLines().get(commandName).get(mode).setOptions(options);

                if (!example.isEmpty())
                    this.app.getCommandLines().get(commandName).get(mode).setExample(example);

                // Register the action handler method
                // Set the action in the action registry
                if (mode != Action.Mode.DEFAULT) {
                    this.actionRegistry.set(this.app, path, method, mode);
                } else {
                    this.actionRegistry.set(this.app, path, method);
                }
            }
        }
    }

    private static Set<CommandArgument<String, Object>> getCommandArguments(Action actionAnnotation) {
        Argument[] argumentAnnotations = actionAnnotation.arguments();
        Set<CommandArgument<String, Object>> arguments = new HashSet<>();
        for (Argument argumentAnnotation : argumentAnnotations) {
            String key = argumentAnnotation.key();
            String argDescription = argumentAnnotation.description();
            CommandArgument<String, Object> argument = new CommandArgument<>(key, null, argDescription);
            arguments.add(argument);
        }
        return arguments;
    }
}
