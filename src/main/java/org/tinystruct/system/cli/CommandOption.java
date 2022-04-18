package org.tinystruct.system.cli;

public class CommandOption extends CommandArgument<String, String> {
    public CommandOption(String key, String value, String description) {
        super("--" + key, value, description);
        this.optional = true;
    }
}
