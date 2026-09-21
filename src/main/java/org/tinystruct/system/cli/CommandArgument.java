package org.tinystruct.system.cli;

import java.lang.reflect.Type;

public class CommandArgument<K, V> {

    private final K key;
    private final V value;
    private String description;
    private boolean optional;
    private Type type;

    public CommandArgument(K key, V value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.optional = false;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * The Java type of the method parameter this argument describes, including generic
     * information (for example {@code Set<Role>}), or {@code null} if it is not known, as it
     * is for command options, which are not bound to a method parameter.
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}