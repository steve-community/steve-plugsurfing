package de.rwth.idsg.steve.extensions.plugsurfing;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Component
@Slf4j
public class ApiKeyHeaderInterceptor extends HandlerInterceptorAdapter implements ClientHttpRequestInterceptor {

    private static final List<String> contentType = Lists.newArrayList(MediaType.APPLICATION_JSON_VALUE);

    private static final String HEADER_KEY = "Authorization";
    private static final String HEADER_CLIENT_VALUE = "key=" + Constants.CONFIG.getApiKey();

    // Since we only register the interceptor if the property is set,
    // at this point we can just call get() of the Optional
    //
    private static final String HEADER_RESOURCE_VALUE = "key=" + Constants.CONFIG.getStevePsApiKey().get();

    // -------------------------------------------------------------------------
    // Steve as REST client
    // -------------------------------------------------------------------------

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        customizeHeaders(request.getHeaders());
        return execution.execute(request, body);
    }

    private void customizeHeaders(HttpHeaders headers) {

        // Set the API key in order to have access to the PlugSurfing API
        //
        headers.add(HEADER_KEY, HEADER_CLIENT_VALUE);

        // Problem description:
        // Apache HTTPClient automatically sets "Content-Type: application/json;charset=UTF-8"
        // But PS strictly expects: "Content-Type: application/json"
        //
        headers.replace(HttpHeaders.CONTENT_TYPE, contentType);
    }

    // -------------------------------------------------------------------------
    // Steve as REST resource
    // -------------------------------------------------------------------------

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String value = request.getHeader(HEADER_KEY);

        if (isValid(value)) {
            // Continue processing with the handler chain
            return true;
        } else {
            log.error("Unauthorized incoming PS Request [API key missing or invalid]. Dropping the message.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Break the chain and return 401
            return false;
        }
    }

    private boolean isValid(String value) {
        return HEADER_RESOURCE_VALUE.equals(value);
    }
}
