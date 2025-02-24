/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.internal.MockWebServerExtension;
import okhttp3.Headers;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.SpreadsheetConfigCollectionService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@ExtendWith(MockWebServerExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class SpreadsheetConfigCollectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;

    @Autowired
    private DirectoryService directoryService;

    private static final String BASE_URL = "/v1/explore/spreadsheet-config-collections";
    private static final String SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL = "/v1/spreadsheet-config-collections";
    private static final UUID COLLECTION_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String COLLECTION_NAME = "Test Collection";

    private String spreadsheetConfigCollectionJson;

    @BeforeEach
    void initialize(final MockWebServer mockWebServer) {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        spreadsheetConfigCollectionService.setSpreadsheetConfigServerBaseUri(baseUrl);
        directoryService.setDirectoryServerBaseUri(baseUrl);

        spreadsheetConfigCollectionJson = "{\"name\":\"" + COLLECTION_NAME + "\",\"description\":\"Test Description\",\"spreadsheetConfigs\":[{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Config 1\"},{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Config 2\"}]}";

        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL) && "POST".equals(request.getMethod())) {
                    return new MockResponse(201, Headers.of("Content-Type", "application/json"), objectMapper.writeValueAsString(COLLECTION_UUID));
                } else if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "/" + COLLECTION_UUID)) {
                    return new MockResponse(204);
                } else if (path.matches(SPREADSHEET_CONFIG_COLLECTION_SERVER_BASE_URL + "\\?duplicateFrom=" + COLLECTION_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse(201, Headers.of("Content-Type", "application/json"), objectMapper.writeValueAsString(UUID.randomUUID()));
                } else if (path.matches("/v1/directories/.*/elements\\?allowNewName=.*") && "POST".equals(request.getMethod()) || path.matches("/v1/elements/" + COLLECTION_UUID)) {
                    ElementAttributes elementAttributes = new ElementAttributes(COLLECTION_UUID, COLLECTION_NAME, "SPREADSHEET_CONFIG_COLLECTION", USER_ID, 0L, null);
                    return new MockResponse(200, Headers.of("Content-Type", "application/json"), objectMapper.writeValueAsString(elementAttributes));
                } else if (path.matches("/v1/elements\\?duplicateFrom=.*&newElementUuid=.*")) {
                    ElementAttributes duplicatedElement = new ElementAttributes(UUID.randomUUID(), COLLECTION_NAME + " (copy)", "SPREADSHEET_CONFIG_COLLECTION", USER_ID, 0L, null);
                    return new MockResponse(200, Headers.of("Content-Type", "application/json"), objectMapper.writeValueAsString(duplicatedElement));
                } else if (path.matches("/v1/elements\\?forDeletion=true&ids=.*")) {
                    return new MockResponse(200);
                }
                return new MockResponse(404);
            }
        });
    }

    @Test
    void testCreateSpreadsheetConfigCollection() throws Exception {
        ResultActions perform = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(spreadsheetConfigCollectionJson)
                .param("name", COLLECTION_NAME)
                .param("description", "Test Description")
                .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                .header("userId", USER_ID));
        perform.andExpect(status().isCreated());
    }

    @Test
    void testUpdateSpreadsheetConfigCollection() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", COLLECTION_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME)
                        .header("userId", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDuplicateSpreadsheetConfigCollection() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", COLLECTION_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isCreated());
    }

    @Test
    void testDuplicateSpreadsheetConfigCollectionWithInvalidUUID() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", "invalid-uuid")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateSpreadsheetConfigCollectionServiceError(final MockWebServer mockWebServer) throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse(500);
            }
        });

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateSpreadsheetConfigCollectionServiceError(final MockWebServer mockWebServer) throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse(500);
            }
        });

        mockMvc.perform(put(BASE_URL + "/{id}", COLLECTION_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigCollectionJson)
                        .param("name", COLLECTION_NAME)
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDuplicateSpreadsheetConfigCollectionServiceError(final MockWebServer mockWebServer) throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse(500);
            }
        });

        mockMvc.perform(post(BASE_URL + "/duplicate")
                        .param("duplicateFrom", COLLECTION_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDeleteSpreadsheetConfigCollection() throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{id}", COLLECTION_UUID)
                        .header("userId", USER_ID))
                .andExpect(status().isOk());
    }
}
