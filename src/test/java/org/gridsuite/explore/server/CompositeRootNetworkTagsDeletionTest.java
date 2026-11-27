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
import com.powsybl.ws.commons.security.AllowAllAuthTestConfiguration;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.NodeInfos;
import org.gridsuite.explore.server.dto.ReferenceAttributes;
import org.gridsuite.explore.server.dto.ReferenceContainer;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.gridsuite.explore.server.services.StudyService;
import org.gridsuite.explore.server.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.explore.server.ExploreConstants.HEADER_USER_ID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting root network tags of a composite modification, allowed only for the tags no study uses it with anymore
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@Import(AllowAllAuthTestConfiguration.class)
class CompositeRootNetworkTagsDeletionTest {

    private static final UUID COMPOSITE_UUID = UUID.randomUUID();
    private static final UUID NODE_UUID = UUID.randomUUID();
    private static final UUID STUDY_UUID = UUID.randomUUID();
    private static final String USER_ID = "userId";

    private static final String COMPOSITE_ELEMENT_PATH = "/v1/elements/" + COMPOSITE_UUID;
    private static final String NODES_INFOS_PATH = "/v1/nodes/infos";
    private static final String ROOT_NETWORK_TAGS_PATH = "/v1/studies/root-network-tags";
    private static final String ROOT_NETWORK_TAG_DELETION_PATH = "/v1/network-modifications/root-network-tag";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private NetworkModificationService networkModificationService;

    @Autowired
    private OutputDestination output;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        directoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        studyService.setStudyServerBaseUri(wireMockServer.baseUrl());
        networkModificationService.setNetworkModificationServerBaseUri(wireMockServer.baseUrl());
        wireMockServer.stubFor(WireMock.delete(WireMock.urlPathEqualTo(ROOT_NETWORK_TAG_DELETION_PATH)).willReturn(WireMock.ok()));
        output.clear();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private void stubCompositeReferences(ReferenceAttributes... references) throws Exception {
        ElementAttributes composite = new ElementAttributes(COMPOSITE_UUID, "composite", "MODIFICATION", "owner", 0L, null);
        composite.setReferences(List.of(references));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(COMPOSITE_ELEMENT_PATH))
                .willReturn(WireMock.ok(objectMapper.writeValueAsString(composite))
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    void testDeleteRootNetworkTagsNoStudyUses() throws Exception {
        stubCompositeReferences();

        mockMvc.perform(delete("/v1/explore/composite-modifications/{id}/root-network-tags", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("rootNetworkTags", "R1", "R2"))
                .andExpect(status().isOk());

        wireMockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlPathEqualTo(ROOT_NETWORK_TAG_DELETION_PATH))
                .withQueryParam("uuids", WireMock.equalTo(COMPOSITE_UUID.toString()))
                .withQueryParam("groupUuids", WireMock.absent())
                .withQueryParam("rootNetworkTags", WireMock.havingExactly("R1", "R2")));
        TestUtils.assertElementUpdatedSent(output, COMPOSITE_UUID, USER_ID);
    }

    @Test
    void testDeleteRootNetworkTagsStillUsedIsRefused() throws Exception {
        stubCompositeReferences(ReferenceAttributes.builder()
                .referenceId(UUID.randomUUID())
                .referenceType(ReferenceAttributes.ReferenceType.STUDY_NODE)
                .referenceContainer(ReferenceContainer.builder().rootContainerId(STUDY_UUID).containerId(NODE_UUID).build())
                .build());
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(NODES_INFOS_PATH))
                .willReturn(WireMock.ok(objectMapper.writeValueAsString(List.of(new NodeInfos(NODE_UUID, "node", STUDY_UUID))))
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(ROOT_NETWORK_TAGS_PATH))
                .willReturn(WireMock.ok(objectMapper.writeValueAsString(Map.of(STUDY_UUID, List.of("R1"))))
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        // R2 is no longer used, but R1 still is: nothing is deleted
        mockMvc.perform(delete("/v1/explore/composite-modifications/{id}/root-network-tags", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("rootNetworkTags", "R1", "R2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.businessErrorCode").value("explore.rootNetworkTagsInUse"))
                .andExpect(jsonPath("$.businessErrorValues.tags").value("R1"));

        wireMockServer.verify(0, WireMock.deleteRequestedFor(WireMock.urlPathEqualTo(ROOT_NETWORK_TAG_DELETION_PATH)));
        assertNull(output.receive(100, "element.update"));
    }
}
