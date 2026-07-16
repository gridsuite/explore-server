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
import org.gridsuite.explore.server.dto.NodeInfos;
import org.gridsuite.explore.server.dto.ReferenceAttributes;
import org.gridsuite.explore.server.dto.SharedElementInfos;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.StudyService;
import org.gridsuite.explore.server.services.UserIdentityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
@SpringBootTest
@AutoConfigureMockMvc
class SharedElementInfosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserIdentityService userIdentityService;

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private StudyService studyService;

    private WireMockServer wireMockServer;

    private static final String USER_ID = "userId";
    private static final String OWNER_SUB = "owner01";
    private static final String MODIFIER_SUB = "modifier01";
    private static final Instant LAST_MODIFICATION_DATE = Instant.parse("2026-07-16T10:15:30.00Z");

    private static final UUID SHARED_ELEMENT_UUID = UUID.randomUUID();
    private static final UUID NODE_1_UUID = UUID.randomUUID();
    private static final UUID NODE_2_UUID = UUID.randomUUID();
    private static final UUID STUDY_1_UUID = UUID.randomUUID();
    private static final UUID STUDY_2_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        userIdentityService.setUserIdentityServerBaseUri(wireMockServer.baseUrl());
        stubUsersIdentities();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private void stubUsersIdentities() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/users/identities"))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"data": {"owner01": {"firstName": "John", "lastName": "Doe"}}}
                                """)));
    }

    private void stubSharedElementReferences(UUID... referencedNodeUuids) {
        ElementAttributes sharedElement = new ElementAttributes(SHARED_ELEMENT_UUID, "sharedModification", "MODIFICATION", OWNER_SUB, 0L, null);
        sharedElement.setReferences(java.util.Arrays.stream(referencedNodeUuids)
                .map(nodeUuid -> new ReferenceAttributes(nodeUuid, ReferenceAttributes.ReferenceType.STUDY_NODE))
                .toList());
        when(directoryService.getElementInfos(SHARED_ELEMENT_UUID)).thenReturn(sharedElement);
    }

    private ElementAttributes study(UUID studyUuid, String name) {
        ElementAttributes study = new ElementAttributes(studyUuid, name, "STUDY", OWNER_SUB, 0L, null);
        study.setLastModifiedBy(MODIFIER_SUB);
        study.setLastModificationDate(LAST_MODIFICATION_DATE);
        return study;
    }

    private void stubStudyPath(UUID studyUuid, String studyName, String... parentDirectoryNames) {
        List<ElementAttributes> path = new java.util.ArrayList<>(java.util.Arrays.stream(parentDirectoryNames)
                .map(directoryName -> new ElementAttributes(UUID.randomUUID(), directoryName, "DIRECTORY", OWNER_SUB, 0L, null))
                .toList());
        // the directory-server returns the element itself as the last segment of its path
        path.add(study(studyUuid, studyName));
        when(directoryService.getElementPath(eq(studyUuid), any())).thenReturn(path);
    }

    private List<SharedElementInfos> getSharedElementInfos() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/{elementUuid}/shared-elements-infos", SHARED_ELEMENT_UUID)
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
    }

    @Test
    void testTwoNodesInDistinctStudies() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        when(studyService.getNodesInfos(any())).thenReturn(List.of(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_2_UUID)));
        when(directoryService.getElementsInfosNotStrict(any(), eq(null), eq(USER_ID)))
                .thenReturn(List.of(study(STUDY_1_UUID, "study1"), study(STUDY_2_UUID, "study2")));
        stubStudyPath(STUDY_1_UUID, "study1", "root", "folder");
        stubStudyPath(STUDY_2_UUID, "study2", "root");

        List<SharedElementInfos> infos = getSharedElementInfos();

        assertEquals(2, infos.size());
        SharedElementInfos first = infos.get(0);
        assertEquals("node1", first.node());
        assertEquals("study1", first.elementName());
        assertEquals("STUDY", first.type());
        // the study itself is excluded from its path, it already has its own column
        assertEquals(List.of("root", "folder"), first.pathName());
        assertEquals(LAST_MODIFICATION_DATE, first.lastModificationDate());
        assertEquals("John Doe", first.ownerLabel());
        // unknown identities fall back to the sub itself
        assertEquals(MODIFIER_SUB, first.lastModifiedByLabel());
        assertEquals("node2", infos.get(1).node());
        assertEquals(List.of("root"), infos.get(1).pathName());

        // the studies are fetched in a single call
        verify(directoryService, times(1)).getElementsInfosNotStrict(any(), eq(null), eq(USER_ID));
    }

    @Test
    void testTwoNodesInSameStudy() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        when(studyService.getNodesInfos(any())).thenReturn(List.of(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_1_UUID)));
        when(directoryService.getElementsInfosNotStrict(any(), eq(null), eq(USER_ID)))
                .thenReturn(List.of(study(STUDY_1_UUID, "study1")));
        stubStudyPath(STUDY_1_UUID, "study1", "root");

        List<SharedElementInfos> infos = getSharedElementInfos();

        // one line per reference, the study is repeated
        assertEquals(2, infos.size());
        assertEquals("study1", infos.get(0).elementName());
        assertEquals("study1", infos.get(1).elementName());
        assertEquals("node1", infos.get(0).node());
        assertEquals("node2", infos.get(1).node());
        // the study path is resolved only once
        verify(directoryService, times(1)).getElementPath(eq(STUDY_1_UUID), any());
    }

    @Test
    void testSameNodeReferencedTwice() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_1_UUID);
        when(studyService.getNodesInfos(List.of(NODE_1_UUID)))
                .thenReturn(List.of(new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID)));
        when(directoryService.getElementsInfosNotStrict(any(), eq(null), eq(USER_ID)))
                .thenReturn(List.of(study(STUDY_1_UUID, "study1")));
        stubStudyPath(STUDY_1_UUID, "study1", "root");

        List<SharedElementInfos> infos = getSharedElementInfos();

        // one line per reference, even when they point to the same node
        assertEquals(2, infos.size());
        assertEquals(infos.get(0), infos.get(1));
    }

    @Test
    void testStudyNotReadableIsOmitted() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        when(studyService.getNodesInfos(any())).thenReturn(List.of(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_2_UUID)));
        // the directory-server filters out the studies the user cannot read
        when(directoryService.getElementsInfosNotStrict(any(), eq(null), eq(USER_ID)))
                .thenReturn(List.of(study(STUDY_1_UUID, "study1")));
        stubStudyPath(STUDY_1_UUID, "study1", "root");

        List<SharedElementInfos> infos = getSharedElementInfos();

        assertEquals(1, infos.size());
        assertEquals("study1", infos.get(0).elementName());
    }

    @Test
    void testElementWithoutReferences() throws Exception {
        stubSharedElementReferences();

        assertTrue(getSharedElementInfos().isEmpty());
        // no need to reach the other servers
        verify(studyService, times(0)).getNodesInfos(any());
    }
}
