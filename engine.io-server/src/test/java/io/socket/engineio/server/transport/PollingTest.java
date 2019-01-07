package io.socket.engineio.server.transport;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.parser.ServerParser;
import io.socket.engineio.server.HttpServletResponseImpl;
import io.socket.engineio.server.ServletInputStreamWrapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class PollingTest {

    @Test
    public void testName() {
        final Polling polling = new Polling();

        assertEquals(Polling.NAME, polling.getName());
    }

    @Test
    public void testWritable_normal() {
        final Polling polling = new Polling();

        assertFalse(polling.isWritable());
    }

    @Test
    public void testOnRequest_error() throws IOException {
        final Polling polling = Mockito.spy(new Polling());

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return "DELETE";
            }
        }).when(request).getMethod();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", Polling.NAME);
                return queryMap;
            }
        }).when(request).getAttribute("query");

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        polling.onRequest(request, response);

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testOnRequest_poll() throws IOException {
        final Polling polling = Mockito.spy(new Polling());

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return "GET";
            }
        }).when(request).getMethod();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", Polling.NAME);
                return queryMap;
            }
        }).when(request).getAttribute("query");

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        polling.onRequest(request, response);

        Mockito.verify(polling, Mockito.times(1))
                .emit(Mockito.eq("drain"));

        final String responseString = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        ServerParser.decodePayload(responseString, new Parser.DecodePayloadCallback() {
            @Override
            public boolean call(Packet packet, int index, int total) {
                assertEquals(1, total);
                assertEquals(Packet.NOOP, packet.type);
                return true;
            }
        });
    }

    @Test
    public void testOnRequest_data() {
        final String messageData = "Test Data";

        final Polling polling = Mockito.spy(new Polling());
        polling.on("packet", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final Packet packet = (Packet) args[0];
                assertEquals(Packet.MESSAGE, packet.type);
                assertEquals(messageData, packet.data);
            }
        });

        final Packet<String> requestPacket = new Packet<>(Packet.MESSAGE, messageData);
        ServerParser.encodePayloadAsBinary(new Packet[]{requestPacket}, new Parser.EncodeCallback<byte[]>() {
            @Override
            public void call(final byte[] data) {
                final ByteArrayInputStream requestInputStream = new ByteArrayInputStream(data);
                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return "POST";
                    }
                }).when(request).getMethod();
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final HashMap<String, String> queryMap = new HashMap<>();
                        queryMap.put("transport", Polling.NAME);
                        return queryMap;
                    }
                }).when(request).getAttribute("query");
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return "application/octet-stream";
                    }
                }).when(request).getContentType();
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return data.length;
                    }
                }).when(request).getContentLength();
                try {
                    Mockito.doAnswer(new Answer() {
                        @Override
                        public Object answer(InvocationOnMock invocationOnMock) {
                            return new ServletInputStreamWrapper(requestInputStream);
                        }
                    }).when(request).getInputStream();
                } catch (IOException ignore) {
                }

                final HttpServletResponseImpl response = new HttpServletResponseImpl();

                try {
                    polling.onRequest(request, response);
                } catch (IOException ignore) {
                }

                Mockito.verify(polling, Mockito.times(1))
                        .emit(Mockito.eq("packet"), Mockito.any(Packet.class));
            }
        });
    }

    @Test
    public void testClose_client() {
        final Polling polling = Mockito.spy(new Polling());

        final Packet<String> requestPacket = new Packet<>(Packet.CLOSE);
        ServerParser.encodePayloadAsBinary(new Packet[]{requestPacket}, new Parser.EncodeCallback<byte[]>() {
            @Override
            public void call(final byte[] data) {
                final ByteArrayInputStream requestInputStream = new ByteArrayInputStream(data);
                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return "POST";
                    }
                }).when(request).getMethod();
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final HashMap<String, String> queryMap = new HashMap<>();
                        queryMap.put("transport", Polling.NAME);
                        return queryMap;
                    }
                }).when(request).getAttribute("query");
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return "application/octet-stream";
                    }
                }).when(request).getContentType();
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return data.length;
                    }
                }).when(request).getContentLength();
                try {
                    Mockito.doAnswer(new Answer() {
                        @Override
                        public Object answer(InvocationOnMock invocationOnMock) {
                            return new ServletInputStreamWrapper(requestInputStream);
                        }
                    }).when(request).getInputStream();
                } catch (IOException ignore) {
                }

                final HttpServletResponseImpl response = new HttpServletResponseImpl();

                try {
                    polling.onRequest(request, response);
                } catch (IOException ignore) {
                }

                Mockito.verify(polling, Mockito.times(1))
                        .emit(Mockito.eq("close"));
            }
        });
    }

    @Test
    public void testClose_server1() throws IOException {
        final Polling polling = Mockito.spy(new Polling());

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return "GET";
            }
        }).when(request).getMethod();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", Polling.NAME);
                return queryMap;
            }
        }).when(request).getAttribute("query");

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        polling.close();
        polling.onRequest(request, response);

        Mockito.verify(polling, Mockito.times(1))
                .emit(Mockito.eq("drain"));
        Mockito.verify(polling, Mockito.times(1))
                .emit(Mockito.eq("close"));

        final String responseString = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        ServerParser.decodePayload(responseString, new Parser.DecodePayloadCallback() {
            @Override
            public boolean call(Packet packet, int index, int total) {
                assertEquals(2, total);
                if (index == 0) {
                    assertEquals(Packet.NOOP, packet.type);
                } else if (index == 1) {
                    assertEquals(Packet.CLOSE, packet.type);
                }
                return true;
            }
        });
    }

    @Test
    public void testClose_server2() {
        final Polling polling = Mockito.spy(new Polling());

        polling.on("drain", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                polling.close();
            }
        });

        final Packet<String> requestPacket = new Packet<>(Packet.CLOSE);
        ServerParser.encodePayloadAsBinary(new Packet[]{requestPacket}, new Parser.EncodeCallback<byte[]>() {
            @SuppressWarnings("Duplicates")
            @Override
            public void call(final byte[] data) {
                final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return "GET";
                    }
                }).when(request).getMethod();
                Mockito.doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final HashMap<String, String> queryMap = new HashMap<>();
                        queryMap.put("transport", Polling.NAME);
                        return queryMap;
                    }
                }).when(request).getAttribute("query");

                final HttpServletResponseImpl response = new HttpServletResponseImpl();

                try {
                    polling.onRequest(request, response);
                } catch (IOException ignore) {
                }

                Mockito.verify(polling, Mockito.times(1))
                        .emit(Mockito.eq("drain"));
                Mockito.verify(polling, Mockito.times(1))
                        .emit(Mockito.eq("close"));

                final String responseString = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
                ServerParser.decodePayload(responseString, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        assertEquals(1, total);
                        assertEquals(Packet.CLOSE, packet.type);
                        return true;
                    }
                });
            }
        });
    }
}