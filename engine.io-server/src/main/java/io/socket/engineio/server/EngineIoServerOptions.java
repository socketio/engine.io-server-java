package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Options for {@link EngineIoServer}
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public final class EngineIoServerOptions {

    /**
     * The default options used by server.
     * This instance is locked and cannot be modified.
     *
     * ping timeout: 5000
     * ping interval: 25000
     * allowed origins: *
     */
    public static final EngineIoServerOptions DEFAULT = new EngineIoServerOptions();

    /**
     * Specify that all origins are to be allowed for CORS
     */
    public static final String[] ALLOWED_CORS_ORIGIN_ALL = null;

    /**
     * Specify that no origins are allowed for CORS
     */
    public static final String[] ALLOWED_CORS_ORIGIN_NONE = new String[0];

    static {
        DEFAULT.setCorsHandlingDisabled(false);
        DEFAULT.setPingTimeout(20000);
        DEFAULT.setPingInterval(25000);
        DEFAULT.setAllowedCorsOrigins(ALLOWED_CORS_ORIGIN_ALL);
        DEFAULT.setMaxTimeoutThreadPoolSize(20);
        DEFAULT.lock();
    }

    private boolean mIsLocked;
    private boolean mCorsHandlingDisabled;
    private boolean mAllowSyncPolling;
    private long mPingInterval;
    private long mPingTimeout;
    private String[] mAllowedCorsOrigins;
    private Packet<Object> mInitialPacket;
    private int mMaxTimeoutThreadPoolSize;
    private ScheduledExecutorService mScheduledExecutorService;
    private EngineIoServer.HandshakeInterceptor mHandshakeInterceptor;

    private EngineIoServerOptions() {
        mIsLocked = false;
    }

    /**
     * Create a new instance of {@link EngineIoServerOptions} by copying
     * default options.
     *
     * @return New instance of {@link EngineIoServerOptions} with default options.
     */
    public static EngineIoServerOptions newFromDefault() {
        return (new EngineIoServerOptions())
                .setCorsHandlingDisabled(DEFAULT.isCorsHandlingDisabled())
                .setAllowSyncPolling(false)
                .setPingInterval(DEFAULT.getPingInterval())
                .setPingTimeout(DEFAULT.getPingTimeout())
                .setAllowedCorsOrigins(DEFAULT.getAllowedCorsOrigins())
                .setMaxTimeoutThreadPoolSize(DEFAULT.getMaxTimeoutThreadPoolSize())
                .setInitialPacket(null);
    }

    /**
     * Gets the value of 'isCorsHandlingDisabled' option.
     */
    public boolean isCorsHandlingDisabled() {
        return mCorsHandlingDisabled;
    }

    /**
     * Sets the 'isCorsHandlingDisabled' option.
     *
     * @param corsHandlingDisabled Boolean value for disabling CORS handling.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     */
    public EngineIoServerOptions setCorsHandlingDisabled(boolean corsHandlingDisabled) throws IllegalStateException {
        if (mIsLocked) {
            throw new IllegalStateException("CORS handling cannot be set. Instance is locked.");
        }

        mCorsHandlingDisabled = corsHandlingDisabled;
        return this;
    }

    /**
     * Whether sync polling transport is allowed or not.
     *
     * WARNING: Sync polling can cause the client to flood the server with requests.
     */
    public boolean isSyncPollingAllowed() {
        return mAllowSyncPolling;
    }

    /**
     * Sets whether sync polling transport is allowed or not.
     *
     * WARNING: Sync polling can cause the client to flood the server with requests.
     */
    public EngineIoServerOptions setAllowSyncPolling(boolean allowSyncPolling) {
        if (mIsLocked) {
            throw new IllegalStateException("Sync polling cannot be set. Instance is locked.");
        }

        mAllowSyncPolling = allowSyncPolling;
        return this;
    }

    /**
     * Gets the ping interval option in milliseconds.
     */
    public long getPingInterval() {
        return mPingInterval;
    }

    /**
     * Sets the ping interval option.
     *
     * @param pingInterval Ping interval in milliseconds.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     */
    public EngineIoServerOptions setPingInterval(long pingInterval) throws IllegalStateException {
        if (mIsLocked) {
            throw new IllegalStateException("Ping interval cannot be set. Instance is locked.");
        }

        mPingInterval = pingInterval;
        return this;
    }

    /**
     * Gets the ping timeout option in milliseconds.
     */
    public long getPingTimeout() {
        return mPingTimeout;
    }

    /**
     * Sets the ping timeout option.
     *
     * @param pingTimeout Ping timeout in milliseconds.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     */
    public EngineIoServerOptions setPingTimeout(long pingTimeout) throws IllegalStateException {
        if (mIsLocked) {
            throw new IllegalStateException("Ping timeout cannot be set. Instance is locked.");
        }

        mPingTimeout = pingTimeout;
        return this;
    }

    /**
     * Gets the allowed CORS origins option.
     */
    public String[] getAllowedCorsOrigins() {
        return mAllowedCorsOrigins;
    }

    /**
     * Sets the allowed CORS origins option.
     *
     * @param allowedCorsOrigins Array of strings containing allowed CORS origins.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     */
    public EngineIoServerOptions setAllowedCorsOrigins(String[] allowedCorsOrigins) throws IllegalStateException {
        if (mIsLocked) {
            throw new IllegalStateException("Allowed CORS origins cannot be set. Instance is locked.");
        }

        mAllowedCorsOrigins = null;
        if (allowedCorsOrigins != null) {
            // Copy the array to prevent outside modifications
            mAllowedCorsOrigins = new String[allowedCorsOrigins.length];
            System.arraycopy(allowedCorsOrigins, 0, mAllowedCorsOrigins, 0, allowedCorsOrigins.length);
        }

        return this;
    }

    /**
     * Gets the initial packet option.
     */
    public Packet<?> getInitialPacket() {
        return mInitialPacket;
    }

    /**
     * Sets the initial packet.
     *
     * @param initialPacket The initial packet to send on client connection.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     * @throws IllegalArgumentException If initialPacket.type is not message or data is null.
     */
    public EngineIoServerOptions setInitialPacket(Packet<?> initialPacket) throws IllegalStateException, IllegalArgumentException {
        if (mIsLocked) {
            throw new IllegalStateException("Initial packet cannot be set. Instance is locked.");
        }

        if (initialPacket != null) {
            if ((initialPacket.type == null) || !initialPacket.type.equals(Packet.MESSAGE)) {
                throw new IllegalArgumentException("Initial packet must be a message packet.");
            }
            if (initialPacket.data == null) {
                throw new IllegalArgumentException("Initial packet data must not be null.");
            }
        }

        mInitialPacket = null;
        if (initialPacket != null) {
            mInitialPacket = new Packet<>(initialPacket.type);
            mInitialPacket.data = initialPacket.data;
        }
        return this;
    }

    /**
     * Lock this options instance to prevent modifications.
     */
    public void lock() {
        mIsLocked = true;
    }


    /**
     * Gets the max threadpool size for the ping timeout timers.
     */
    public int getMaxTimeoutThreadPoolSize() {
        return mMaxTimeoutThreadPoolSize;
    }

    /**
     * Sets the max threadpool size for the ping timeout timers.
     *
     * @param maxTimeoutThreadPoolSize Max threadpool size for handling ping timeouts.
     * @return Instance for chaining.
     * @throws IllegalStateException If instance is locked.
     */
    public EngineIoServerOptions setMaxTimeoutThreadPoolSize(int maxTimeoutThreadPoolSize) throws IllegalStateException {
        if (mIsLocked) {
            throw new IllegalStateException("Max timeout thread pool size cannot be set. Instance is locked.");
        }

        mMaxTimeoutThreadPoolSize = maxTimeoutThreadPoolSize;
        return this;
    }

    /**
     * Gets the custom {@link ScheduledExecutorService} for the server to use or null to let
     * the server create it's own executor.
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return mScheduledExecutorService;
    }

    /**
     * Sets a custom {@link ScheduledExecutorService} for the server to use.
     * This might be a good option if the application using this library already has
     * an executor service it manages and uses.
     *
     * NOTE: Caller is responsible for shutting down this executor.
     *
     * @param scheduledExecutorService Custom {@link ScheduledExecutorService} to use for timed tasks.
     */
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        if (mIsLocked) {
            throw new IllegalStateException("Executor service cannot be set. Instance is locked.");
        }

        mScheduledExecutorService = scheduledExecutorService;
    }

    /**
     * Gets the {@link io.socket.engineio.server.EngineIoServer.HandshakeInterceptor} for the server.
     * {@link io.socket.engineio.server.EngineIoServer.HandshakeInterceptor} can be used to allow or block handshake.
     */
    public EngineIoServer.HandshakeInterceptor getHandshakeInterceptor() {
        return mHandshakeInterceptor;
    }

    /**
     * Sets a {@link io.socket.engineio.server.EngineIoServer.HandshakeInterceptor} for the server.
     * {@link io.socket.engineio.server.EngineIoServer.HandshakeInterceptor} can be used to allow or block handshake.
     *
     * @param handshakeInterceptor Interceptor object to set for the server or null to remove it.
     */
    public void setHandshakeInterceptor(EngineIoServer.HandshakeInterceptor handshakeInterceptor) {
        if (mIsLocked) {
            throw new IllegalStateException("Handshake interceptor cannot be set. Instance is locked.");
        }

        mHandshakeInterceptor = handshakeInterceptor;
    }
}
