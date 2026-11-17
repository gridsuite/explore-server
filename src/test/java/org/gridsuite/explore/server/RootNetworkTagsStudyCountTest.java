/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.ReferenceAttributes;
import org.gridsuite.explore.server.dto.ReferenceContainer;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.StudyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AllowAllAuthorizationTestConfiguration.class)
class RootNetworkTagsStudyCountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private StudyService studyService;

    private WireMockServer wireMockServer;

    private static final UUID SHARED_ELEMENT_UUID = UUID.randomUUID();
    private static final UUID NODE_1_UUID = UUID.randomUUID();
    private static final UUID NODE_2_UUID = UUID.randomUUID();
    private static final UUID STUDY_1_UUID = UUID.randomUUID();

    private static final String SHARED_ELEMENT_PATH = "/v1/elements/" + SHARED_ELEMENT_UUID;
    private static final String STUDY_COUNT_PATH = "/v1/nodes/root-network-tags/study-count";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        directoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        studyService.setStudyServerBaseUri(wireMockServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static ReferenceAttributes reference(ReferenceAttributes.ReferenceType type, UUID rootContainerId, UUID containerId) {
        return ReferenceAttributes.builder()
                .referenceId(UUID.randomUUID())
                .referenceType(type)
                .referenceContainer(ReferenceContainer.builder()
                        .rootContainerId(rootContainerId)
                        .containerId(containerId)
                        .build())
                .build();
    }

    private void stubSharedElementReferences(ReferenceAttributes... references) throws Exception {
        ElementAttributes sharedElement = new ElementAttributes(SHARED_ELEMENT_UUID, "sharedModification", "MODIFICATION", "owner", 0L, null);
        sharedElement.setReferences(List.of(references));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(SHARED_ELEMENT_PATH))
                .willReturn(WireMock.ok(objectMapper.writeValueAsString(sharedElement))
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    private Map<String, Long> getRootNetworkTagsStudyCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/{elementUuid}/root-network-tags/study-count", SHARED_ELEMENT_UUID))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
    }

    @Test
    void testCountFromReferencedNodes() throws Exception {
        stubSharedElementReferences(
                // node 1: at the root of its node, referenced twice
                reference(ReferenceAttributes.ReferenceType.STUDY_NODE, STUDY_1_UUID, NODE_1_UUID),
                reference(ReferenceAttributes.ReferenceType.STUDY_NODE, STUDY_1_UUID, NODE_1_UUID),
                // node 2: inside a composite of the node
                reference(ReferenceAttributes.ReferenceType.STUDY_NODE_NETWORK_MODIFICATION, NODE_2_UUID, UUID.randomUUID()),
                // not in a study: ignored
                reference(ReferenceAttributes.ReferenceType.DIRECTORY_NETWORK_MODIFICATION, UUID.randomUUID(), UUID.randomUUID()));
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(STUDY_COUNT_PATH))
                .willReturn(WireMock.ok("{\"R1\": 2, \"R2\": 1}")
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        assertEquals(Map.of("R1", 2L, "R2", 1L), getRootNetworkTagsStudyCount());

        // the study-server is called once, with each referenced node only once
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo(STUDY_COUNT_PATH))
                .withRequestBody(WireMock.equalToJson("[\"" + NODE_1_UUID + "\", \"" + NODE_2_UUID + "\"]")));
    }

    @Test
    void testElementWithoutStudyReference() throws Exception {
        stubSharedElementReferences(
                reference(ReferenceAttributes.ReferenceType.DIRECTORY_NETWORK_MODIFICATION, UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(Map.of(), getRootNetworkTagsStudyCount());

        // no node to count from: the study-server is not reached
        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo(STUDY_COUNT_PATH)));
    }
}
