package org.tinystruct.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard Header Names.
 */
public class Header implements Cloneable {
    private static final Map<String, Header> map = new HashMap<>();

    /**
     * {@code "Accept"}
     */
    public static final Header ACCEPT = new Header("Accept", new StandardValue[]{StandardValue.APPLICATION_JSON, StandardValue.APPLICATION_X_WWW_FORM_URLENCODED});

    /**
     * {@code "Accept-Charset"}
     */
    public static final Header ACCEPT_CHARSET = new Header("Accept-Charset");

    /**
     * {@code "Accept-Encoding"}
     */
    public static final Header ACCEPT_ENCODING = new Header("Accept-Encoding");

    /**
     * {@code "Accept-Language"}
     */
    public static final Header ACCEPT_LANGUAGE = new Header("Accept-Language");

    /**
     * {@code "Accept-Ranges"}
     */
    public static final Header ACCEPT_RANGES = new Header("Accept-Ranges");

    /**
     * {@code "Accept-Patch"}
     */
    public static final Header ACCEPT_PATCH = new Header("Accept-Patch");

    /**
     * {@code "Access-Control-Allow-Credentials"}
     */
    public static final Header ACCESS_CONTROL_ALLOW_CREDENTIALS = new Header("Access-Control-Allow-Credentials");

    /**
     * {@code "Access-Control-Allow-Headers"}
     */
    public static final Header ACCESS_CONTROL_ALLOW_HEADERS = new Header("Access-Control-Allow-Headers");

    /**
     * {@code "Access-Control-Allow-Methods"}
     */
    public static final Header ACCESS_CONTROL_ALLOW_METHODS = new Header("Access-Control-Allow-Methods");

    /**
     * {@code "Access-Control-Allow-Origin"}
     */
    public static final Header ACCESS_CONTROL_ALLOW_ORIGIN = new Header("Access-Control-Allow-Origin");

    /**
     * {@code "Access-Control-Expose-Headers"}
     */
    public static final Header ACCESS_CONTROL_EXPOSE_HEADERS = new Header("Access-Control-Expose-Headers");

    /**
     * {@code "Access-Control-Max-Age"}
     */
    public static final Header ACCESS_CONTROL_MAX_AGE = new Header("Access-Control-Max-Age");

    /**
     * {@code "Access-Control-Request-Headers"}
     */
    public static final Header ACCESS_CONTROL_REQUEST_HEADERS = new Header("Access-Control-Request-Headers");

    /**
     * {@code "Access-Control-Request-Method"}
     */
    public static final Header ACCESS_CONTROL_REQUEST_METHOD = new Header("Access-Control-Request-Method");

    /**
     * {@code "Age"}
     */
    public static final Header AGE = new Header("Age");

    /**
     * {@code "Allow"}
     */
    public static final Header ALLOW = new Header("Allow");

    /**
     * {@code "Authorization"}
     */
    public static final Header AUTHORIZATION = new Header("Authorization");

    /**
     * {@code "Cache-Control"}
     */
    public static final Header CACHE_CONTROL = new Header("Cache-Control");

    /**
     * {@code "Connection"}
     */
    public static final Header CONNECTION = new Header("Connection");

    /**
     * {@code "Content-Base"}
     */
    public static final Header CONTENT_BASE = new Header("Content-Base");

    /**
     * {@code "Content-Encoding"}
     */
    public static final Header CONTENT_ENCODING = new Header("Content-Encoding");

    /**
     * {@code "Content-Language"}
     */
    public static final Header CONTENT_LANGUAGE = new Header("Content-Language");

    /**
     * {@code "Content-Length"}
     */
    public static final Header CONTENT_LENGTH = new Header("Content-Length");

    /**
     * {@code "Content-Disposition"}
     */
    public static final Header CONTENT_DISPOSITION = new Header("Content-Disposition");

    /**
     * {@code "Content-Location"}
     */
    public static final Header CONTENT_LOCATION = new Header("Content-Location");

