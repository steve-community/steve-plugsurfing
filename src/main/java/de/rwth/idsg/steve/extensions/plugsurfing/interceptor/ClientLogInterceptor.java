package de.rwth.idsg.steve.extensions.plugsurfing.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 01.03.2016
 */
@Slf4j
public class ClientLogInterceptor implements ClientHttpRequestInterceptor {

    private static final String PREFIX_FORMAT = "[CorrelationId:%d]";
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        String prefix = String.format(PREFIX_FORMAT, counter.incrementAndGet());

        logRequest(prefix, body);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(prefix, response);
        return response;
    }

    private void logRequest(String prefix, byte[] body) throws IOException {
        String message = getStringMessage(body);
        log.info("{} PS Request: payload={}", prefix, message);
    }

    private void logResponse(String prefix, ClientHttpResponse response) throws IOException {
        String message = getStringMessage(response.getBody());
        log.info("{} PS Response: statusCode={}; payload={}", prefix, response.getRawStatusCode(), message);
    }

    private String getStringMessage(InputStream in) throws IOException {
        byte[] body = StreamUtils.copyToByteArray(in);
        return getStringMessage(body);
    }

    private String getStringMessage(byte[] body) throws UnsupportedEncodingException {
        return new String(body, StandardCharsets.UTF_8.name());
    }
}
