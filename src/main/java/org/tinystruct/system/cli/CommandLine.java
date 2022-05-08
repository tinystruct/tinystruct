package org.tinystruct.system.cli;

import org.tinystruct.Application;
import org.tinystruct.system.util.StringUtilities;

import java.util.*;

public class CommandLine implements Comparable<CommandLine>{
    private final String command;
    private String description;
    private final Application app;
    private List<CommandOption> options = new ArrayList<>();
    private Set<CommandArgument<String, Object>> arguments = new HashSet<>();
    private String example;

    public static final int ARGUMENT_MAX_WIDTH = 77;

    public CommandLine(Application app, String command, String description) {
        this.app = app;
        this.command = command;
        this.description = description;
    }

    public CommandLine(Application app, String command, String description, Set<CommandArgument<String, Object>> arguments, List<CommandOption> options) {
        this(app, command, description);

        this.arguments = arguments;
        this.options = options;
    }

    public Application getApplication() {
        return this.app;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(Object value) {
        this.example = String.format("bin/dispatcher %s/%s\n", this.command, value);
    }

    public List<CommandOption> getOptions() {
        return options;
    }

    public CommandLine setOptions(List<CommandOption> options) {
        this.options = options;

        return this;
    }

    public Set<CommandArgument<String, Object>> getArguments() {
        return arguments;
    }

    public CommandLine addArgument(CommandArgument<String, Object> argument) {
        this.arguments.add(argument);

        return this;
    }

    public CommandLine setArguments(Set<CommandArgument<String, Object>> arguments) {
        this.arguments = arguments;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder placeholders = new StringBuilder();
        for (CommandArgument<String, Object> argument : arguments) {
            placeholders.append("/{").append(argument.getKey()).append("}");
        }

        StringBuilder help = new StringBuilder("Usage: bin/dispatcher " + command + placeholders + " [OPTIONS]\n");
        help.append(this.description).append("\n");
        if (this.example != null) {
            help.append("Example: ").append(this.example).append("\n");
        }

        if (arguments.size() > 0) {
            OptionalInt longSizeCommand = this.arguments.stream().mapToInt(o->o.getKey().length()).max();
            int max = longSizeCommand.orElse(0);

            help.append("Arguments: \n");
            for (CommandArgument<String, Object> argument : arguments) {
                help.append("\t").append(StringUtilities.rightPadding(argument.getKey(), max, ' ')).append("\t").append(argument.getDescription() == "" ? "Not specified" : argument.getDescription()).append("\n");
            }
        }

        if (options.size() > 0) {
            OptionalInt longSizeCommand = this.options.stream().mapToInt(o->o.getKey().length()).max();
            int max = longSizeCommand.orElse(0);

            help.append("Options: \n");
            for (CommandArgument<String, String> argument : options) {
                help.append("\t").append(StringUtilities.rightPadding(argument.getKey(), max, ' ')).append("\t").append(argument.getDescription()).append("\n");
            }
        }

        return help + "\n";
    }

    public String getCommand() {
        return this.command;
    }

    public String help() {
        return this.toString();
    }

    @Override
    public int compareTo(CommandLine o) {
        return this.getCommand().compareTo(o.getCommand());
    }
}