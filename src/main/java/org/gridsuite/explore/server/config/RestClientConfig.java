/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreConstants.HEADER_ROLES;
import static org.gridsuite.explore.server.ExploreConstants.HEADER_USER_ID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Configuration
public class RestClientConfig {
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> builder
            .messageConverters(messageConverters -> {
                //find and replace Jackson message converter with our own
                for (int i = 0; i < messageConverters.size(); i++) {
                    final HttpMessageConverter<?> httpMessageConverter = messageConverters.get(i);
                    if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
                        messageConverters.set(i, mappingJackson2HttpMessageConverter());
                    }
                }
            })
            .requestInterceptor(new HeaderForwardingInterceptor());
    }

    /**
     * In our microservice architecture, user permissions (userId and roles) must be preserved when
     * one service calls another. This interceptor automatically forwards the "userId" and "roles" headers
     * from incoming requests to any outgoing REST calls made with this RestTemplate.
     *
     * Without this interceptor, authorization information would be lost in service-to-service
     * communication.
     */
    public static class HeaderForwardingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                request.getHeaders().set(HEADER_USER_ID, authentication.getName());

                // The roles are sent through a single HTTP header, so the authorities must be joined
                // with "|" before forwarding them to the next service
                String roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining("|"));
                if (!roles.isEmpty()) {
                    request.getHeaders().set(HEADER_ROLES, roles);
                }
            }

            return execution.execute(request, body);
        }
    }

    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    private ObjectMapper createObjectMapper() {
        var objectMapper = Jackson2ObjectMapperBuilder.json().build();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JsonComponentModule());
        return objectMapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return createObjectMapper();
    }
}
