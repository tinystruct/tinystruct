package org.tinystruct.transfer.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public abstract class AbstractReadableByteChannelWrapper implements ReadableByteChannel {
    protected ReadableByteChannel rbc;
    private int read;
    private int expected;

    public AbstractReadableByteChannelWrapper(ReadableByteChannel newChannel) {
        this.rbc = newChannel;
    }

    @Override
    public boolean isOpen() {
        return rbc.isOpen();
    }

    @Override
    public void close() throws IOException {
        rbc.close();
    }

    public int getReceived() {
        return read;
    }

    public void setExpected(int expected) {
        this.expected = expected;
    }

    @Override
    public int read(ByteBuffer bb) throws IOException {
        int size;
        double progress;

        if ((size = rbc.read(bb)) > 0) {
            read += size;
            progress = expected > 0 ? (double) read / (double) expected * 100.0 : -1.0;
            this.progress(progress);
        }

        return size;
    }

    public abstract void progress(double progress);
}