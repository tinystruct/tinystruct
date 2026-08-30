package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;

/**
 * Tracks nesting depth for a single top-level {@code parse()} invocation and
 * enforces a maximum, guarding against a {@link StackOverflowError} from
 * maliciously deep or accidentally malformed JSON input.
 *
 * <p>{@link Builder} and {@link Builders} are mutually recursive while
 * parsing nested objects and arrays. Rather than each class tracking depth
 * itself (which either duplicates the bookkeeping or forces one class to
 * reach into the other's internals), every recursive call shares a single
 * {@code ParseContext}, passed down as a plain parameter. This keeps the
 * depth policy in one place, needs no static or thread-local state, and is
 * therefore trivially thread-safe and reentrant.</p>
 */
final class ParseContext {

    /** Default nesting limit used by the public, single-argument {@code parse(String)} entry points. */
    static final int DEFAULT_MAX_DEPTH = 1000;

    private final int maxDepth;
    private int depth;

    ParseContext(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Enters one more level of nesting. Intended to be used in a
     * try-with-resources statement; the returned {@link Scope} exits the
     * level automatically, so callers cannot forget to release it even if
     * an exception is thrown while parsing the nested content.
     *
     * @throws ApplicationException if entering would exceed the configured maximum depth
     */
    Scope enter() throws ApplicationException {
        if (depth >= maxDepth) {
            throw new ApplicationException("Maximum JSON nesting depth of " + maxDepth + " exceeded");
        }
        depth++;
        return this::exit;
    }

    private void exit() {
        depth--;
    }

    /** A single entered nesting level; closing it exits that level. */
    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
