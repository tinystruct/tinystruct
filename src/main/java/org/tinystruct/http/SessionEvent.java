package org.tinystruct.http;

import java.util.EventObject;

public class SessionEvent extends EventObject {

    private Type type;

    /**
     * Constructs a prototypical Event.
     *
     * @param type Type of event
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public SessionEvent(Type type, Session source) {
        super(source);
        this.type = type;
    }

    public Session getSession() {
        return (Session) source;
    }

    public Type getType() {
        return this.type;
    }

    /**
     * Event type.
     */
    public enum Type {
        CREATED,
        DESTROYED
    }
    //TODO
}
