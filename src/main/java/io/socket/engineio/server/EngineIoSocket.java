package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public final class EngineIoSocket extends Emitter {

    private final String mSid;
    private final EngineIoServer mServer;
    private final LinkedList<Packet> mWriteBuffer = new LinkedList<>();

    private Timer mPingTimeout = null;
    private TimerTask mPingTimeoutTask= null;

    private Runnable mCleanupFunction = null;
    private ReadyState mReadyState;
    private Transport mTransport;

    EngineIoSocket(String sid, EngineIoServer server, Transport transport, @SuppressWarnings("unused") HttpServletRequest initialRequest) {
        mSid = sid;
        mServer = server;

        mReadyState = ReadyState.OPENING;

        setTransport(transport);
        onOpen();
    }

    public void send(Packet packet) {
        sendPacket(packet);
    }

    public void close() {
        if(mReadyState == ReadyState.OPEN) {
            mReadyState = ReadyState.CLOSING;

            if(mWriteBuffer.size() > 0) {
                mTransport.on("drain", new Listener() {
                    @Override
                    public void call(Object... args) {
                        closeTransport();
                    }
                });
            } else {
                closeTransport();
            }
        }
    }

    void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        mTransport.onRequest(request, response);
    }

    private void setTransport(final Transport transport) {
        mTransport = transport;
        transport.once("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onError();
            }
        });
        transport.once("close", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String description = (args.length > 0)? ((String) args[0]) : null;
                onClose("transport close", description);
            }
        });
        transport.on("packet", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onPacket((Packet) args[0]);
            }
        });
        transport.on("drain", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                flush();
            }
        });

        mCleanupFunction = new Runnable() {
            @Override
            public void run() {
                transport.off("error");
                transport.off("close");
                transport.off("packet");
                transport.off("drain");
            }
        };
    }

    private void closeTransport() {
        mTransport.close();
    }

    private void clearTransport() {
        if(mCleanupFunction != null) {
            mCleanupFunction.run();
        }

        mTransport.close();

        mPingTimeout.cancel();
    }

    private void onOpen() {
        mReadyState = ReadyState.OPEN;

        JSONObject handshakePacket = new JSONObject();
        handshakePacket.put("sid", mSid);
        handshakePacket.put("upgrades", new JSONArray());
        handshakePacket.put("pingInterval", mServer.getPingInterval());
        handshakePacket.put("pingTimeout", mServer.getPingTimeout());

        Packet<String> openPacket = new Packet<>(Packet.OPEN);
        openPacket.data = handshakePacket.toString();

        sendPacket(openPacket);

        emit("open");
        resetPingTimeout();
    }

    private void onClose(String reason, String description) {
        if(mReadyState != ReadyState.CLOSED) {
            mReadyState = ReadyState.CLOSED;
            mPingTimeout.cancel();

            clearTransport();
            emit("close", reason, description);
        }
    }

    private void onError() {
        onClose("transport error", null);
    }

    private void onPacket(Packet packet) {
        if(mReadyState == ReadyState.OPEN) {
            emit("packet", packet);

            resetPingTimeout();

            switch (packet.type) {
                case Packet.PING:
                    sendPacket(new Packet(Packet.PONG));
                    emit("heartbeat");
                    break;
                case Packet.ERROR:
                    onClose("parse error", null);
                    break;
                case Packet.MESSAGE:
                    emit("data", packet.data);
                    emit("message", packet.data);
                    break;
            }
        }
    }

    private void sendPacket(Packet packet) {
        if((mReadyState != ReadyState.CLOSING) && (mReadyState != ReadyState.CLOSED)) {
            synchronized (mWriteBuffer) {
                mWriteBuffer.add(packet);
            }
        }
    }

    private void flush() {
        if((mReadyState != ReadyState.CLOSED) && (mTransport.isWritable()) && (mWriteBuffer.size() > 0)) {
            Object[] emitArg = mWriteBuffer.toArray();
            emit("flush", emitArg);

            synchronized (mWriteBuffer) {
                mTransport.send(mWriteBuffer);
                mWriteBuffer.clear();
            }

            emit("drain");
        }
    }

    private void resetPingTimeout() {
        if(mPingTimeout != null) {
            mPingTimeout.cancel();
        }

        mPingTimeout = new Timer();
        mPingTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                onClose("ping timeout", null);
            }
        };
        mPingTimeout.schedule(mPingTimeoutTask, mServer.getPingInterval() + mServer.getPingTimeout());
    }
}