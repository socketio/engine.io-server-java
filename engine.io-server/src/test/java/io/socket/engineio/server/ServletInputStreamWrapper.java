package io.socket.engineio.server;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.InputStream;

public final class ServletInputStreamWrapper extends ServletInputStream {

    private InputStream mInputStream;

    public ServletInputStreamWrapper(InputStream inputStream) {
        mInputStream = inputStream;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mInputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return mInputStream.read(b, off, len);
    }

    @Override
    public int read() throws IOException {
        return mInputStream.read();
    }

    @Override
    public long skip(long n) throws IOException {
        return mInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return mInputStream.available();
    }

    @Override
    public void close() throws IOException {
        mInputStream.close();
    }

    @Override
    public synchronized void reset() throws IOException {
        mInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return mInputStream.markSupported();
    }

    @Override
    public synchronized void mark(int readlimit) {
        mInputStream.mark(readlimit);
    }
}