package io.socket.engineio.server;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ServletOutputStreamWrapper extends ServletOutputStream {

    private final ByteArrayOutputStream mByteOutputStream = new ByteArrayOutputStream();

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }

    @Override
    public void write(byte[] b) throws IOException {
        mByteOutputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        mByteOutputStream.write(b, off, len);
    }

    @Override
    public void write(int b) {
        mByteOutputStream.write(b);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    ByteArrayOutputStream getByteStream() {
        return mByteOutputStream;
    }
}
