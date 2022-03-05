package io.socket.engineio.server.transport;

import io.socket.engineio.server.parser.Packet;
import io.socket.engineio.server.parser.Parser;
import io.socket.engineio.server.Transport;
import io.socket.engineio.server.utils.ParseQS;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Polling transport.
 */
public final class Polling extends Transport implements AsyncListener {

    public static final String NAME = "polling";

    private static final List<Packet<?>> PACKET_CLOSE = Collections.unmodifiableList(new ArrayList<Packet<?>>() {{
        add(new Packet<String>(Packet.CLOSE));
    }});
    private static final List<Packet<?>> PACKET_NOOP = Collections.unmodifiableList(new ArrayList<Packet<?>>() {{
        add(new Packet<String>(Packet.NOOP));
    }});

    private final Object mLockObject;

    private HttpServletRequest mPollRequest;
    private HttpServletResponse mPollResponse;
    private boolean mWritable;
    private boolean mShouldClose;
    private Map<String, String> mQuery;
    private Map<String, List<String>> mHeaders;

    public Polling(Object lockObject, Parser parser) {
        super(parser);
        mLockObject = lockObject;

        mWritable = false;
        mShouldClose = false;
    }

    /* Transport */

    @Override
    public Map<String, String> getInitialQuery() {
        return mQuery;
    }

    @Override
    public Map<String, List<String>> getInitialHeaders() {
        return mHeaders;
    }

    @Override
    public void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        synchronized (mLockObject) {
            if (mQuery == null) {
                if (request.getQueryString() != null) {
                    mQuery = ParseQS.decode(request.getQueryString());
                } else {
                    mQuery = new HashMap<>();
                }
            }
            if (mHeaders == null) {
                mHeaders = new HashMap<>();

                final Enumeration<String> iter = request.getHeaderNames();
                if (iter != null) {
                    while (iter.hasMoreElements()) {
                        final String headerName = iter.nextElement();
                        final List<String> headerValues = new ArrayList<>();

                        final Enumeration<String> iter2 = request.getHeaders(headerName);
                        while (iter2.hasMoreElements()) {
                            headerValues.add(iter2.nextElement());
                        }

                        mHeaders.put(headerName, headerValues);
                    }
                }
            }

            switch (request.getMethod().toLowerCase()) {
                case "get":
                    onPollRequest(request, response);
                    break;
                case "post":
                    onDataRequest(request, response);
                    break;
                default:
                    response.setStatus(500);
                    response.getWriter().write("");
                    break;
            }
        }
    }

    @Override
    public void send(List<Packet<?>> packets) {
        synchronized (mLockObject) {
            mWritable = false;

            if(mShouldClose) {
                packets.add(new Packet<>(Packet.CLOSE));
            }

            @SuppressWarnings("unchecked") final Map<String, String> query = (Map<String, String>) mPollRequest.getAttribute("query");
            final boolean jsonp = query.containsKey("j");

            if(packets.size() == 0) {
                throw new IllegalArgumentException("No packets to send.");
            }
            mParser.encodePayload(packets, true, data -> {
                final String contentType;
                final byte[] contentBytes;

                if (jsonp) {
                    final String jsonpIndex = query.get("j").replaceAll("[^0-9]", "");
                    final String jsonContentString = (data instanceof String)?
                            JSONObject.quote((String)data) :
                            JSONObject.valueToString(new JSONArray(data));
                    final String jsContentString = jsonContentString
                            .replace("\u2028", "\\u2028")
                            .replace("\u2029", "\\u2029");
                    final String contentString = "___eio[" + jsonpIndex + "](" + jsContentString + ")";

                    contentType = "text/javascript; charset=UTF-8";
                    contentBytes = contentString.getBytes(StandardCharsets.UTF_8);
                } else {
                    contentType = (data instanceof String)? "text/plain; charset=UTF-8" : "application/octet-stream";
                    contentBytes = (data instanceof String)? ((String)data).getBytes(StandardCharsets.UTF_8) : ((byte[])data);
                }

                mPollResponse.setContentType(contentType);
                mPollResponse.setContentLength(contentBytes.length);

                try (OutputStream outputStream = mPollResponse.getOutputStream()) {
                    outputStream.write(contentBytes);
                } catch (IOException ex) {
                    onError("write failure", ex.getMessage());
                }

                if (mPollRequest.isAsyncStarted()) {
                    mPollRequest.getAsyncContext().complete();
                }

                mPollRequest = null;
                mPollResponse = null;
            });

            if(mShouldClose) {
                onClose();
            }
        }
    }

    @Override
    public boolean isWritable() {
        return mWritable;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void doClose() {
        synchronized (mLockObject) {
            if(mWritable) {
                send(new ArrayList<>(PACKET_CLOSE));
                onClose();
            } else {
                mShouldClose = true;
            }
        }
    }

    @Override
    protected void onData(Object data) {
        mParser.decodePayload(data, (packet, index, total) -> {
            if(packet.type.equals(Packet.CLOSE)) {
                onClose();
                return false;
            } else {
                onPacket(packet);
                return true;
            }
        });
    }

    @Override
    protected void onClose() {
        if(mWritable) {
            send(new ArrayList<>(PACKET_NOOP));
        }
        super.onClose();
    }

    /* AsyncListener */

    @Override
    public void onStartAsync(AsyncEvent asyncEvent) {
    }

    @Override
    public void onComplete(AsyncEvent asyncEvent) {
    }

    @Override
    public void onTimeout(AsyncEvent asyncEvent) {
        send(new ArrayList<>(PACKET_NOOP));
    }

    @Override
    public void onError(AsyncEvent asyncEvent) {
        onError("async failure", null);
    }

    /* Private */

    private void onPollRequest(HttpServletRequest request,
                               @SuppressWarnings("unused") HttpServletResponse response) {
        if (mPollRequest != null) {
            onError("overlap from client", "");
            mPollResponse.setStatus(500);
            try (PrintWriter writer = mPollResponse.getWriter()) {
                writer.print("error");
                writer.flush();
            } catch (IOException ignore) {
            }
            return;
        }

        mPollRequest = request;
        mPollResponse = response;

        boolean asyncEnabled = false;
        if (request.isAsyncSupported() || request.isAsyncStarted()) {
            final AsyncContext asyncContext = request.startAsync();
            asyncContext.addListener(this);
            asyncContext.setTimeout(3 * 60 * 1000);

            asyncEnabled = true;
        }

        mWritable = true;
        emit("drain");

        if (mWritable && (!asyncEnabled || mShouldClose)) {
            send(new ArrayList<>(PACKET_NOOP));
        }
    }

    private void onDataRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked") final Map<String, String> query = (Map<String, String>) request.getAttribute("query");

        final boolean jsonp = query.containsKey("j");

        try(final ServletInputStream inputStream = request.getInputStream()) {
            final byte[] mReadBuffer = new byte[request.getContentLength()];

            //noinspection ResultOfMethodCallIgnored
            inputStream.read(mReadBuffer, 0, mReadBuffer.length);

            if (jsonp) {
                final String packetPayloadRaw = ParseQS.decode(new String(mReadBuffer, StandardCharsets.UTF_8)).get("d");
                final String packetPayload = packetPayloadRaw.replace("\\n", "\n");
                onData(packetPayload);
            } else {
                onData((new String(mReadBuffer, StandardCharsets.UTF_8)));
            }
        }

        response.setContentType("text/html");
        response.getWriter().write("ok");
    }
}