/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.DynamicMappingService;
import org.gridsuite.explore.server.utils.WireMockUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
class DynamicMappingTest {

    @Autowired
    private MockMvc mockMvc;

    private WireMockServer wireMockServer;

    protected WireMockUtils wireMockUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<ElementAttributes> elementAttributesCaptor;

    @Autowired
    private DynamicMappingService dynamicMappingService;

    @MockitoBean
    private DirectoryService directoryService;

    private static final String URL_EXPLORE_DYNAMIC_MAPPINGS = "/v1/explore/dynamic-mappings";
    private static final String URL_MAPPINGS = "/mappings";

    private static final String QUERY_PARAM_NAME = "name";
    private static final String QUERY_PARAM_DESCRIPTION = "description";
    private static final String QUERY_PARAM_PARENT_DIRECTORY_ID = "parentDirectoryUuid";
    private static final String QUERY_PARAM_USER_ID = "userId";

    private static final UUID ID = UUID.randomUUID();
    private static final UUID NEW_ID = UUID.randomUUID();
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final UUID DIRECTORY_ID = UUID.randomUUID();
    private static final String USER_ID = "userId";
    private static final String DYNAMIC_MAPPING = "dynamicMapping";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockUtils = new WireMockUtils(wireMockServer);
        wireMockServer.start();
        dynamicMappingService.setDynamicMappingServerBaseUri(wireMockServer.baseUrl());
    }

    @AfterEach
    void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void createDynamicMapping() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(urlPathEqualTo(URL_MAPPINGS))
            .willReturn(WireMock.ok()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(ID))))
            .getId();

        mockMvc.perform(post(URL_EXPLORE_DYNAMIC_MAPPINGS)
                .queryParam(QUERY_PARAM_NAME, NAME)
                .queryParam(QUERY_PARAM_DESCRIPTION, DESCRIPTION)
                .queryParam(QUERY_PARAM_PARENT_DIRECTORY_ID, DIRECTORY_ID.toString())
                .header(QUERY_PARAM_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(DYNAMIC_MAPPING))
            .andExpect(status().isOk());

        verify(directoryService, times(1)).checkPermission(List.of(DIRECTORY_ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(1)).createElementWithNewName(elementAttributesCaptor.capture(), eq(DIRECTORY_ID), eq(USER_ID), eq(true));
        assertEquals(ID, elementAttributesCaptor.getValue().getElementUuid());
        wireMockUtils.verifyPostRequest(stubId, URL_MAPPINGS, Map.of(), false);
    }

    @Test
    void createDynamicMappingServerError() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(urlPathEqualTo(URL_MAPPINGS))
                .willReturn(WireMock.serverError()))
            .getId();

        mockMvc.perform(post(URL_EXPLORE_DYNAMIC_MAPPINGS)
                .queryParam(QUERY_PARAM_NAME, NAME)
                .queryParam(QUERY_PARAM_DESCRIPTION, DESCRIPTION)
                .queryParam(QUERY_PARAM_PARENT_DIRECTORY_ID, DIRECTORY_ID.toString())
                .header(QUERY_PARAM_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(DYNAMIC_MAPPING))
            .andExpect(status().isInternalServerError());

        verify(directoryService, times(1)).checkPermission(List.of(DIRECTORY_ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(0)).createElementWithNewName(any(ElementAttributes.class), any(UUID.class), any(String.class), any(boolean.class));
        wireMockUtils.verifyPostRequest(stubId, URL_MAPPINGS, Map.of(), false);
    }

    @Test
    void updateDynamicMapping() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.put(urlPathEqualTo(URL_MAPPINGS + "/" + ID))
            .willReturn(WireMock.ok()))
            .getId();

        mockMvc.perform(put(URL_EXPLORE_DYNAMIC_MAPPINGS + "/" + ID)
                .queryParam(QUERY_PARAM_NAME, NAME)
                .queryParam(QUERY_PARAM_DESCRIPTION, DESCRIPTION)
                .header(QUERY_PARAM_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(DYNAMIC_MAPPING))
            .andExpect(status().isOk());

        verify(directoryService, times(1)).checkPermission(List.of(ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(1)).updateElement(eq(ID), elementAttributesCaptor.capture(), eq(USER_ID));
        wireMockUtils.verifyPutRequest(stubId, URL_MAPPINGS + "/" + ID, Map.of(), false);
    }

    @Test
    void updateDynamicMappingServerError() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.put(urlPathEqualTo(URL_MAPPINGS + "/" + ID))
                .willReturn(WireMock.serverError()))
            .getId();

        mockMvc.perform(put(URL_EXPLORE_DYNAMIC_MAPPINGS + "/" + ID)
                .queryParam(QUERY_PARAM_NAME, NAME)
                .queryParam(QUERY_PARAM_DESCRIPTION, DESCRIPTION)
                .header(QUERY_PARAM_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(DYNAMIC_MAPPING))
            .andExpect(status().isInternalServerError());

        verify(directoryService, times(1)).checkPermission(List.of(ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(0)).updateElement(any(UUID.class), any(ElementAttributes.class), any(String.class));
        wireMockUtils.verifyPutRequest(stubId, URL_MAPPINGS + "/" + ID, Map.of(), false);
    }

    @Test
    void duplicateDynamicMapping() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(urlPathEqualTo(URL_MAPPINGS + "/" + ID + "/duplicate"))
                .willReturn(WireMock.ok()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(NEW_ID))))
            .getId();

        mockMvc.perform(post(URL_EXPLORE_DYNAMIC_MAPPINGS)
                .queryParam("duplicateFrom", ID.toString())
                .queryParam(QUERY_PARAM_PARENT_DIRECTORY_ID, DIRECTORY_ID.toString())
                .header(QUERY_PARAM_USER_ID, USER_ID))
            .andExpect(status().isOk());

        verify(directoryService, times(1)).checkPermission(List.of(ID), null, USER_ID, PermissionType.READ);
        verify(directoryService, times(1)).checkPermission(List.of(DIRECTORY_ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(1)).duplicateElement(ID, NEW_ID, DIRECTORY_ID, USER_ID);
        wireMockUtils.verifyPostRequest(stubId, URL_MAPPINGS + "/" + ID + "/duplicate", Map.of(), false);
    }

    @Test
    void duplicateDynamicMappingServerError() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(urlPathEqualTo(URL_MAPPINGS + "/" + ID + "/duplicate"))
                .willReturn(WireMock.serverError()))
            .getId();

        mockMvc.perform(post(URL_EXPLORE_DYNAMIC_MAPPINGS)
                .queryParam("duplicateFrom", ID.toString())
                .queryParam(QUERY_PARAM_PARENT_DIRECTORY_ID, DIRECTORY_ID.toString())
                .header(QUERY_PARAM_USER_ID, USER_ID))
            .andExpect(status().isInternalServerError());

        verify(directoryService, times(1)).checkPermission(List.of(ID), null, USER_ID, PermissionType.READ);
        verify(directoryService, times(1)).checkPermission(List.of(DIRECTORY_ID), null, USER_ID, PermissionType.WRITE);
        verify(directoryService, times(0)).duplicateElement(any(UUID.class), any(UUID.class), any(UUID.class), any(String.class));
        wireMockUtils.verifyPostRequest(stubId, URL_MAPPINGS + "/" + ID + "/duplicate", Map.of(), false);
    }
}
