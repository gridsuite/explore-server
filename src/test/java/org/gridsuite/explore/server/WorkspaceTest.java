/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceService workspaceService;

    @MockitoBean
    private DirectoryService directoryService;

    @Captor
    private ArgumentCaptor<ElementAttributes> elementAttributesCaptor;

    private WireMockServer wireMockServer;

    private static final String BASE_URL = "/v1/explore/workspaces";
    private static final String STUDY_CONFIG_SERVER_BASE_URL = "/v1/workspaces";
    private static final UUID WORKSPACE_UUID = UUID.randomUUID();
    private static final UUID SOURCE_WORKSPACE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String WORKSPACE_NAME = "Test Workspace";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        workspaceService.setStudyConfigServerBaseUri(wireMockServer.baseUrl());

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(STUDY_CONFIG_SERVER_BASE_URL))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(WORKSPACE_UUID))));

        wireMockServer.stubFor(WireMock.put(WireMock.urlPathMatching(STUDY_CONFIG_SERVER_BASE_URL + "/.*/replace"))
                .willReturn(WireMock.noContent()));

        wireMockServer.stubFor(WireMock.delete(WireMock.urlPathMatching(STUDY_CONFIG_SERVER_BASE_URL + "/.*"))
                .willReturn(WireMock.noContent()));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testCreateWorkspace() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("workspaceId", SOURCE_WORKSPACE_UUID.toString())
                        .param("name", WORKSPACE_NAME)
                        .param("description", "Test workspace description")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isCreated());

        verify(directoryService, times(1)).createElement(elementAttributesCaptor.capture(), eq(PARENT_DIRECTORY_UUID), eq(USER_ID));
        verify(directoryService, times(1)).checkPermission(List.of(PARENT_DIRECTORY_UUID), null, USER_ID, PermissionType.WRITE);
        assertEquals(WORKSPACE_UUID, elementAttributesCaptor.getValue().getElementUuid());
    }

    @Test
    void testReplaceWorkspace() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", WORKSPACE_UUID)
                        .param("workspaceId", SOURCE_WORKSPACE_UUID.toString())
                        .param("name", WORKSPACE_NAME)
                        .param("description", "Updated description")
                        .header("userId", USER_ID))
                .andExpect(status().isNoContent());

        verify(directoryService, times(1)).updateElement(eq(WORKSPACE_UUID), elementAttributesCaptor.capture(), eq(USER_ID));
        verify(directoryService, times(1)).checkPermission(List.of(WORKSPACE_UUID), null, USER_ID, PermissionType.WRITE);
    }

    @Test
    void testDuplicateWorkspace() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", SOURCE_WORKSPACE_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isCreated());

        verify(directoryService, times(1)).duplicateElement(SOURCE_WORKSPACE_UUID, WORKSPACE_UUID, PARENT_DIRECTORY_UUID, USER_ID);
        verify(directoryService, times(1)).checkPermission(List.of(PARENT_DIRECTORY_UUID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(1)).checkPermission(List.of(SOURCE_WORKSPACE_UUID), null, USER_ID, PermissionType.READ);
    }

    @Test
    void testDuplicateWorkspaceInSameDirectory() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", SOURCE_WORKSPACE_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isCreated());

        verify(directoryService, times(1)).duplicateElement(SOURCE_WORKSPACE_UUID, WORKSPACE_UUID, null, USER_ID);
    }

    @Test
    void testDuplicateWorkspaceWithInvalidUUID() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", "invalid-uuid")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateWorkspaceServiceError() throws Exception {
        wireMockServer.resetAll();
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(STUDY_CONFIG_SERVER_BASE_URL))
                .willReturn(WireMock.serverError()));

        mockMvc.perform(post(BASE_URL)
                        .param("workspaceId", SOURCE_WORKSPACE_UUID.toString())
                        .param("name", WORKSPACE_NAME)
                        .param("description", "Test workspace description")
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testReplaceWorkspaceServiceError() throws Exception {
        wireMockServer.resetAll();
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathMatching(STUDY_CONFIG_SERVER_BASE_URL + "/.*/replace"))
                .willReturn(WireMock.serverError()));

        mockMvc.perform(put(BASE_URL + "/{id}", WORKSPACE_UUID)
                        .param("workspaceId", SOURCE_WORKSPACE_UUID.toString())
                        .param("name", WORKSPACE_NAME)
                        .param("description", "Updated description")
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDuplicateWorkspaceServiceError() throws Exception {
        wireMockServer.resetAll();
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(STUDY_CONFIG_SERVER_BASE_URL))
                .willReturn(WireMock.serverError()));

        mockMvc.perform(post(BASE_URL)
                        .param("duplicateFrom", SOURCE_WORKSPACE_UUID.toString())
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isInternalServerError());
    }
}