    /**
     * {@code "Content-Transfer-Encoding"}
     */
    public static final Header CONTENT_TRANSFER_ENCODING = new Header("Content-Transfer-Encoding");

    /**
     * {@code "Content-MD5"}
     */
    public static final Header CONTENT_MD5 = new Header("Content-MD5");

    /**
     * {@code "Content-Range"}
     */
    public static final Header CONTENT_RANGE = new Header("Content-Range");

    /**
     * {@code "Content-Type"}
     */
    public static final Header CONTENT_TYPE = new Header("Content-Type");

    /**
     * {@code "Cookie"}
     */
    public static final Header COOKIE = new Header("Cookie");

    /**
     * {@code "Date"}
     */
    public static final Header DATE = new Header("Date");

    /**
     * {@code "ETag"}
     */
    public static final Header ETAG = new Header("ETag");

    /**
     * {@code "Expect"}
     */
    public static final Header EXPECT = new Header("Expect");

    /**
     * {@code "Expires"}
     */
    public static final Header EXPIRES = new Header("Expires");

    /**
     * {@code "From"}
     */
    public static final Header FROM = new Header("From");

    /**
     * {@code "Host"}
     */
    public static final Header HOST = new Header("Host");

    /**
     * {@code "If-Match"}
     */
    public static final Header IF_MATCH = new Header("If-Match");

    /**
     * {@code "If-Modified-Since"}
     */
    public static final Header IF_MODIFIED_SINCE = new Header("If-Modified-Since");

    /**
     * {@code "If-None-Match"}
     */
    public static final Header IF_NONE_MATCH = new Header("If-None-Match");

    /**
     * {@code "If-Range"}
     */
    public static final Header IF_RANGE = new Header("If-Range");

    /**
     * {@code "If-Unmodified-Since"}
     */
    public static final Header IF_UNMODIFIED_SINCE = new Header("If-Unmodified-Since");

    /**
     * {@code "Last-Modified"}
     */
    public static final Header LAST_MODIFIED = new Header("Last-Modified");

    /**
     * {@code "Location"}
     */
    public static final Header LOCATION = new Header("Location");

    /**
     * {@code "Max-Forwards"}
     */
    public static final Header MAX_FORWARDS = new Header("Max-Forwards");

    /**
     * {@code "Origin"}
     */
    public static final Header ORIGIN = new Header("Origin");

    /**
     * {@code "Pragma"}
     */
    public static final Header PRAGMA = new Header("Pragma");

    /**
     * {@code "Proxy-Authenticate"}
     */
    public static final Header PROXY_AUTHENTICATE = new Header("Proxy-Authenticate");

    /**
     * {@code "Proxy-Authorization"}
     */
    public static final Header PROXY_AUTHORIZATION = new Header("Proxy-Authorization");

    /**
     * {@code "Range"}
     */
    public static final Header RANGE = new Header("Range");

    /**
     * {@code "Referer"}
     */
    public static final Header REFERER = new Header("Referer");

    /**
     * {@code "Retry-After"}
     */
    public static final Header RETRY_AFTER = new Header("Retry-After");

    /**
     * {@code "Sec-WebSocket-Key1"}
     */
    public static final Header SEC_WEBSOCKET_KEY1 = new Header("Sec-WebSocket-Key1");

    /**
     * {@code "Sec-WebSocket-Key2"}
     */
    public static final Header SEC_WEBSOCKET_KEY2 = new Header("Sec-WebSocket-Key2");

    /**
     * {@code "Sec-WebSocket-Location"}
     */
    public static final Header SEC_WEBSOCKET_LOCATION = new Header("Sec-WebSocket-Location");

    /**
     * {@code "Sec-WebSocket-Origin"}
     */
    public static final Header SEC_WEBSOCKET_ORIGIN = new Header("Sec-WebSocket-Origin");

