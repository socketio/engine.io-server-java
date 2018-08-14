package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.transport.WebSocket;
import io.socket.yeast.Yeast;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public final class EngineIoSocketTest {

    private static final String SUB_TRANSPORT_NAME = "stubTransport";
    private static final class StubTransport extends Transport {

        @Override
        public void onRequest(HttpServletRequest request, HttpServletResponse response) {
        }

        @Override
        public void send(List<Packet> packets) {
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

    @Test
    public void testInit() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = Mockito.spy(new EngineIoSocket(Yeast.yeast(), new EngineIoServer()));

        socket.init(transport, null);

        Assert.assertEquals(SUB_TRANSPORT_NAME, socket.getCurrentTransportName());
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("open"));
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("flush"), Mockito.any());
        Mockito.verify(socket, Mockito.times(1))
                .emit(Mockito.eq("drain"));
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.<Packet>anyList());
    }

    @Test
    public void testOnRequest() throws IOException {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        socket.onRequest(request, response);

        Mockito.verify(transport, Mockito.times(1))
                .onRequest(Mockito.eq(request), Mockito.eq(response));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testSend() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        Mockito.doAnswer(new Answer() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                List<Packet> packets = (List<Packet>) invocationOnMock.getArguments()[0];
                Assert.assertEquals(1, packets.size());
                Assert.assertEquals(packet, packets.get(0));
                return null;
            }
        }).when(transport).send(Mockito.<Packet>anyList());

        socket.send(packet);

        // +1 for init()
        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.<Packet>anyList());
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testSend_delayed() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return false;
            }
        }).when(transport).isWritable();

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        socket.send(packet);

        // +1 for init()
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.<Packet>anyList());

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return true;
            }
        }).when(transport).isWritable();
        Mockito.doAnswer(new Answer() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                List<Packet> packets = (List<Packet>) invocationOnMock.getArguments()[0];
                Assert.assertEquals(1, packets.size());
                Assert.assertEquals(packet, packets.get(0));
                return null;
            }
        }).when(transport).send(Mockito.<Packet>anyList());

        transport.emit("drain");

        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.<Packet>anyList());

    }

    @Test
    public void testClose_withoutData() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        socket.close();

        Mockito.verify(transport, Mockito.times(1))
                .close();
    }

    @Test
    public void testClose_withData() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return false;
            }
        }).when(transport).isWritable();

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "TestMessage");

        socket.send(packet);
        socket.close();

        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.<Packet>anyList());

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return true;
            }
        }).when(transport).isWritable();

        transport.emit("drain");

        // init() + send()
        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.<Packet>anyList());
        Mockito.verify(transport, Mockito.times(1))
                .close();
    }

    @Test
    public void testCanUpgrade() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoServer server = new EngineIoServer();
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), server);
        socket.init(transport, null);

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport).getName();
        Assert.assertFalse(socket.canUpgrade(Polling.NAME));
        Assert.assertTrue(socket.canUpgrade(WebSocket.NAME));
    }

    @Test
    public void testPingTimeout() throws InterruptedException {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setPingInterval(1500)
                .setPingTimeout(3000));
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), server);
        socket.init(transport, null);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        socket.send(new Packet(Packet.NOOP));

        Thread.sleep(server.getPingInterval() + server.getPingTimeout() + 500);

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("ping timeout"), Mockito.isNull());
    }

    @Test
    public void testTransportClose() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

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
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

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
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

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
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        final String packetData = "TestMessage";
        final Packet<String> packet = new Packet<>(Packet.MESSAGE, packetData);

        final Emitter.Listener messageListener = Mockito.spy(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Assert.assertEquals(1, args.length);
                Assert.assertEquals(packetData, args[0]);
            }
        });
        socket.on("message", messageListener);
        socket.on("data", messageListener);

        transport.emit("packet", packet);

        Mockito.verify(messageListener, Mockito.times(2))
                .call(Mockito.eq(packetData));
    }

    @Test
    public void testTransportPacket_ping() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        final Packet<String> packet = new Packet<>(Packet.PING);

        final Emitter.Listener heartbeatListener = Mockito.mock(Emitter.Listener.class);
        socket.on("heartbeat", heartbeatListener);

        Mockito.doAnswer(new Answer() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final Packet argPacket = ((List<Packet>) invocationOnMock.getArgument(0)).get(0);
                Assert.assertEquals(Packet.PONG, argPacket.type);
                return null;
            }
        }).when(transport).send(Mockito.<Packet>anyList());

        transport.emit("packet", packet);

        Mockito.verify(heartbeatListener, Mockito.times(1))
                .call();
        Mockito.verify(transport, Mockito.times(2))
                .send(Mockito.<Packet>anyList());
    }

    @Test
    public void testTransportPacket_error() {
        final Transport transport = Mockito.spy(new StubTransport());
        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport, null);

        final Packet<String> packet = new Packet<>(Packet.ERROR);

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        socket.on("close", closeListener);

        transport.emit("packet", packet);

        Mockito.verify(closeListener, Mockito.times(1))
                .call(Mockito.eq("parse error"), Mockito.isNull());
        Mockito.verify(transport, Mockito.times(1))
                .send(Mockito.<Packet>anyList());
    }

    @Test
    public void testUpgrade_probe() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final List<Packet> packetList = invocationOnMock.getArgument(0);

                Assert.assertEquals(1, packetList.size());

                final Packet packet = packetList.get(0);
                Assert.assertEquals(Packet.PONG, packet.type);
                Assert.assertEquals("probe", packet.data);

                return null;
            }
        }).when(transport2).send(Mockito.<Packet>anyList());

        final Emitter.Listener upgradingListener = Mockito.mock(Emitter.Listener.class);
        socket.on("upgrading", upgradingListener);

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        transport2.emit("packet", new Packet<>(Packet.PING, "probe"));
        Mockito.verify(transport2, Mockito.times(1))
                .send(Mockito.<Packet>anyList());
        Mockito.verify(upgradingListener, Mockito.times(1))
                .call(Mockito.eq(transport2));
    }

    @Test
    public void testUpgrade_upgrade() {
        final Transport transport1 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();

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
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();

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
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();

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
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();

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
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return Polling.NAME;
            }
        }).when(transport1).getName();

        final EngineIoSocket socket = new EngineIoSocket(Yeast.yeast(), new EngineIoServer());
        socket.init(transport1, null);

        final Transport transport2 = Mockito.spy(new StubTransport());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return WebSocket.NAME;
            }
        }).when(transport2).getName();

        socket.upgrade(transport2);
        Mockito.verify(transport2, Mockito.times(1))
                .on(Mockito.eq("packet"), Mockito.any(Emitter.Listener.class));

        socket.emit("close", "transport close", null);
        Mockito.verify(transport2, Mockito.times(1))
                .close();
    }
}