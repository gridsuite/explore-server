/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.ws.commons.security.AllowAllAuthTestConfiguration;
import lombok.SneakyThrows;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import okhttp3.Headers;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.SpreadsheetConfigCollectionService;
import org.gridsuite.explore.server.services.UserAdminService;
import org.gridsuite.explore.server.utils.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@SpringBootTest(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@AutoConfigureMockMvc
@Import(AllowAllAuthTestConfiguration.class)
class SpreadsheetConfigCollectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private UserAdminService userAdminService;

    @StartStop
    final MockWebServer mockWebServer = new MockWebServer();

    private static final String BASE_URL = "/v1/explore/spreadsheet-config-collections";
    private static final String SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL = "/v1/spreadsheet-config-collections";
    private static final UUID COLLECTION_UUID = UUID.randomUUID();
    private static final UUID DUPLICATED_COLLECTION_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String COLLECTION_NAME = "Test Collection";

    private String spreadsheetConfigCollectionJson;

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        spreadsheetConfigCollectionService.setSpreadsheetConfigServerBaseUri(baseUrl);
        directoryService.setDirectoryServerBaseUri(baseUrl);
        userAdminService.setUserAdminServerBaseUri(baseUrl);

        spreadsheetConfigCollectionJson = "{\"name\":\"" + COLLECTION_NAME + "\",\"description\":\"Test Description\",\"spreadsheetConfigs\":[{\"id\":\"" + UUID.randomUUID() +
                "\",\"name\":\"Config 1\"},{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Config 2\"}]}";

        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getTarget();
                if ((path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL) || path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "/merge")) && "POST".equals(request.getMethod())) {
                    return new MockResponse.Builder().code(201).headers(Headers.of("Content-Type", "application/json")).body(objectMapper.writeValueAsString(COLLECTION_UUID)).build();
                } else if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "/" + COLLECTION_UUID)) {
                    return new MockResponse.Builder().code(204).build();
                } else if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "/" + COLLECTION_UUID + "/spreadsheet-configs/replace-all")) {
                    return new MockResponse.Builder().code(204).build();
                } else if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "/" + COLLECTION_UUID + "/duplicate") && "POST".equals(request.getMethod())) {
                    return new MockResponse.Builder().code(201).headers(Headers.of("Content-Type", "application/json")).body(objectMapper.writeValueAsString(DUPLICATED_COLLECTION_UUID)).build();
                } else if (path.matches("/v1/directories/.*/elements\\?allowNewName=.*") && "POST".equals(request.getMethod()) || path.matches("/v1/elements/" + COLLECTION_UUID)) {
                    ElementAttributes elementAttributes = new ElementAttributes(COLLECTION_UUID, COLLECTION_NAME, "SPREADSHEET_CONFIG_COLLECTION", USER_ID, 0L, null);
                    return new MockResponse.Builder().code(200).headers(Headers.of("Content-Type", "application/json")).body(objectMapper.writeValueAsString(elementAttributes)).build();
                } else if (path.matches("/v1/elements/.*/duplicate\\?newElementUuid=.*")) {
                    ElementAttributes duplicatedElement = new ElementAttributes(DUPLICATED_COLLECTION_UUID, COLLECTION_NAME + " (copy)", "SPREADSHEET_CONFIG_COLLECTION", USER_ID, 0L, null);
                    return new MockResponse.Builder().code(200).headers(Headers.of("Content-Type", "application/json")).body(objectMapper.writeValueAsString(duplicatedElement)).build();
                } else if (path.matches("/v1/elements\\?forDeletion=true&ids=.*")) {
                    return new MockResponse.Builder().code(200).build();
                } else if (path.matches("/v1/elements\\?f=true&ids=.*")) {
                    return new MockResponse.Builder().code(200).build();
                } else if (path.matches("/v1/elements\\?ids=.*&status=.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse.Builder().code(200).build();
                }
                return new MockResponse.Builder().code(404).build();
            }
        });
    }

    @Test
    void testCreateSpreadsheetConfigCollection() throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(spreadsheetConfigCollectionJson)
                .param("name", COLLECTION_NAME)
                .param("description", "Test Description")
                .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(COLLECTION_UUID, objectMapper.readValue(response, UUID.class));
    }

    @Test
    void testCreateSpreadsheetConfigCollectionWithMerge() throws Exception {
        String configIds = "[\"" + UUID.randomUUID() + "\", \"" + UUID.randomUUID() + "\"]";
        String response = mockMvc.perform(post(BASE_URL + "/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(configIds)
                .param("name", COLLECTION_NAME)
                .param("description", "Test Description")
                .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(COLLECTION_UUID, objectMapper.readValue(response, UUID.class));
    }

    @Test
    void testReplaceAllSpreadsheetConfigsInCollection() throws Exception {
        String configIds = "[\"" + UUID.randomUUID() + "\", \"" + UUID.randomUUID() + "\"]";
        ResultActions perform = mockMvc.perform(put(BASE_URL + "/{id}/spreadsheet-configs/replace-all", COLLECTION_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(configIds)
                .param("name", COLLECTION_NAME)
                .param("description", "Test Description"));
        perform.andExpect(status().isNoContent());
    }

    @Test
    void testUpdateSpreadsheetConfigCollection() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", COLLECTION_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME)
                        .param("description", "Test Description"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDuplicateSpreadsheetConfigCollection() throws Exception {
        String response = mockMvc.perform(post(BASE_URL + "/" + COLLECTION_UUID + "/duplicate")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(DUPLICATED_COLLECTION_UUID, objectMapper.readValue(response, UUID.class));

        // check that we called 1 time spreadsheet-config to duplicate the spreadsheet config and 1 time directory to duplicate the element
        TestUtils.getRequestsWithBodyDone(2, mockWebServer);
    }

    @Test
    void testDuplicateSpreadsheetConfigCollectionInSameDirectory() throws Exception {
        String response = mockMvc.perform(post(BASE_URL + "/" + COLLECTION_UUID + "/duplicate"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(DUPLICATED_COLLECTION_UUID, objectMapper.readValue(response, UUID.class));

        // check that we called 1 time spreadsheet-config to duplicate the spreadsheet config and 1 time directory to duplicate the element
        TestUtils.getRequestsWithBodyDone(2, mockWebServer);
    }

    @Test
    void testDuplicateSpreadsheetConfigCollectionWithInvalidUUID() throws Exception {
        mockMvc.perform(post(BASE_URL + "/invalid-uuid/duplicate")
                .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateSpreadsheetConfigCollectionServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse.Builder().code(500).build();
            }
        });

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateSpreadsheetConfigCollectionServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse.Builder().code(500).build();
            }
        });

        mockMvc.perform(put(BASE_URL + "/{id}", COLLECTION_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicateSpreadsheetConfigCollectionServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse.Builder().code(500).build();
            }
        });

        mockMvc.perform(post(BASE_URL + "/duplicate")
                        .param("duplicateFrom", COLLECTION_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testDeleteSpreadsheetConfigCollection() throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{id}", COLLECTION_UUID))
                .andExpect(status().isOk());
    }
}
