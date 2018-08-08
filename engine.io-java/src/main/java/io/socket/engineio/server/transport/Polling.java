package io.socket.engineio.server.transport;

import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.parser.ServerParser;
import io.socket.engineio.server.Transport;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Polling transport.
 */
public final class Polling extends Transport {

    public static final String NAME = "polling";

    private static final List<Packet> PACKET_CLOSE = Collections.unmodifiableList(new ArrayList<Packet>() {{
        add(new Packet<String>(Packet.CLOSE));
    }});
    private static final List<Packet> PACKET_NOOP = Collections.unmodifiableList(new ArrayList<Packet>() {{
        add(new Packet<String>(Packet.NOOP));
    }});

    private HttpServletRequest mRequest;
    private HttpServletResponse mResponse;
    private boolean mWritable;
    private boolean mShouldClose;

    public Polling() {
        mWritable = false;
        mShouldClose = false;
    }

    @Override
    public synchronized void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        mRequest = request;
        mResponse = response;

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

        mRequest = null;
        mResponse = null;
    }

    @Override
    public synchronized void send(List<Packet> packets) {
        mWritable = false;

        if(mShouldClose) {
            packets.add(new Packet(Packet.CLOSE));
        }

        @SuppressWarnings("unchecked")
        final boolean supportsBinary = (!((Map<String, String>) mRequest.getAttribute("query")).containsKey("b64"));

        if(packets.size() == 0) {
            throw new IllegalArgumentException("No packets to send.");
        }
        ServerParser.encodePayload(packets.toArray(new Packet[0]), supportsBinary, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                final String contentType = (data instanceof String)? "text/plain; charset=UTF-8" : "application/octet-stream";
                final int contentLength = (data instanceof String)? ((String) data).length() : ((byte[]) data).length;

                mResponse.setContentType(contentType);
                mResponse.setContentLength(contentLength);

                try (OutputStream outputStream = mResponse.getOutputStream()) {
                    // TODO: Support JSONP
                    byte[] writeBytes = (data instanceof String)? ((String) data).getBytes(StandardCharsets.UTF_8) : ((byte[]) data);
                    outputStream.write(writeBytes);
                } catch (IOException ex) {
                    onError("write failure", ex.getMessage());
                }
            }
        });

        if(mShouldClose) {
            onClose();
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
    protected synchronized void doClose() {
        if(mWritable) {
            send(new ArrayList<>(PACKET_CLOSE));
            onClose();
        } else {
            mShouldClose = true;
        }
    }

    @Override
    protected void onData(Object data) {
        ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
            @Override
            public boolean call(Packet packet, int index, int total) {
                if(packet.type.equals(Packet.CLOSE)) {
                    onClose();
                    return false;
                } else {
                    onPacket(packet);
                    return true;
                }
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

    private void onPollRequest(@SuppressWarnings("unused") HttpServletRequest request,
                               @SuppressWarnings("unused") HttpServletResponse response) {
        mWritable = true;
        emit("drain");

        if(mWritable) {
            send(new ArrayList<>(PACKET_NOOP));
        }
    }

    private void onDataRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final boolean isBinary = request.getContentType().equals("application/octet-stream");

        try(final ServletInputStream inputStream = request.getInputStream()) {
            byte[] mReadBuffer = new byte[request.getContentLength()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(mReadBuffer, 0, inputStream.available());

            onData((isBinary)? mReadBuffer : (new String(mReadBuffer, StandardCharsets.UTF_8)));
        }

        response.setContentType("text/html");
        response.getWriter().write("ok");
    }
}