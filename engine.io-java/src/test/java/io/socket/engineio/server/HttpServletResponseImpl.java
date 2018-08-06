package io.socket.engineio.server;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public final class HttpServletResponseImpl implements HttpServletResponse {

    private final ServletOutputStreamWrapper mServletOutputStreamWrapper = new ServletOutputStreamWrapper();
    private final ArrayList<Cookie> mCookieList = new ArrayList<>();

    private int mStatus = HttpServletResponse.SC_OK;

    @Override
    public void addCookie(Cookie cookie) {
        mCookieList.add(cookie);
    }

    @Override
    public boolean containsHeader(String s) {
        return false;
    }

    @Override
    public String encodeURL(String s) {
        return URLEncoder.encode(s);
    }

    @Override
    public String encodeRedirectURL(String s) {
        return URLEncoder.encode(s);
    }

    @Override
    public String encodeUrl(String s) {
        return URLEncoder.encode(s);
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return URLEncoder.encode(s);
    }

    @Override
    public void sendError(int i, String s) {
    }

    @Override
    public void sendError(int i) {
    }

    @Override
    public void sendRedirect(String s) {

    }

    @Override
    public void setDateHeader(String s, long l) {

    }

    @Override
    public void addDateHeader(String s, long l) {

    }

    @Override
    public void setHeader(String s, String s1) {

    }

    @Override
    public void addHeader(String s, String s1) {

    }

    @Override
    public void setIntHeader(String s, int i) {

    }

    @Override
    public void addIntHeader(String s, int i) {

    }

    @Override
    public void setStatus(int i) {
        mStatus = i;
    }

    @Override
    public void setStatus(int i, String s) {
        mStatus = i;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public String getHeader(String s) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return mServletOutputStreamWrapper;
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(mServletOutputStreamWrapper);
    }

    @Override
    public void setCharacterEncoding(String s) {

    }

    @Override
    public void setContentLength(int i) {

    }

    @Override
    public void setContentLengthLong(long l) {

    }

    @Override
    public void setContentType(String s) {

    }

    @Override
    public void setBufferSize(int i) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {

    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale locale) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    public ArrayList<Cookie> getCookieList() {
        return mCookieList;
    }

    public ByteArrayOutputStream getByteOutputStream() {
        return mServletOutputStreamWrapper.getByteOutputStream();
    }
}
