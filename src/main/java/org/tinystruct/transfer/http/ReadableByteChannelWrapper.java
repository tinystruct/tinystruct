package org.tinystruct.transfer.http;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ReadableByteChannelWrapper extends AbstractReadableByteChannelWrapper {

    public ReadableByteChannelWrapper(ReadableByteChannel newChannel, int expected) {
        super(newChannel);
        this.setExpected(expected);
    }

    public ReadableByteChannelWrapper(URL uri) throws Exception {
        super(Channels.newChannel(uri.openStream()));

        this.setExpected(length(uri));
    }

    private int length(URL url) throws Exception {
        HttpURLConnection connection;
        int length = -1;

        try {
            HttpURLConnection.setFollowRedirects(false);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            length = connection.getContentLength();
        } catch (Exception e) {
            throw e;
        }

        return length;
    }

    @Override
    public void progress(double progress) {
        System.out.print("\r" + String.format("%d bytes received, %.02f%%", this.getReceived(), progress));
    }

}
