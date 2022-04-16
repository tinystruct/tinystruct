package org.tinystruct.system.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandLine {
    private final String command;
    private List<CommandOption> options = new ArrayList<>();
    private Set<CommandArgument<String, Object>> arguments = new HashSet<>();
    private String example;

    public CommandLine(String command) {
        this.command = command;
    }

    public CommandLine(String command, Set<CommandArgument<String, Object>> arguments, List<CommandOption> options) {
        this.command = command;
        this.arguments = arguments;
        this.options = options;
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

    public void setOptions(List<CommandOption> options) {
        this.options = options;
    }

    public Set<CommandArgument<String, Object>> getArguments() {
        return arguments;
    }

    public void addArgument(CommandArgument<String, Object> argument) {
        this.arguments.add(argument);
    }

    public void setArguments(Set<CommandArgument<String, Object>> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        String placeholders = "";
        for (CommandArgument<String, Object> argument : arguments) {
            placeholders += "/{" + argument.getKey() + "}";
        }

        String help = "Usage: bin/dispatcher " + command + placeholders + " [Options]\n";
        if (this.example != null) {
            help += "Example: " + this.example+"\n";
        }

        if (arguments.size() > 0) {
            help += "Arguments: \n";
            for (CommandArgument<String, Object> argument : arguments) {
                help += "\t  " + argument.getKey() + ":\t" + (argument.getDescription() == "" ? "Not specified" : argument.getDescription()) + "\n";
            }
        }

        if (options.size() > 0) {
            help += "Options: \n";
            for (CommandArgument<String, Object> argument : options) {
                help += "\t" + argument.getKey() + "(optional):\t" + argument.getDescription() + "\n";
            }
        }

        return help+"\n";
    }

    public String getCommand() {
        return this.command;
    }
}