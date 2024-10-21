/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Sylvain BOUZOLS <sylvain.bouzols at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserIdentityService userIdentityService;

    @Autowired
    private DirectoryService directoryService;

    private static MockWebServer mockWebServer;

    private static final String BASE_URL = "/v1/explore/elements/users-identities";
    private static final String USER_IDENTITY_SERVER_BASE_URL = "/v1/users";
    private static final String DIRECTORY_SERVER_BASE_URL = "/v1/elements";
    private static final String SUB = "user01";
    private static final String UNKNOWN_SUB = "user02";

    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final String ELEMENT_NAME = "Test Element";

    private static final UUID NON_EXISTING_ELEMENT_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UNKNOWN_SUB_UUID = UUID.randomUUID();
    private static final String ELEMENT_UNKNOWN_SUB_NAME = "Test Element Unknown sub";

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        userIdentityService.setUserIdentityServerBaseUri(baseUrl);
        directoryService.setDirectoryServerBaseUri(baseUrl);

        mockWebServer.setDispatcher(new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.matches(USER_IDENTITY_SERVER_BASE_URL + "/identities\\?subs=" + SUB) && "GET".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{sub: " + SUB + ", firstName: \"userFirstName\", lastName: \"userLastName\"}");
                } else if (path.matches(USER_IDENTITY_SERVER_BASE_URL + "/identities\\?subs=" + UNKNOWN_SUB) && "GET".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(404);
                } else if (path.matches(DIRECTORY_SERVER_BASE_URL + "\\?ids=" + NON_EXISTING_ELEMENT_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse()
                        .setResponseCode(404);
                } else if (path.matches(DIRECTORY_SERVER_BASE_URL + "\\?ids=" + ELEMENT_UNKNOWN_SUB_UUID) && "GET".equals(request.getMethod())) {
                    ElementAttributes elementAttributes = new ElementAttributes(
                            ELEMENT_UNKNOWN_SUB_UUID,
                            ELEMENT_UNKNOWN_SUB_NAME,
                            "SOME TYPE",
                            UNKNOWN_SUB,
                            0L,
                            null
                    );

                    List<ElementAttributes> elementAttributesList = Collections.singletonList(elementAttributes);

                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(elementAttributesList));
                } else if (path.matches(DIRECTORY_SERVER_BASE_URL + "\\?ids=.*") && "GET".equals(request.getMethod())) {
                    ElementAttributes elementAttributes = new ElementAttributes(
                            ELEMENT_UUID,
                            ELEMENT_NAME,
                            "SOME TYPE",
                            SUB,
                            0L,
                            null
                    );

                    List<ElementAttributes> elementAttributesList = Collections.singletonList(elementAttributes);

                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(elementAttributesList));
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @Test
    void testGetSubIdentity() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("ids", ELEMENT_UUID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get(BASE_URL)
                .param("ids", NON_EXISTING_ELEMENT_UUID.toString()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get(BASE_URL)
                .param("ids", ELEMENT_UNKNOWN_SUB_UUID.toString()))
            .andExpect(status().isNotFound());
    }
}
