package io.socket.engineio.server;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class EngineIoSocketTimeoutHandler {

    private final ExecutorService executorPool;
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * Constructs a new socket timeout handler.
     * The purpose of this class is to dynamically handle concurrent timeout tasks using
     * a resource efficient thread pool.
     *
     * The {@link ScheduledExecutorService} can't have a dynamic expanding/contracting thread pool
     * so the task is sent to a {@link ThreadPoolExecutor) for execution once the timeout occurs.
     *
     * The two {@link EngineIoThreadFactory} instances are used to name the threads appropriately.
     *
     * @param maxThreadPoolSize The maximum thread pool size to expand to
     */
    EngineIoSocketTimeoutHandler(int maxThreadPoolSize) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new EngineIoThreadFactory("scheduler"));

        executorPool = new ThreadPoolExecutor(
                1,
                maxThreadPoolSize,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(true),
                new EngineIoThreadFactory("pool"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }


    /**
     * Schedules the socketTimeoutTask on the executorScheduler.
     * Upon the scheduler timeout, the task will be posted to the dynamic executorPool for execution.
     *
     * @param socketTimeoutTask The task to schedule on the pool
     * @param timeout The timeout of the task
     * @param timeUnit The TimeUnit of the timeout
     *
     * @return The {@link ScheduledFuture} to enable cancellation of the task before its timeout
     */
    ScheduledFuture schedule(Runnable socketTimeoutTask, long timeout, TimeUnit timeUnit) {
        Runnable executorTask = () -> executorPool.execute(socketTimeoutTask);
        return scheduledExecutorService.schedule(executorTask, timeout, timeUnit);
    }


    /**
     * {@link ThreadFactory} for naming the thread pool threads with appropriate names
     */
    private static class EngineIoThreadFactory implements ThreadFactory {

        private AtomicInteger count = new AtomicInteger();
        private String mGroupNamePostfix;

        EngineIoThreadFactory(String groupNamePostfix) {
            mGroupNamePostfix = groupNamePostfix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(String.format("engineIo-socketTimeout-%s-%d", mGroupNamePostfix, count.incrementAndGet()));
            thread.setDaemon(true);
            return thread;
        }
    }

}
