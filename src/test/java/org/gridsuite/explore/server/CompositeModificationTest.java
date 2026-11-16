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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.explore.server.services.ExploreService.MODIFICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that a composite modification created or duplicated is created directly with its final name
 * (ex: "name (1)") in the network modification server, as it is the one displayed : it is never renamed afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@Import(AllowAllAuthorizationTestConfiguration.class)
class CompositeModificationTest {

    private static final String BASE_URL = "/v1/explore/composite-modifications";
    private static final String NMS_COMPOSITES_URL = "/v1/network-composite-modifications";
    private static final String NMS_DUPLICATE_URL = NMS_COMPOSITES_URL + "/duplication";
    private static final UUID SOURCE_COMPOSITE_UUID = UUID.randomUUID();
    private static final UUID NEW_COMPOSITE_UUID = UUID.randomUUID();
    private static final UUID SOURCE_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID TARGET_DIRECTORY_UUID = UUID.randomUUID();
    private static final String OUTDATED_DIRECTORY_NAME = "outdated name";
    private static final String REAL_NAME = "composite";
    private static final String REAL_DESCRIPTION = "composite description";
    private static final String NEW_NAME = "composite (1)";
    private static final String USER_ID = "userId";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkModificationService networkModificationService;

    @MockitoBean
    private DirectoryService directoryService;

    @Captor
    private ArgumentCaptor<ElementAttributes> elementAttributesCaptor;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        networkModificationService.setNetworkModificationServerBaseUri(wireMockServer.baseUrl());

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(NMS_DUPLICATE_URL))
                .willReturn(jsonResponse(Map.of(SOURCE_COMPOSITE_UUID, NEW_COMPOSITE_UUID))));
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(NMS_COMPOSITES_URL))
                .willReturn(jsonResponse(NEW_COMPOSITE_UUID)));
        wireMockServer.stubFor(WireMock.delete(WireMock.urlPathEqualTo("/v1/network-modifications"))
                .willReturn(WireMock.ok()));
        // the real name and description of the source composite are the ones of the network modification server
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/network-modifications/metadata"))
                .withQueryParam("ids", WireMock.equalTo(SOURCE_COMPOSITE_UUID.toString()))
                .willReturn(jsonResponse(List.of(Map.of("id", SOURCE_COMPOSITE_UUID, "name", REAL_NAME, "description", REAL_DESCRIPTION)))));

        when(directoryService.getElementInfos(SOURCE_COMPOSITE_UUID))
                .thenReturn(new ElementAttributes(SOURCE_COMPOSITE_UUID, OUTDATED_DIRECTORY_NAME, MODIFICATION, USER_ID, 0, "outdated description"));
        when(directoryService.getParentDirectoryUuid(SOURCE_COMPOSITE_UUID)).thenReturn(SOURCE_DIRECTORY_UUID);
    }

    @AfterEach
    void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private String duplicationBody(String newName) throws JsonProcessingException {
        // the source composite is duplicated directly with its new name
        return objectMapper.writeValueAsString(Map.of(SOURCE_COMPOSITE_UUID, newName));
    }

    private ResponseDefinitionBuilder jsonResponse(Object body) throws JsonProcessingException {
        return WireMock.ok()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(body));
    }

    private void verifyNeverRenamed() {
        wireMockServer.verify(0, WireMock.putRequestedFor(WireMock.urlPathMatching("/v1/network-modifications/.*/name-and-description")));
    }

    private void verifyDirectoryElementCreated(UUID directoryUuid, String name, String description) {
        // the directory element must have exactly this name (no new name given by the directory server)
        verify(directoryService).createElement(elementAttributesCaptor.capture(), eq(directoryUuid));
        verify(directoryService, never()).createElementWithNewName(any(), any(), eq(true));
        ElementAttributes createdElement = elementAttributesCaptor.getValue();
        assertEquals(NEW_COMPOSITE_UUID, createdElement.getElementUuid());
        assertEquals(name, createdElement.getElementName());
        assertEquals(description, createdElement.getDescription());
        assertEquals(MODIFICATION, createdElement.getType());
    }

    @Test
    void testDuplicateCompositeModificationInSameDirectory() throws Exception {
        // the name candidate is computed from the real name of the composite, in the source directory
        when(directoryService.getNameCandidate(SOURCE_DIRECTORY_UUID, REAL_NAME, MODIFICATION)).thenReturn(NEW_NAME);

        mockMvc.perform(post(BASE_URL + "/{id}/duplicate", SOURCE_COMPOSITE_UUID))
                .andExpect(status().isOk());

        // the composite is duplicated directly with the name candidate
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo(NMS_DUPLICATE_URL))
                .withRequestBody(WireMock.equalToJson(duplicationBody(NEW_NAME))));
        verifyDirectoryElementCreated(SOURCE_DIRECTORY_UUID, NEW_NAME, REAL_DESCRIPTION);
        verify(directoryService, never()).duplicateElement(any(), any(), any(), any());
        verifyNeverRenamed();
    }

    @Test
    void testDuplicateCompositeModificationInOtherDirectoryWithoutConflict() throws Exception {
        when(directoryService.getNameCandidate(TARGET_DIRECTORY_UUID, REAL_NAME, MODIFICATION)).thenReturn(REAL_NAME);

        mockMvc.perform(post(BASE_URL + "/{id}/duplicate", SOURCE_COMPOSITE_UUID)
                        .param("parentDirectoryUuid", TARGET_DIRECTORY_UUID.toString()))
                .andExpect(status().isOk());

        verify(directoryService, never()).getParentDirectoryUuid(any());
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo(NMS_DUPLICATE_URL))
                .withRequestBody(WireMock.equalToJson(duplicationBody(REAL_NAME))));
        verifyDirectoryElementCreated(TARGET_DIRECTORY_UUID, REAL_NAME, REAL_DESCRIPTION);
        verifyNeverRenamed();
    }

    @Test
    void testDuplicateCompositeModificationDeletedWhenDirectoryCreationFails() throws Exception {
        when(directoryService.getNameCandidate(SOURCE_DIRECTORY_UUID, REAL_NAME, MODIFICATION)).thenReturn(NEW_NAME);
        // ex: an element named NEW_NAME was created in the meantime
        when(directoryService.createElement(any(), any())).thenThrow(new RuntimeException("directory error"));

        mockMvc.perform(post(BASE_URL + "/{id}/duplicate", SOURCE_COMPOSITE_UUID));

        // the duplicated composite is deleted, not renamed
        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlPathEqualTo("/v1/network-modifications"))
                .withQueryParam("uuids", WireMock.equalTo(NEW_COMPOSITE_UUID.toString())));
        verifyNeverRenamed();
    }

    @Test
    void testCreateCompositeModificationWithExistingName() throws Exception {
        when(directoryService.getNameCandidate(TARGET_DIRECTORY_UUID, REAL_NAME, MODIFICATION)).thenReturn(NEW_NAME);

        mockMvc.perform(post(BASE_URL)
                        .param("name", REAL_NAME)
                        .param("description", REAL_DESCRIPTION)
                        .param("parentDirectoryUuid", TARGET_DIRECTORY_UUID.toString())
                        .param("rootNetworkTags", "PH1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(UUID.randomUUID()))))
                .andExpect(status().isOk());

        // the composite is created directly with the name candidate
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo(NMS_COMPOSITES_URL))
                .withQueryParam("name", WireMock.equalTo(NEW_NAME)));
        verifyDirectoryElementCreated(TARGET_DIRECTORY_UUID, NEW_NAME, REAL_DESCRIPTION);
        verifyNeverRenamed();
    }
}
