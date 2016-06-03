package de.rwth.idsg.steve.extensions.plugsurfing.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 03.06.2016
 */
@Slf4j
public class ResourceApiKeyHeaderInterceptor extends HandlerInterceptorAdapter {

    private static final String HEADER_KEY = "Authorization";

    private final String HEADER_RESOURCE_VALUE;

    public ResourceApiKeyHeaderInterceptor(String stevePsApiKey) {
        HEADER_RESOURCE_VALUE = "key=" + stevePsApiKey;
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
