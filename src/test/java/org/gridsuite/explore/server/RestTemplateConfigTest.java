/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;


/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestTemplateConfig.class, RestTemplateConfigTest.TestConfig.class})
class RestTemplateConfigTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;
    private static final String ROLES_HEADER = "roles";
    private static final String TEST_ROLES = "ADMIN|USER";
    private static final String TEST_ENDPOINT = "http://test-service/api/resource";

    // Needed for the RestTemplateBuilder since the test doesn't load
    // the full springboot auto-configuration with @SpringBootTest
    @Configuration
    static class TestConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }
    }

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void tearDown() {
        // Clean up the RequestContextHolder after each test
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testRoleHeaderIsPropagated() {
        // Setup mock incoming request with roles header
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ROLES_HEADER, TEST_ROLES);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Setup mock response for the outgoing request
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ROLES_HEADER, TEST_ROLES)) // This verifies our interceptor works
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        // Execute request through our RestTemplate
        restTemplate.getForObject(TEST_ENDPOINT, String.class);

        // Verify the request was made correctly
        mockServer.verify();
    }

    @Test
    void testNoRoleHeaderPropagationWhenNotPresent() {
        // Setup mock incoming request WITHOUT roles header
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Setup mock response - here we expect NOT to see the roles header
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(req -> {
                    // Verify the header isn't present (would throw if present)
                    if (req.getHeaders().containsKey(ROLES_HEADER)) {
                        throw new AssertionError("Roles header should not be present");
                    }
                })
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        // Execute request
        restTemplate.getForObject(TEST_ENDPOINT, String.class);

        // Verify
        mockServer.verify();
    }

    @Test
    void testEmptyRoleHeaderNotPropagated() {
        // Setup mock incoming request with EMPTY roles header
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ROLES_HEADER, ""); // Empty value
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Setup mock - we don't expect the header to be forwarded if empty
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(req -> {
                    if (req.getHeaders().containsKey(ROLES_HEADER)) {
                        throw new AssertionError("Roles header should not be present when empty");
                    }
                })
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        // Execute request
        restTemplate.getForObject(TEST_ENDPOINT, String.class);

        // Verify
        mockServer.verify();
    }

    @Test
    void testContextHolderIsNull() {
        // Make sure the context holder is null
        RequestContextHolder.resetRequestAttributes();

        // Setup mock - we don't expect any header forwarding when no request context exists
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    if (request.getHeaders().containsKey(ROLES_HEADER)) {
                        throw new AssertionError("Roles header should not be present when RequestContextHolder is null");
                    }
                })
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        // Execute request
        restTemplate.getForObject(TEST_ENDPOINT, String.class);

        // Verify
        mockServer.verify();
    }
}
