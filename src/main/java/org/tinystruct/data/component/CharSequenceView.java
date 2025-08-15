package org.tinystruct.data.component;

public final class CharSequenceView implements CharSequence {
    private final CharSequence source;
    private final int start;
    private final int end;

    public CharSequenceView(CharSequence source, int start, int end) {
        if (start < 0 || end > source.length() || start > end) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + " to " + end);
        }
        this.source = source;
        this.start = start;
        this.end = end;
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException();
        }
        return source.charAt(start + index);
    }

    @Override
    public CharSequence subSequence(int subStart, int subEnd) {
        return new CharSequenceView(source, start + subStart, start + subEnd);
    }

    @Override
    public String toString() {
        return source.subSequence(start, end).toString();
    }
}