package de.rwth.idsg.steve.extensions.plugsurfing.interceptor;

import com.google.common.collect.Lists;
import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.List;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Slf4j
public class ClientApiKeyHeaderInterceptor implements ClientHttpRequestInterceptor {

    private static final List<String> contentType = Lists.newArrayList(MediaType.APPLICATION_JSON_VALUE);

    private static final String HEADER_KEY = "Authorization";
    private static final String HEADER_CLIENT_VALUE = "key=" + Constants.CONFIG.getApiKey();

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
}
