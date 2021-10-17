package org.tinystruct.system.cli;

public class CommandArgument<K, V> {

    private K key;
    private V value;

    public CommandArgument(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}