    /**
     * {@code "Sec-WebSocket-Protocol"}
     */
    public static final Header SEC_WEBSOCKET_PROTOCOL = new Header("Sec-WebSocket-Protocol");

    /**
     * {@code "Sec-WebSocket-Version"}
     */
    public static final Header SEC_WEBSOCKET_VERSION = new Header("Sec-WebSocket-Version");

    /**
     * {@code "Sec-WebSocket-Key"}
     */
    public static final Header SEC_WEBSOCKET_KEY = new Header("Sec-WebSocket-Key");

    /**
     * {@code "Sec-WebSocket-Accept"}
     */
    public static final Header SEC_WEBSOCKET_ACCEPT = new Header("Sec-WebSocket-Accept");

    /**
     * {@code "Sec-Fetch-Dest"}
     */
    public static final Header SEC_FETCH_DEST = new Header("Sec-Fetch-Dest");

    /**
     * {@code "Sec-Fetch-Mode"}
     */
    public static final Header SEC_FETCH_MODE = new Header("Sec-Fetch-Mode");

    /**
     * {@code "Sec-Fetch-Site"}
     */
    public static final Header SEC_FETCH_SITE = new Header("Sec-Fetch-Site");

    /**
     * {@code "Sec-Fetch-User"}
     */
    public static final Header SEC_FETCH_USER = new Header("Sec-Fetch-User");

    /**
     * {@code "Server"}
     */
    public static final Header SERVER = new Header("Server");

    /**
     * {@code "Set-Cookie"}
     */
    public static final Header SET_COOKIE = new Header("Set-Cookie");

    /**
     * {@code "Set-Cookie2"}
     */
    public static final Header SET_COOKIE2 = new Header("Set-Cookie2");

    /**
     * {@code "TE"}
     */
    public static final Header TE = new Header("TE");

    /**
     * {@code "Trailer"}
     */
    public static final Header TRAILER = new Header("Trailer");

    /**
     * {@code "Transfer-Encoding"}
     */
    public static final Header TRANSFER_ENCODING = new Header("Transfer-Encoding");

    /**
     * {@code "Upgrade"}
     */
    public static final Header UPGRADE = new Header("Upgrade");

    /**
     * {@code "Upgrade-Insecure-Requests"}
     */
    public static final Header UPGRADE_INSECURE_REQUESTS = new Header("Upgrade-Insecure-Requests");

    /**
     * {@code "User-Agent"}
     */
    public static final Header USER_AGENT = new Header("User-Agent");

    /**
     * {@code "Vary"}
     */
    public static final Header VARY = new Header("Vary");

    /**
     * {@code "Via"}
     */
    public static final Header VIA = new Header("Via");

    /**
     * {@code "Warning"}
     */
    public static final Header WARNING = new Header("Warning");

    /**
     * {@code "WebSocket-Location"}
     */
    public static final Header WEBSOCKET_LOCATION = new Header("WebSocket-Location");

    /**
     * {@code "WebSocket-Origin"}
     */
    public static final Header WEBSOCKET_ORIGIN = new Header("WebSocket-Origin");

    /**
     * {@code "WebSocket-Protocol"}
     */
    public static final Header WEBSOCKET_PROTOCOL = new Header("WebSocket-Protocol");

    /**
     * {@code "WWW-Authenticate"}
     */
    public static final Header WWW_AUTHENTICATE = new Header("WWW-Authenticate");

    /**
     * {@code "x-requested-with"}
     */
    public static final Header X_REQUESTED_WITH = new Header("X-Requested-With");

    /**
     * {@code "X-Request-Id"}
     */
    public static final Header X_REQUEST_ID = new Header("X-Request-Id");

    /**
     * {@code "Not-Supported"}
     */
    public static final Header NOT_SUPPORTED = new Header("Not-Supported");
    private final String name;
    private StandardValue[] options;
    private Object value;

    Header(String name) {
        this.name = name;
        map.put(this.name, this);
    }

    Header(String name, StandardValue[] options) {
        this.name = name;
        this.options = options;
    }

