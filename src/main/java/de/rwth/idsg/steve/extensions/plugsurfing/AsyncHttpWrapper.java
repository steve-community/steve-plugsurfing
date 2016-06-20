package de.rwth.idsg.steve.extensions.plugsurfing;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.rwth.idsg.steve.extensions.plugsurfing.model.ErrorResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 20.06.2016
 */
@Slf4j
public class AsyncHttpWrapper {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private final AtomicBoolean responseSent = new AtomicBoolean(false);
    private final long id;

    public AsyncHttpWrapper(HttpServletRequest request, HttpServletResponse response, long id) {
        this.request = request;
        this.response = response;
        this.id = id;
    }

    public String parseRequestBody() throws IOException {
        String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        logRequest(requestBody);
        return requestBody;
    }

    public void finishExceptionally(Throwable t) {
        HttpStatus status;
        String msg;

        if (t instanceof PsApiException) {
            PsApiException ps = (PsApiException) t;
            status = ps.getResponseStatus();
            msg = ps.getMessage();

        } else if (t instanceof JsonProcessingException) {
            status = HttpStatus.BAD_REQUEST;
            msg = "Cannot parse request";

        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            msg = t.getMessage();
        }

        finishError(new ErrorResponse(msg), status);
    }

    private void finishError(ErrorResponse objectToSend, HttpStatus status) {
        String jsonResponse = PsApiJsonParser.SINGLETON.serializeOrNull(objectToSend);
        if (jsonResponse == null) {
            sendInternalError();
        } else {
            sendJson(jsonResponse, status);
        }
    }

    public void finishSuccess(SuccessResponse objectToSend) {
        String jsonResponse = PsApiJsonParser.SINGLETON.serializeOrNull(objectToSend);
        if (jsonResponse == null) {
            sendInternalError();
        } else {
            sendJson(jsonResponse, HttpStatus.OK);
        }
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    private void logRequest(String requestBody) {
        log.info("[CorrelationId:{}] PS Request: {}", id, requestBody);
    }

    private void logResponse(HttpStatus status, String responseBody) {
        log.info("[CorrelationId:{}] PS Response: statusCode={}; payload={}", id, status.value(), responseBody);
    }

    // -------------------------------------------------------------------------
    // Async
    // -------------------------------------------------------------------------

    public void startAsync() {
        AsyncContext ctx = request.startAsync(request, response);
        ctx.addListener(new AsyncListener() {

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                log.debug("onStartAsync {}", id);
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.debug("onComplete {}", id);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log.debug("onTimeout {}", id);
                finishError(new ErrorResponse("Timeout"), HttpStatus.REQUEST_TIMEOUT);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log.debug("onError {}", id);
                sendInternalError();
            }
        });
    }

    private void completeAsync() {
        if (request.isAsyncStarted()) {
            request.getAsyncContext().complete();
        }
    }

    // -------------------------------------------------------------------------
    // Actual response send
    // -------------------------------------------------------------------------

    private void sendInternalError() {
        if (responseSent.compareAndSet(false, true)) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            try {
                response.setStatus(status.value());
            } finally {
                logResponse(status, "");
                completeAsync();
            }
        }
    }

    private void sendJson(String jsonResponse, HttpStatus status) {
        if (responseSent.compareAndSet(false, true)) {
            PrintWriter writer;
            try {
                writer = response.getWriter();
            } catch (IOException e) {
                sendInternalError();
                return;
            }

            try {
                response.setStatus(status.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                writer.write(jsonResponse);
            } finally {
                logResponse(status, jsonResponse);
                completeAsync();
            }
        }
    }
}
