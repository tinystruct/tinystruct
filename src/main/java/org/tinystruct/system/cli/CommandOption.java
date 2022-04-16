package org.tinystruct.system.cli;

public class CommandOption extends CommandArgument {
    public CommandOption(Object key, Object value, String description) {
        super("--" + key, value, description);
        this.optional = true;
    }
}
