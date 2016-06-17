package de.rwth.idsg.steve.extensions.plugsurfing.interceptor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logs HTTP request/response bodies
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 16.06.2016
 */
@Slf4j
public class ResourceLogFilter extends OncePerRequestFilter {

    private AtomicLong counter = new AtomicLong(0);

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long id = -1;

        if (!(request instanceof RequestWrapper)) {
            id = counter.incrementAndGet();
            request = new RequestWrapper(request, id);
        }

        if (!(response instanceof ResponseWrapper)) {
            response = new ResponseWrapper(response, id);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            switch (request.getDispatcherType()) {
                case REQUEST:
                    // After receiving the request
                    logRequest(request);

                    // This is the case, when we cannot validate the request and send an error response back,
                    // before even communicating with a charging station.
                    if (!isAsyncStarted(request)) {
                        logResponse(response);
                    }
                    break;

                case ASYNC:
                    // After receiving the response from the charging station and having mapped it to a PS response
                    logResponse(response);
                    break;

                default:
                    break;
            }
        }
    }

    private void logRequest(HttpServletRequest request) {
        if (request instanceof RequestWrapper) {
            RequestWrapper wrap = (RequestWrapper) request;
            String s = new String(wrap.toByteArray(), StandardCharsets.UTF_8);
            log.info("[CorrelationId:{}] PS Request: {}", wrap.getId(), s);
        }
    }

    private void logResponse(HttpServletResponse response) {
        if (response instanceof ResponseWrapper) {
            ResponseWrapper wrap = (ResponseWrapper) response;
            String s = new String(wrap.toByteArray(), StandardCharsets.UTF_8);
            log.info("[CorrelationId:{}] PS Response: {}", wrap.getId(), s);
        }
    }

    // -------------------------------------------------------------------------
    // Request/response wrappers
    // -------------------------------------------------------------------------

    private static class RequestWrapper extends HttpServletRequestWrapper {

        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Getter private final long id;

        RequestWrapper(HttpServletRequest request, long id) throws IOException {
            super(request);
            this.id = id;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new TeyInputStream(RequestWrapper.super.getInputStream(), bos);
        }

        byte[] toByteArray() {
            return bos.toByteArray();
        }
    }

    private static class ResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(bos);

        @Getter private final long id;

        ResponseWrapper(HttpServletResponse response, long id) {
            super(response);
            this.id = id;
        }

        @Override
        public ServletResponse getResponse() {
            return this;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new TeyOutputStream(ResponseWrapper.super.getOutputStream(), bos);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new TeyPrintWriter(super.getWriter(), writer);
        }

        byte[] toByteArray(){
            return bos.toByteArray();
        }
    }

    // -------------------------------------------------------------------------
    // Copiers
    // -------------------------------------------------------------------------

    private static class TeyInputStream extends ServletInputStream {

        private final ServletInputStream main;
        private final ByteArrayOutputStream branch;

        TeyInputStream(ServletInputStream main, ByteArrayOutputStream branch) {
            this.main = main;
            this.branch = branch;
        }

        @Override
        public int read() throws IOException {
            int ch = main.read();
            if (ch != -1) {
                branch.write(ch);
            }
            return ch;
        }

        @Override
        public boolean isFinished() {
            // Auto-generated method stub
            return false;
        }

        @Override
        public boolean isReady() {
            // Auto-generated method stub
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Auto-generated method stub
        }
    }

    private static class TeyOutputStream extends ServletOutputStream {

        private final ServletOutputStream main;
        private final ByteArrayOutputStream branch;

        TeyOutputStream(ServletOutputStream main, ByteArrayOutputStream branch) {
            this.main = main;
            this.branch = branch;
        }

        @Override
        public void write(int b) throws IOException {
            main.write(b);
            branch.write(b);
        }

        @Override
        public boolean isReady() {
            // Auto-generated method stub
            return false;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            // Auto-generated method stub
        }
    }

    private static class TeyPrintWriter extends PrintWriter {

        private final PrintWriter branch;

        TeyPrintWriter(PrintWriter main, PrintWriter branch) {
            super(main, true);
            this.branch = branch;
        }

        @Override
        public void write(char buf[], int off, int len) {
            super.write(buf, off, len);
            super.flush();
            branch.write(buf, off, len);
            branch.flush();
        }

        @Override
        public void write(String s, int off, int len) {
            super.write(s, off, len);
            super.flush();
            branch.write(s, off, len);
            branch.flush();
        }

        @Override
        public void write(int c) {
            super.write(c);
            super.flush();
            branch.write(c);
            branch.flush();
        }

        @Override
        public void flush() {
            super.flush();
            branch.flush();
        }
    }
}
