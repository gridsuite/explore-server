/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.config;

import org.gridsuite.explore.server.UserAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;

/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@ExtendWith(SpringExtension.class)
//NOTE: this surprises given the name AutoConfigureWebClient, but this is not actually web clients,
// it is builders for webclients, and it's not just webflux webclient,
// it's also resttemplatebuilder that we need.
// And other builders are also registered but without consequences, they're just unused.
// In the future springboot 4.0.0 is supposed to have changed the name to be less surprising
@AutoConfigureWebClient
@ContextConfiguration(classes = {RestTemplateConfig.class})
class RestTemplateConfigTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;
    private static final String ROLES_HEADER = "roles";
    private static final String USER_ID_HEADER = "userId";
    private static final String TEST_ROLES = "ADMIN|USER";
    private static final String TEST_USER_ID = "user";
    private static final String TEST_ENDPOINT = "http://test-service/api/resource";

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void tearDown() {
        // Clean up the RequestContextHolder after each test
        RequestContextHolder.resetRequestAttributes(); // ça sert à quoi ??
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRoleAndUserIdHeaderIsPropagated() {
        setAuthentication(TEST_USER_ID, TEST_ROLES);

        // Setup mock response for the outgoing request
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ROLES_HEADER, TEST_ROLES))
                .andExpect(header(USER_ID_HEADER, TEST_USER_ID)) // This verifies our interceptor works
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        // Execute request through our RestTemplate
        restTemplate.getForObject(TEST_ENDPOINT, String.class);

        // Verify the request was made correctly
        mockServer.verify();
    }

    @Test
    void testNoRoleAndUserIdHeaderPropagationWhenNotPresent() {
        // Setup mock response - here we expect NOT to see the roles and userId header
        mockServer.expect(requestTo(TEST_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(req -> {
                    // Verify the header isn't present (would throw if present)
                    if (req.getHeaders().containsKey(ROLES_HEADER)) {
                        throw new AssertionError("Roles header should not be present");
                    }
                    if (req.getHeaders().containsKey(USER_ID_HEADER)) {
                        throw new AssertionError("UserId header should not be present");
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
        setAuthentication(TEST_USER_ID, "");

        // Setup mock - we don't expect the roles header to be forwarded if empty
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

    // TODO: delete
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

    void setAuthentication(String userId, String roles) {
        UserAuthentication userAuthentication = new UserAuthentication(userId, roles);
        SecurityContextHolder.getContext().setAuthentication(userAuthentication);
    }
}
