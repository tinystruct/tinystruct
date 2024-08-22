package org.tinystruct.transfer.http;

import org.tinystruct.ApplicationException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * A wrapper class for ReadableByteChannel with progress tracking functionality specific to HTTP connections.
 */
public class ReadableByteChannelWrapper extends AbstractReadableByteChannelWrapper {

    /**
     * Constructs a new instance of ReadableByteChannelWrapper with the specified ReadableByteChannel and expected length.
     *
     * @param newChannel The ReadableByteChannel to wrap.
     * @param expected   The expected total number of bytes to be read.
     */
    public ReadableByteChannelWrapper(ReadableByteChannel newChannel, int expected) {
        super(newChannel);
        this.setExpected(expected);
    }

    /**
     * Constructs a new instance of ReadableByteChannelWrapper with a URL and automatically determines the expected length.
     *
     * @param uri The URL to read from.
     * @throws Exception If an error occurs while obtaining the expected length from the URL connection.
     */
    public ReadableByteChannelWrapper(URL uri) throws Exception {
        super(Channels.newChannel(uri.openStream()));

        this.setExpected(length(uri));
    }

    /**
     * Retrieves the content length of the resource at the specified URL.
     *
     * @param url The URL to retrieve the content length from.
     * @return The content length of the resource, or -1 if it cannot be determined.
     */
    private int length(URL url) throws ApplicationException {
        HttpURLConnection connection;
        int length = -1;

        try {
            HttpURLConnection.setFollowRedirects(false);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            length = connection.getContentLength();
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

        return length;
    }

    /**
     * Displays progress information when reading bytes from the channel.
     *
     * @param progress The progress value (in percentage).
     */
    @Override
    public void progress(double progress) {
        System.out.print("\r" + String.format("%d bytes received, %.02f%%", this.getReceived(), progress));
    }
}
