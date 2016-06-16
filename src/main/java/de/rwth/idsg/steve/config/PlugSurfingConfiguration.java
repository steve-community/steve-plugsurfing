package de.rwth.idsg.steve.config;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.interceptor.ClientApiKeyHeaderInterceptor;
import de.rwth.idsg.steve.extensions.plugsurfing.interceptor.ClientLogInterceptor;
import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiJsonParser;
import de.rwth.idsg.steve.extensions.plugsurfing.interceptor.ResourceApiKeyHeaderInterceptor;
import de.rwth.idsg.steve.extensions.plugsurfing.interceptor.ResourceLogFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Configuration
public class PlugSurfingConfiguration extends WebMvcConfigurerAdapter {

    @Autowired private ServletContext servletContext;

    @PostConstruct
    private void init() {
        FilterRegistration.Dynamic logFilter = servletContext.addFilter("psLogFilter", new ResourceLogFilter());
        logFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/ps-api/*");
        logFilter.setAsyncSupported(true);
    }

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        // In-going serialization
        converters.add(getMessageConverter());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Optional<String> optionalApiKey = Constants.CONFIG.getStevePsApiKey();

        // Register only if the property is set
        if (optionalApiKey.isPresent()) {
            registry.addInterceptor(new ResourceApiKeyHeaderInterceptor(optionalApiKey.get()))
                    .addPathPatterns("/ps-api/**");
        }
    }

    @Bean
    public RestTemplate normalTemplate() {
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new ClientApiKeyHeaderInterceptor());
        interceptors.add(new ClientLogInterceptor());

        // Out-going serialization
        RestTemplate restTemplate = new RestTemplate(Collections.singletonList(getMessageConverter()));
        restTemplate.setInterceptors(interceptors);
        restTemplate.setRequestFactory(
                new BufferingClientHttpRequestFactory(
                        new HttpComponentsClientHttpRequestFactory()));

        return restTemplate;
    }

    @Bean
    public MappingJackson2HttpMessageConverter getMessageConverter() {
        return new MappingJackson2HttpMessageConverter(PsApiJsonParser.SINGLETON.getMapper());
    }

}
