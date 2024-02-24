package org.tinystruct.data;

import java.util.List;

public class Attachments {
    private final String parameterName;
    private final List<Attachment> list;

    public Attachments(String parameterName, List<Attachment> attachments) {
        this.parameterName = parameterName;
        this.list = attachments;
    }

    public String getParameterName() {
        return parameterName;
    }

    public List<Attachment> list() {
        return list;
    }
}