    public static Header value0f(String name) {
        try {
            return Header.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Header.NOT_SUPPORTED;
        }
    }

    private static Header valueOf(String name) {
        Header header;
        if ((header = map.get(name)) == null) throw new IllegalArgumentException();
        return header;
    }

    @Override
    @Deprecated
    public String toString() {
        return this.name;
    }

    public StandardValue[] getOptions() {
        return this.options;
    }

    public void setOptions(StandardValue[] options) {
        this.options = options;
    }

    public Header set(Object value) {
        this.value = value;
        try {
            return (Header) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Header setInt(int readableBytes) {
        this.value = readableBytes;
        try {
            return (Header) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Object value() {
        return this.value;
    }

    public String name() {
        return this.name;
    }

    /**
     * Standard HTTP header values.
     */
    public enum StandardValue {
        /**
         * {@code "application/json"}
         */
        APPLICATION_JSON("application/json"),

        /**
         * {@code "application/x-www-form-urlencoded"}
         */
        APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),

        /**
         * {@code "base64"}
         */
        BASE64("base64"),

        /**
         * {@code "binary"}
         */
        BINARY("binary"),

        /**
         * {@code "boundary"}
         */
        BOUNDARY("boundary"),

        /**
         * {@code "bytes"}
         */
        BYTES("bytes"),

        /**
         * {@code "charset"}
         */
        CHARSET("charset"),

        /**
         * {@code "chunked"}
         */
        CHUNKED("chunked"),

        /**
         * {@code "close"}
         */
        CLOSE("close"),

        /**
         * {@code "compress"}
         */
        COMPRESS("compress"),

        /**
         * {@code "100-continue"}
         */
        CONTINUE("100-continue"),

        /**
         * {@code "deflate"}
         */
        DEFLATE("deflate"),

        /**
         * {@code "gzip"}
         */
        GZIP("gzip"),

        /**
         * {@code "gzip,deflate"}
         */
        GZIP_DEFLATE("gzip,deflate"),

        /**
         * {@code "identity"}
         */
        IDENTITY("identity"),

        /**
         * {@code "keep-alive"}
         */
        KEEP_ALIVE("keep-alive"),

        /**
         * {@code "max-age"}
         */
        MAX_AGE("max-age"),

        /**
         * {@code "max-stale"}
         */
        MAX_STALE("max-stale"),

        /**
         * {@code "min-fresh"}
         */
        MIN_FRESH("min-fresh"),

        /**
         * {@code "multipart/form-data"}
         */
        MULTIPART_FORM_DATA("multipart/form-data"),

        /**
         * {@code "must-revalidate"}
         */
        MUST_REVALIDATE("must-revalidate"),

        /**
         * {@code "no-cache"}
         */
        NO_CACHE("no-cache"),

        /**
         * {@code "no-store"}
         */
        NO_STORE("no-store"),

        /**
         * {@code "no-transform"}
         */
        NO_TRANSFORM("no-transform"),

        /**
         * {@code "none"}
         */
        NONE("none"),

        /**
         * {@code "only-if-cached"}
         */
        ONLY_IF_CACHED("only-if-cached"),

        /**
         * {@code "private"}
         */
        PRIVATE("private"),

        /**
         * {@code "proxy-revalidate"}
         */
        PROXY_REVALIDATE("proxy-revalidate"),

        /**
         * {@code "public"}
         */
        PUBLIC("public"),

        /**
         * {@code "quoted-printable"}
         */
        QUOTED_PRINTABLE("quoted-printable"),

        /**
         * {@code "s-maxage"}
         */
        S_MAXAGE("s-maxage"),

        /**
         * {@code "trailers"}
         */
        TRAILERS("trailers"),

        /**
         * {@code "Upgrade"}
         */
        UPGRADE("Upgrade"),

        /**
         * {@code "WebSocket"}
         */
        WEBSOCKET("WebSocket");

        private final String value;

        StandardValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}