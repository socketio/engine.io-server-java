package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;
import io.socket.engineio.server.parser.Parser;
import io.socket.engineio.server.parser.ParserV4;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.utils.ParseQS;
import io.socket.engineio.server.utils.ServerYeast;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class DeadLockTest {

	/**
	 * Tests for deadlock of server.
	 * If this runs for 60 seconds, we consider it a success.
	 */
	@Test
	public void testTransportDeadlock() {
		final Object lockObject = new Object();
		final EngineIoSocket socket = new EngineIoSocket(lockObject,
				ServerYeast.yeast(),
				ParserV4.PROTOCOL,
				new EngineIoServer(),
				Executors.newSingleThreadScheduledExecutor());
		final Transport transport = new Polling(lockObject, Parser.PROTOCOL_V4);
		socket.init(transport);

		final HttpServletRequest request = getConnectRequest(new HashMap<String, String>() {{
				put("transport", Polling.NAME);
		}});

		final boolean[] terminate = new boolean[3];

		final Thread sender = new Thread(() -> {
			while (!terminate[1]) {
				Packet<String> packet = new Packet<>(Packet.NOOP);
				socket.send(packet);
			}
		});
		sender.start();

		final Thread poller = new Thread(() -> {
			while (!terminate[2]) {
				HttpServletResponseImpl response = new HttpServletResponseImpl();
				try {
					socket.onRequest(request, response);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		});
		poller.start();

		final Thread interrupter = new Thread(() -> {
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException ignore) {
			}

			terminate[0] = true;
		});
		interrupter.start();

		boolean isDeadlocked = false;
		while (!terminate[0]) {
			ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
			long[] deadlockedThreadIds = mbean.findDeadlockedThreads();
			if (deadlockedThreadIds != null && deadlockedThreadIds.length > 1) {
				/*ThreadInfo[] threadInfos = mbean.getThreadInfo(deadlockedThreadIds);
				Map<Thread, StackTraceElement[]> stackTraceMap = Thread.getAllStackTraces();
				for (ThreadInfo threadInfo : threadInfos) {
					if (threadInfo != null) {
						for (Thread thread : stackTraceMap.keySet()) {
							if (thread.getId() == threadInfo.getThreadId()) {
								System.out.println(threadInfo.toString().trim());

								for (StackTraceElement ste : thread.getStackTrace()) {
									System.out.println("\t" + ste.toString().trim());
								}
							}
						}
					}
				}*/
				isDeadlocked = true;
				terminate[0] = true;
			}
		}

		terminate[1] = true;
		terminate[2] = true;

		Assert.assertFalse(isDeadlocked);
	}

	private HttpServletRequest getConnectRequest(final Map<String, String> query) {
		final HashMap<String, Object> attributes = new HashMap<>();
		attributes.put("transport", Polling.NAME);
		attributes.put("j", "100");
		attributes.put("query", query);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.doAnswer(invocationOnMock -> ParseQS.encode(query)).when(request).getQueryString();
		Mockito.when(request.getMethod()).thenReturn("GET");
		Mockito.doAnswer(invocationOnMock -> {
			final String name = invocationOnMock.getArgument(0);
			final Object value = invocationOnMock.getArgument(1);
			attributes.put(name, value);
			return null;
		}).when(request).setAttribute(Mockito.anyString(), Mockito.any());
		Mockito.doAnswer(invocationOnMock -> attributes.get((String)invocationOnMock.getArgument(0)))
				.when(request)
				.getAttribute(Mockito.anyString());
		return request;
	}
}