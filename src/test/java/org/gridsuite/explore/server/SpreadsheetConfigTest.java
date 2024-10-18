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
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Achour BERRAHMA <achour.berrahma at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpreadsheetConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpreadsheetConfigService spreadsheetConfigService;

    @Autowired
    private DirectoryService directoryService;

    private static MockWebServer mockWebServer;

    private static final String BASE_URL = "/v1/explore/spreadsheet-configs";
    private static final String SPREADSHEET_CONFIG_SERVER_BASE_URL = "/v1/spreadsheet-configs";
    private static final UUID CONFIG_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String CONFIG_NAME = "Test Config";

    private String spreadsheetConfigJson;

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
        spreadsheetConfigService.setSpreadsheetConfigServerBaseUri(baseUrl);
        directoryService.setDirectoryServerBaseUri(baseUrl);

        spreadsheetConfigJson = "{\"sheetType\":\"GENERATORS\",\"customColumns\":[{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Custom Column\",\"formula\":\"SUM(A1:A10)\"}]}";

        mockWebServer.setDispatcher(new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.matches(SPREADSHEET_CONFIG_SERVER_BASE_URL) && "POST".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(201)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(CONFIG_UUID));
                } else if (path.matches(SPREADSHEET_CONFIG_SERVER_BASE_URL + "/" + CONFIG_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(204);
                } else if (path.matches(SPREADSHEET_CONFIG_SERVER_BASE_URL + "/duplicate\\?duplicateFrom=" + CONFIG_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(201)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(UUID.randomUUID()));
                } else if (path.matches(SPREADSHEET_CONFIG_SERVER_BASE_URL + "/metadata\\?ids=" + CONFIG_UUID)) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("id", CONFIG_UUID);
                    metadata.put("sheetType", "GENERATORS");

                    List<Map<String, Object>> responseList = Collections.singletonList(metadata);

                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(responseList));
                } else if (path.matches("/v1/elements\\?ids=.*")) {
                    ElementAttributes elementAttributes = new ElementAttributes(
                            CONFIG_UUID,
                            CONFIG_NAME,
                            "SPREADSHEET_CONFIG",
                            USER_ID,
                            0L,
                            null,
                            null  // We'll set specificMetadata to null here as it's handled separately
                    );

                    List<ElementAttributes> elementAttributesList = Collections.singletonList(elementAttributes);

                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(elementAttributesList));
                } else if (path.matches("/v1/directories/.*/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    ElementAttributes elementAttributes = new ElementAttributes(CONFIG_UUID, CONFIG_NAME, "SPREADSHEET_CONFIG", USER_ID, 0L, null);
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(elementAttributes));
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                    return new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + CONFIG_UUID) && "PUT".equals(request.getMethod())) {
                    // Mock response for updating element in directory
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/elements\\?duplicateFrom=.*&newElementUuid=.*")) {
                    ElementAttributes duplicatedElement = new ElementAttributes(UUID.randomUUID(), CONFIG_NAME + " (copy)", "SPREADSHEET_CONFIG", USER_ID, 0L, null);
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(objectMapper.writeValueAsString(duplicatedElement));
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @Test
    void testCreateSpreadsheetConfig() throws Exception {
        ResultActions perform = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(spreadsheetConfigJson)
                .param("name", CONFIG_NAME)
                .param("description", "comment")
                .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                .header("userId", USER_ID));
        perform.andExpect(status().isCreated());
    }

    @Test
    void testUpdateSpreadsheetConfig() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", CONFIG_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigJson)
                        .param("name", CONFIG_NAME)
                        .header("userId", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDuplicateSpreadsheetConfig() throws Exception {
        mockMvc.perform(post(BASE_URL + "/duplicate")
                        .param("duplicateFrom", CONFIG_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isCreated());
    }

    @Test
    void testDuplicateSpreadsheetConfigWithInvalidUUID() throws Exception {
        mockMvc.perform(post(BASE_URL + "/duplicate")
                        .param("duplicateFrom", "invalid-uuid")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateSpreadsheetConfigServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(500);
            }
        });

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigJson)
                        .param("name", CONFIG_NAME)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateSpreadsheetConfigServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(500);
            }
        });

        mockMvc.perform(put(BASE_URL + "/{id}", CONFIG_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spreadsheetConfigJson)
                        .param("name", CONFIG_NAME)
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDuplicateSpreadsheetConfigServiceError() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(500);
            }
        });

        mockMvc.perform(post(BASE_URL + "/duplicate")
                        .param("duplicateFrom", CONFIG_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetSpreadsheetConfigMetadata() throws Exception {
        mockMvc.perform(get("/v1/explore/elements/metadata")
                        .param("ids", CONFIG_UUID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].specificMetadata.id").value(CONFIG_UUID.toString()))
                .andExpect(jsonPath("$[0].specificMetadata.sheetType").value("GENERATORS"));
    }
}
