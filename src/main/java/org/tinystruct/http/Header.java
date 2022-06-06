package org.tinystruct.http;

import java.util.Locale;

/**
 * Standard Header Names.
 */
public enum Header {

    /**
     * {@code "Accept"}
     */
    ACCEPT("Accept", new StandardValue[]{StandardValue.APPLICATION_JSON, StandardValue.APPLICATION_X_WWW_FORM_URLENCODED}),

    /**
     * {@code "Accept-Charset"}
     */
    ACCEPT_CHARSET("Accept-Charset"),

    /**
     * {@code "Accept-Encoding"}
     */
    ACCEPT_ENCODING("Accept-Encoding"),

    /**
     * {@code "Accept-Language"}
     */
    ACCEPT_LANGUAGE("Accept-Language"),

    /**
     * {@code "Accept-Ranges"}
     */
    ACCEPT_RANGES("Accept-Ranges"),

    /**
     * {@code "Accept-Patch"}
     */
    ACCEPT_PATCH("Accept-Patch"),

    /**
     * {@code "Access-Control-Allow-Credentials"}
     */
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),

    /**
     * {@code "Access-Control-Allow-Headers"}
     */
    ACCESS_CONTROL_ALLOW_HEADERS("Access-Control-Allow-Headers"),

    /**
     * {@code "Access-Control-Allow-Methods"}
     */
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods"),

    /**
     * {@code "Access-Control-Allow-Origin"}
     */
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),

    /**
     * {@code "Access-Control-Expose-Headers"}
     */
    ACCESS_CONTROL_EXPOSE_HEADERS("Access-Control-Expose-Headers"),

    /**
     * {@code "Access-Control-Max-Age"}
     */
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age"),

    /**
     * {@code "Access-Control-Request-Headers"}
     */
    ACCESS_CONTROL_REQUEST_HEADERS("Access-Control-Request-Headers"),

    /**
     * {@code "Access-Control-Request-Method"}
     */
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method"),

    /**
     * {@code "Age"}
     */
    AGE("Age"),

    /**
     * {@code "Allow"}
     */
    ALLOW("Allow"),

    /**
     * {@code "Authorization"}
     */
    AUTHORIZATION("Authorization"),

    /**
     * {@code "Cache-Control"}
     */
    CACHE_CONTROL("Cache-Control"),

    /**
     * {@code "Connection"}
     */
    CONNECTION("Connection"),

    /**
     * {@code "Content-Base"}
     */
    CONTENT_BASE("Content-Base"),

    /**
     * {@code "Content-Encoding"}
     */
    CONTENT_ENCODING("Content-Encoding"),

    /**
     * {@code "Content-Language"}
     */
    CONTENT_LANGUAGE("Content-Language"),

    /**
     * {@code "Content-Length"}
     */
    CONTENT_LENGTH("Content-Length"),

    /**
     * {@code "Content-Location"}
     */
    CONTENT_LOCATION("Content-Location"),

    /**
     * {@code "Content-Transfer-Encoding"}
     */
    CONTENT_TRANSFER_ENCODING("Content-Transfer-Encoding"),

    /**
     * {@code "Content-MD5"}
     */
    CONTENT_MD5("Content-MD5"),

    /**
     * {@code "Content-Range"}
     */
    CONTENT_RANGE("Content-Range"),

    /**
     * {@code "Content-Type"}
     */
    CONTENT_TYPE("Content-Type"),

    /**
     * {@code "Cookie"}
     */
    COOKIE("Cookie"),

    /**
     * {@code "Date"}
     */
    DATE("Date"),

    /**
     * {@code "ETag"}
     */
    ETAG("ETag"),

    /**
     * {@code "Expect"}
     */
    EXPECT("Expect"),

    /**
     * {@code "Expires"}
     */
    EXPIRES("Expires"),

    /**
     * {@code "From"}
     */
    FROM("From"),

    /**
     * {@code "Host"}
     */
    HOST("Host"),

    /**
     * {@code "If-Match"}
     */
    IF_MATCH("If-Match"),

    /**
     * {@code "If-Modified-Since"}
     */
    IF_MODIFIED_SINCE("If-Modified-Since"),

    /**
     * {@code "If-None-Match"}
     */
    IF_NONE_MATCH("If-None-Match"),

    /**
     * {@code "If-Range"}
     */
    IF_RANGE("If-Range"),

    /**
     * {@code "If-Unmodified-Since"}
     */
    IF_UNMODIFIED_SINCE("If-Unmodified-Since"),

    /**
     * {@code "Last-Modified"}
     */
    LAST_MODIFIED("Last-Modified"),

    /**
     * {@code "Location"}
     */
    LOCATION("Location"),

    /**
     * {@code "Max-Forwards"}
     */
    MAX_FORWARDS("Max-Forwards"),

    /**
     * {@code "Origin"}
     */
    ORIGIN("Origin"),

    /**
     * {@code "Pragma"}
     */
    PRAGMA("Pragma"),

    /**
     * {@code "Proxy-Authenticate"}
     */
    PROXY_AUTHENTICATE("Proxy-Authenticate"),

    /**
     * {@code "Proxy-Authorization"}
     */
    PROXY_AUTHORIZATION("Proxy-Authorization"),

    /**
     * {@code "Range"}
     */
    RANGE("Range"),

    /**
     * {@code "Referer"}
     */
    REFERER("Referer"),

    /**
     * {@code "Retry-After"}
     */
    RETRY_AFTER("Retry-After"),

    /**
     * {@code "Sec-WebSocket-Key1"}
     */
    SEC_WEBSOCKET_KEY1("Sec-WebSocket-Key1"),

    /**
     * {@code "Sec-WebSocket-Key2"}
     */
    SEC_WEBSOCKET_KEY2("Sec-WebSocket-Key2"),

    /**
     * {@code "Sec-WebSocket-Location"}
     */
    SEC_WEBSOCKET_LOCATION("Sec-WebSocket-Location"),

    /**
     * {@code "Sec-WebSocket-Origin"}
     */
    SEC_WEBSOCKET_ORIGIN("Sec-WebSocket-Origin"),

    /**
     * {@code "Sec-WebSocket-Protocol"}
     */
    SEC_WEBSOCKET_PROTOCOL("Sec-WebSocket-Protocol"),

    /**
     * {@code "Sec-WebSocket-Version"}
     */
    SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),

    /**
     * {@code "Sec-WebSocket-Key"}
     */
    SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),

    /**
     * {@code "Sec-WebSocket-Accept"}
     */
    SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept"),

    /**
     * {@code "Server"}
     */
    SERVER("Server"),

    /**
     * {@code "Set-Cookie"}
     */
    SET_COOKIE("Set-Cookie"),

    /**
     * {@code "Set-Cookie2"}
     */
    SET_COOKIE2("Set-Cookie2"),

    /**
     * {@code "TE"}
     */
    TE("TE"),

    /**
     * {@code "Trailer"}
     */
    TRAILER("Trailer"),

    /**
     * {@code "Transfer-Encoding"}
     */
    TRANSFER_ENCODING("Transfer-Encoding"),

    /**
     * {@code "Upgrade"}
     */
    UPGRADE("Upgrade"),

    /**
     * {@code "User-Agent"}
     */
    USER_AGENT("User-Agent"),

    /**
     * {@code "Vary"}
     */
    VARY("Vary"),

    /**
     * {@code "Via"}
     */
    VIA("Via"),

    /**
     * {@code "Warning"}
     */
    WARNING("Warning"),

    /**
     * {@code "WebSocket-Location"}
     */
    WEBSOCKET_LOCATION("WebSocket-Location"),

    /**
     * {@code "WebSocket-Origin"}
     */
    WEBSOCKET_ORIGIN("WebSocket-Origin"),

    /**
     * {@code "WebSocket-Protocol"}
     */
    WEBSOCKET_PROTOCOL("WebSocket-Protocol"),

    /**
     * {@code "WWW-Authenticate"}
     */
    WWW_AUTHENTICATE("WWW-Authenticate");

    private final String name;
    private StandardValue[] options;
    private Object value;

    Header(String name) {
        this.name = name;
    }

    Header(String name, StandardValue[] options) {
        this.name = name;
        this.options = options;
    }

    public static Header value0f(String name) {
        return Header.valueOf(Header.class, name.toUpperCase(Locale.ROOT).replaceAll("-","_"));
    }

    @Override
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
        return this;
    }

    public Object value() {
        return this.value;
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