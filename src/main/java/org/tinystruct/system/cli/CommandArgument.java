package org.tinystruct.system.cli;

public class CommandArgument<K, V> {

    private K key;
    private V value;
    private String description;
    boolean optional;

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

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}