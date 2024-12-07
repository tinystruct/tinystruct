package org.tinystruct.transfer.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * An abstract class providing a wrapper for ReadableByteChannel with progress tracking functionality.
 */
public abstract class AbstractReadableByteChannelWrapper implements ReadableByteChannel {
    protected final ReadableByteChannel rbc; // The wrapped ReadableByteChannel
    private int read; // Number of bytes read

    public int getExpected() {
        return expected;
    }

    private int expected; // Expected total number of bytes to read

    /**
     * Constructs a new instance of AbstractReadableByteChannelWrapper with the specified ReadableByteChannel.
     *
     * @param newChannel The ReadableByteChannel to wrap.
     */
    public AbstractReadableByteChannelWrapper(ReadableByteChannel newChannel) {
        this.rbc = newChannel;
    }

    /**
     * Checks if the channel is open.
     *
     * @return true if the channel is open, false otherwise.
     */
    @Override
    public boolean isOpen() {
        return rbc.isOpen();
    }

    /**
     * Closes the channel.
     *
     * @throws IOException If an I/O error occurs while closing the channel.
     */
    @Override
    public void close() throws IOException {
        rbc.close();
    }

    /**
     * Gets the total number of bytes received.
     *
     * @return The total number of bytes received.
     */
    public int getReceived() {
        return read;
    }

    /**
     * Sets the expected total number of bytes to be read.
     *
     * @param expected The expected total number of bytes to be read.
     */
    public void setExpected(int expected) {
        this.expected = expected;
    }

    /**
     * Reads bytes from the channel into the given buffer and tracks the progress.
     *
     * @param bb The ByteBuffer to read bytes into.
     * @return The number of bytes read, or -1 if the channel has reached end-of-stream.
     * @throws IOException If an I/O error occurs while reading from the channel.
     */
    @Override
    public int read(ByteBuffer bb) throws IOException {
        int size;

        // Read bytes from the channel
        if ((size = rbc.read(bb)) > 0) {
            read += size;
            // Calculate progress if expected total bytes are available
            double progress = expected > 0 ? (double) read / (double) expected * 100.0 : -1.0;
            // Notify subclass about the progress
            this.progress(progress);
        }

        return size;
    }

    /**
     * Abstract method to be implemented by subclasses for handling progress updates.
     *
     * @param progress The progress value (in percentage).
     */
    public abstract void progress(double progress);
}
