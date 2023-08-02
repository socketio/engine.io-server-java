package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;
import io.socket.engineio.server.parser.Parser;
import io.socket.engineio.server.parser.ParserV4;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.transport.WebSocket;
import io.socket.engineio.server.utils.ServerYeast;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class EngineIoSocketTest {

    private static final String SUB_TRANSPORT_NAME = "stubTransport";
    private static final class StubTransport extends Transport {

        public StubTransport() {
            super(Parser.PROTOCOL_V4);
        }

        @Override
        public Map<String, String> getInitialQuery() {
            return null;
        }

        @Override
        public Map<String, List<String>> getInitialHeaders() {
            return null;
        }

        @Override
        public void onRequest(HttpServletRequest request, HttpServletResponse response) {
        }

        @Override
        public void send(List<Packet<?>> packets) {
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public String getName() {
            return SUB_TRANSPORT_NAME;
        }

        @Override
        protected void doClose() {
        }
    }

    private final ScheduledExecutorService mPingTimeoutHandler = Executors.newSingleThreadScheduledExecutor();

    @Test
    public void testInit() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = Mockito.spy(new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler));

        Mockito.doAnswer(invocationOnMock -> {
            final List<Packet<?>> packetList = invocationOnMock.getArgument(0);
            for (Packet<?> packet : packetList) {
                if (packet.type.equals(Packet.OPEN)) {
                    final JSONObject jsonObject = new JSONObject((String) packet.data);
                    Assert.assertTrue(jsonObject.has("sid"));
                    Assert.assertEquals(socket.getId(), jsonObject.get("sid"));
                }
            }
            return null;
        }).when(transport).send(Mockito.anyList());

        socket.init(transport);

        Assert.assertEquals(SUB_TRANSPORT_NAME, socket.getCurrentTransportName());
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("open"));
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("flush"), Mockito.any());
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("drain"));
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.anyList());
    }

    @Test
    public void testInit_initialPacket() {
        final Packet<?> initialPacket = new Packet<>(Packet.MESSAGE, "Initial Message");
        final EngineIoServerOptions options = EngineIoServerOptions.newFromDefault()
                .setInitialPacket(initialPacket);

        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = Mockito.spy(new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(options), mPingTimeoutHandler));

        Mockito.doAnswer(invocationOnMock -> {
            final List<Packet<?>> packetList = invocationOnMock.getArgument(0);
            for (Packet<?> packet : packetList) {
                if (packet.type.equals(Packet.MESSAGE)) {
                    Assert.assertEquals(initialPacket.data, packet.data);
                }
            }
            return null;
        }).when(transport).send(Mockito.anyList());

        socket.init(transport);

        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.anyList());
    }

    @Test
    public void testOnRequest() throws IOException {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        socket.onRequest(request, response);

        Mockito.verify(transport, Mockito.times(1))
                .onRequest(Mockito.eq(request), Mockito.eq(response));
    }

    @SuppressWarnings({"Duplicates", "unchecked"})
    @Test
    public void testSend() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        Mockito.doAnswer(invocationOnMock -> {
            List<Packet<?>> packets = (List<Packet<?>>) invocationOnMock.getArguments()[0];
            Assert.assertEquals(1, packets.size());

            if (packets.get(0).type.equals(Packet.MESSAGE)) {
                Assert.assertEquals(packet.data, packets.get(0).data);
            }
            return null;
        }).when(transport).send(Mockito.anyList());

        socket.send(packet);

        // +1 for init()
        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.anyList());
    }

    @SuppressWarnings({"Duplicates", "unchecked"})
    @Test
    public void testSend_delayed() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        Mockito.doAnswer(invocationOnMock -> false).when(transport).isWritable();

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        socket.send(packet);

        // +1 for init()
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.anyList());

        Mockito.doAnswer(invocationOnMock -> true).when(transport).isWritable();
        Mockito.doAnswer(invocationOnMock -> {
            List<Packet<?>> packets = (List<Packet<?>>) invocationOnMock.getArguments()[0];
            Assert.assertEquals(1, packets.size());

            if (packets.get(0).type.equals(Packet.MESSAGE)) {
                Assert.assertEquals(packet.data, packets.get(0).data);
            }
            return null;
        }).when(transport).send(Mockito.anyList());

        transport.emit("drain");

        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.anyList());
    }

    @Test
    public void testClose_withoutData() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        socket.close();

        Assert.assertEquals(ReadyState.CLOSING, socket.getReadyState());
        Mockito.verify(transport, Mockito.times(1))
                .close();

        transport.emit("close");
        Assert.assertEquals(ReadyState.CLOSED, socket.getReadyState());
    }

    @Test
    public void testClose_withData() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        Mockito.doAnswer(invocationOnMock -> false).when(transport).isWritable();

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        socket.send(packet);
        socket.close();

        Assert.assertEquals(ReadyState.CLOSING, socket.getReadyState());
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.anyList());

        Mockito.doAnswer(invocationOnMock -> true).when(transport).isWritable();

        transport.emit("drain");

        Assert.assertEquals(ReadyState.CLOSING, socket.getReadyState());
        // init() + send()
        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.anyList());
        Mockito.verify(transport, Mockito.times(1))
                .close();

        transport.emit("close");
        Assert.assertEquals(ReadyState.CLOSED, socket.getReadyState());
    }

    @Test
    public void testCanUpgrade() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoServer server = new EngineIoServer();
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, server, mPingTimeoutHandler);
        socket.init(transport);

        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport).getName();
        Assert.assertFalse(socket.canUpgrade(Polling.NAME));
        Assert.assertTrue(socket.canUpgrade(WebSocket.NAME));
    }

    @Test
    public void testPingTimeout() throws InterruptedException {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setPingInterval(1500)
                .setPingTimeout(3000));
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, server, mPingTimeoutHandler);
        socket.init(transport);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        socket.send(new Packet<>(Packet.NOOP));

        Thread.sleep(server.getOptions().getPingInterval() + server.getOptions().getPingTimeout() + 500);

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("ping timeout"), Mockito.isNull());
    }

    @Test
    public void testTransportClose() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        transport.emit("close");

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("transport close"), Mockito.isNull());
        Mockito.verify(transport, Mockito.times(1))
                .close();
    }

    @Test
    public void testTransportError() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        transport.emit("error");

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("transport error"), Mockito.isNull());
        Mockito.verify(transport, Mockito.times(1))
                .close();
    }

    @Test
    public void testTransportPacket() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final Packet<String> packet = new Packet<>(Packet.NOOP);
        final Emitter.Listener packetListener = Mockito.mock(Emitter.Listener.class);
        socket.on("packet", packetListener);

        transport.emit("packet", packet);

        Mockito.verify(packetListener, Mockito.times(1))
                .call(Mockito.eq(packet));
    }

    @Test
    public void testTransportPacket_message() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final String packetData = "TestMessage";
        final Packet<String> packet = new Packet<>(Packet.MESSAGE, packetData);

        final Emitter.Listener messageListener = Mockito.mock(Emitter.Listener.class);
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            Assert.assertEquals(1, args.length);
            Assert.assertEquals(packetData, args[0]);
            return null;
        }).when(messageListener).call(Mockito.any());

        socket.on("message", messageListener);
        socket.on("data", messageListener);

        transport.emit("packet", packet);

        Mockito.verify(messageListener, Mockito.times(2))
                .call(Mockito.eq(packetData));
    }

    @Test
    public void testTransportPacket_ping() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoServerOptions options = EngineIoServerOptions.newFromDefault()
                .setPingInterval(1000)
                .setPingTimeout(1000);
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(options), mPingTimeoutHandler);
        socket.init(transport);

        final Emitter.Listener heartbeatListener = Mockito.mock(Emitter.Listener.class);
        socket.on("heartbeat", heartbeatListener);

        Mockito.doAnswer(invocationOnMock -> {
            final List<Packet<?>> packets = invocationOnMock.getArgument(0);
            if (packets.size() == 1 && packets.get(0).type.equals(Packet.PING)) {
                transport.emit("packet", new Packet<>(Packet.PONG));
            }
            return null;
        }).when(transport).send(Mockito.anyList());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
        }

        Mockito.verify(heartbeatListener, Mockito.atLeast(1))
                .call();
    }

    @Test
    public void testTransportPacket_error() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport);

        final Packet<String> packet = new Packet<>(Packet.ERROR);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        transport.emit("packet", packet);

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("parse error"), Mockito.isNull());
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.anyList());
    }

    @Test
    public void testUpgrade_probe() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();
        Mockito.doAnswer(invocationOnMock -> {
            final List<Packet<?>> packetList = invocationOnMock.getArgument(0);

            Assert.assertEquals(1, packetList.size());

            final Packet<?> packet = packetList.get(0);
            Assert.assertEquals(Packet.PONG, packet.type);
            Assert.assertEquals("probe", packet.data);

            return null;
        }).when(transport2).send(Mockito.anyList());

        final Emitter.Listener upgradingListener = Mockito.mock(Emitter.Listener.class);
        socket.on("upgrading", upgradingListener);

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("packet", new Packet<>(Packet.PING, "probe"));
        Mockito.verify(transport2, Mockito.times(1))
                .send(Mockito.anyList());
        Mockito.verify(upgradingListener, Mockito.times(1))
                .call(Mockito.eq(transport2));
    }

    @Test
    public void testUpgrade_upgrade() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();

        final Emitter.Listener upgradingListener = Mockito.mock(Emitter.Listener.class);
        socket.on("upgrading", upgradingListener);

        final Emitter.Listener upgradeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("upgrade", upgradeListener);

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("packet", new Packet<>(Packet.PING, "probe"));

        Mockito.verify(upgradingListener, Mockito.times(1))
                .call(Mockito.eq(transport2));

        transport2.emit("packet", new Packet<>(Packet.UPGRADE));

        Mockito.verify(transport1, Mockito.times(1))
                .close();
        Mockito.verify(upgradeListener, Mockito.times(1))
                .call(Mockito.eq(transport2));
    }

    @Test
    public void testUpgrade_invalid_handshake() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("packet", new Packet<>(Packet.MESSAGE, "foo"));

        Mockito.verify(transport2, Mockito.times(1))
                .close();
    }

    @Test
    public void testUpgrade_close() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("close", "foo");
        Mockito.verify(transport2, Mockito.times(1))
                .close();
    }

    @Test
    public void testUpgrade_error() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("error", "foo");
        Mockito.verify(transport2, Mockito.times(1))
                .close();
    }

    @Test
    public void testUpgrade_socketClose() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> Polling.NAME).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(new Object(), ServerYeast.yeast(), ParserV4.PROTOCOL, new EngineIoServer(), mPingTimeoutHandler);
        socket.init(transport1);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(invocationOnMock -> WebSocket.NAME).when(transport2).getName();

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        socket.emit("close", "transport close", null);
        Mockito.verify(transport2, Mockito.times(1))
                .close();
    }
}