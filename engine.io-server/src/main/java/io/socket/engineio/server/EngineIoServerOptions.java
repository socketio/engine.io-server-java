package io.socket.engineio.server;

import io.socket.engineio.parser.Packet;

import java.util.Arrays;
import java.util.Comparator;

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
        DEFAULT.setPingTimeout(5000);
        DEFAULT.setPingInterval(25000);
        DEFAULT.setAllowedCorsOrigins(ALLOWED_CORS_ORIGIN_ALL);
        DEFAULT.setMaxTimeoutThreadPoolSize(20);
        DEFAULT.lock();
    }

    private boolean mIsLocked;
    private boolean mCorsHandlingDisabled;
    private long mPingInterval;
    private long mPingTimeout;
    private String[] mAllowedCorsOrigins;
    private Packet<Object> mInitialPacket;
    private int mMaxTimeoutThreadPoolSize;

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
                .setPingInterval(DEFAULT.getPingInterval())
                .setPingTimeout(DEFAULT.getPingTimeout())
                .setAllowedCorsOrigins(DEFAULT.getAllowedCorsOrigins())
                .setMaxTimeoutThreadPoolSize(DEFAULT.getMaxTimeoutThreadPoolSize())
                .setInitialPacket(null);
    }

    /**
     * Gets the value of 'isCorsHandlingDisabled' option.
     *
     * @return Boolean value indicating if CORS handling is disabled.
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
     * Gets the ping interval option.
     *
     * @return Ping interval in milliseconds.
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
     * Gets the ping timeout option.
     *
     * @return Ping timeout in milliseconds.
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
     *
     * @return Array of strings containing allowed CORS origins.
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

            // Sort the array for faster search on request
            Arrays.sort(mAllowedCorsOrigins, Comparator.naturalOrder());
        }

        return this;
    }

    /**
     * Gets the initial packet option.
     *
     * @return Initial packet.
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
     *
     * @return Max threadpool size for ping timeout timers.
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
}
