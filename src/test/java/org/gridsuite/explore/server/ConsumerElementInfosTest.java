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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ConsumerElementInfos;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.NodeInfos;
import org.gridsuite.explore.server.dto.ReferenceAttributes;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConsumerElementInfosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserIdentityService userIdentityService;

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

    private static final String SHARED_ELEMENT_PATH = "/v1/elements/" + SHARED_ELEMENT_UUID;
    private static final String ELEMENTS_PATH = "/v1/elements";
    private static final String ELEMENTS_PATHS_PATH = "/v1/elements/paths";
    private static final String NODES_INFOS_PATH = "/v1/nodes/infos";
    private static final String USERS_IDENTITIES_PATH = "/v1/users/identities";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        directoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        studyService.setStudyServerBaseUri(wireMockServer.baseUrl());
        userIdentityService.setUserIdentityServerBaseUri(wireMockServer.baseUrl());
        stubUsersIdentities();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private ResponseDefinitionBuilder jsonResponse(Object body) throws Exception {
        return WireMock.ok(objectMapper.writeValueAsString(body))
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

    private void stubUsersIdentities() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(USERS_IDENTITIES_PATH))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"data": {"owner01": {"firstName": "John", "lastName": "Doe"}}}
                                """)));
    }

    private void stubSharedElementReferences(UUID... referencedNodeUuids) throws Exception {
        ElementAttributes sharedElement = new ElementAttributes(SHARED_ELEMENT_UUID, "sharedModification", "MODIFICATION", OWNER_SUB, 0L, null);
        sharedElement.setReferences(Arrays.stream(referencedNodeUuids)
                .map(nodeUuid -> new ReferenceAttributes(nodeUuid, ReferenceAttributes.ReferenceType.STUDY_NODE))
                .toList());
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(SHARED_ELEMENT_PATH))
                .willReturn(jsonResponse(sharedElement)));
    }

    private void stubNodesInfos(NodeInfos... nodesInfos) throws Exception {
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(NODES_INFOS_PATH))
                .willReturn(jsonResponse(List.of(nodesInfos))));
    }

    private void stubStudies(ElementAttributes... studies) throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(ELEMENTS_PATH))
                .willReturn(jsonResponse(List.of(studies))));
    }

    private void stubStudiesPaths(Map<UUID, List<ElementAttributes>> pathByStudyUuid) throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(ELEMENTS_PATHS_PATH))
                .willReturn(jsonResponse(pathByStudyUuid)));
    }

    private ElementAttributes studyStub(UUID studyUuid, String name) {
        ElementAttributes study = new ElementAttributes(studyUuid, name, "STUDY", OWNER_SUB, 0L, null);
        study.setLastModifiedBy(MODIFIER_SUB);
        study.setLastModificationDate(LAST_MODIFICATION_DATE);
        return study;
    }

    private List<ElementAttributes> pathStub(UUID studyUuid, String studyName, String... parentDirectoryNames) {
        List<ElementAttributes> path = new ArrayList<>(Arrays.stream(parentDirectoryNames)
                .map(directoryName -> new ElementAttributes(UUID.randomUUID(), directoryName, "DIRECTORY", OWNER_SUB, 0L, null))
                .toList());
        // the directory-server returns the element itself as the last segment of its path
        path.add(studyStub(studyUuid, studyName));
        return path;
    }

    private List<ConsumerElementInfos> getConsumerElementInfos() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/{elementUuid}/consumer-element-infos", SHARED_ELEMENT_UUID)
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
    }

    @Test
    void testTwoNodesInDistinctStudies() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        stubNodesInfos(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_2_UUID));
        stubStudies(studyStub(STUDY_1_UUID, "study1"), studyStub(STUDY_2_UUID, "study2"));
        stubStudiesPaths(Map.of(
                STUDY_1_UUID, pathStub(STUDY_1_UUID, "study1", "root", "folder"),
                STUDY_2_UUID, pathStub(STUDY_2_UUID, "study2", "root")));

        List<ConsumerElementInfos> infos = getConsumerElementInfos();

        assertEquals(2, infos.size());

        ConsumerElementInfos first = infos.get(0);
        assertEquals("node1", first.node());
        assertEquals("study1", first.elementName());
        assertEquals("STUDY", first.type());
        // the study itself is excluded from its path
        assertEquals(List.of("root", "folder"), first.path());
        assertEquals("John Doe", first.ownerLabel());
        assertEquals(LAST_MODIFICATION_DATE, first.lastModificationDate());
        // unknown identities fall back to the sub itself
        assertEquals(MODIFIER_SUB, first.lastModifiedByLabel());

        ConsumerElementInfos second = infos.get(1);
        assertEquals("node2", second.node());
        assertEquals("study2", second.elementName());
        assertEquals("STUDY", second.type());
        // this study sits directly in the root directory
        assertEquals(List.of("root"), second.path());
        assertEquals("John Doe", second.ownerLabel());
        assertEquals(LAST_MODIFICATION_DATE, second.lastModificationDate());
        assertEquals(MODIFIER_SUB, second.lastModifiedByLabel());

        // the studies are fetched in a single, non-strict call
        wireMockServer.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATH))
                .withQueryParam("strictMode", WireMock.equalTo("false")));
    }

    @Test
    void testTwoNodesInSameStudy() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        stubNodesInfos(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_1_UUID));
        stubStudies(studyStub(STUDY_1_UUID, "study1"));
        stubStudiesPaths(Map.of(STUDY_1_UUID, pathStub(STUDY_1_UUID, "study1", "root")));

        List<ConsumerElementInfos> infos = getConsumerElementInfos();

        // one line per reference, the study is repeated
        assertEquals(2, infos.size());
        assertEquals("study1", infos.get(0).elementName());
        assertEquals("study1", infos.get(1).elementName());
        assertEquals("node1", infos.get(0).node());
        assertEquals("node2", infos.get(1).node());
        // the paths are resolved in a single call
        wireMockServer.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATHS_PATH)));
    }

    @Test
    void testSameNodeReferencedTwice() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_1_UUID);
        stubNodesInfos(new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID));
        stubStudies(studyStub(STUDY_1_UUID, "study1"));
        stubStudiesPaths(Map.of(STUDY_1_UUID, pathStub(STUDY_1_UUID, "study1", "root")));

        List<ConsumerElementInfos> infos = getConsumerElementInfos();

        // one line per reference, even when they point to the same node
        assertEquals(2, infos.size());
        assertEquals(infos.get(0), infos.get(1));
        // the node is queried only once: duplicate references are collapsed before hitting the study-server
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo(NODES_INFOS_PATH))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(List.of(NODE_1_UUID)))));
    }

    @Test
    void testStudyNotReadableIsOmitted() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        stubNodesInfos(
                new NodeInfos(NODE_1_UUID, "node1", STUDY_1_UUID),
                new NodeInfos(NODE_2_UUID, "node2", STUDY_2_UUID));
        // the directory-server filters out the studies the user cannot read
        stubStudies(studyStub(STUDY_1_UUID, "study1"));
        stubStudiesPaths(Map.of(STUDY_1_UUID, pathStub(STUDY_1_UUID, "study1", "root")));

        List<ConsumerElementInfos> infos = getConsumerElementInfos();

        // only the readable study is described, the other one is dropped along with the node referencing it
        assertEquals(1, infos.size());
        ConsumerElementInfos readable = infos.get(0);
        assertEquals("node1", readable.node());
        assertEquals("study1", readable.elementName());
        assertEquals("STUDY", readable.type());
        assertEquals(List.of("root"), readable.path());
        assertEquals("John Doe", readable.ownerLabel());
        assertEquals(LAST_MODIFICATION_DATE, readable.lastModificationDate());
        assertEquals(MODIFIER_SUB, readable.lastModifiedByLabel());

        // nothing of the unreadable study leaks, not even the name of the node referencing it
        assertTrue(infos.stream().noneMatch(info -> "study2".equals(info.elementName()) || "node2".equals(info.node())));
    }

    @Test
    void testUnknownNodesLeaveNothingToDescribe() throws Exception {
        stubSharedElementReferences(NODE_1_UUID, NODE_2_UUID);
        // the study-server knows none of the referenced nodes anymore
        stubNodesInfos();

        assertTrue(getConsumerElementInfos().isEmpty());

        // with no resolved study, neither the directory elements/paths nor the user-identity server is queried
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATH)));
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATHS_PATH)));
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(USERS_IDENTITIES_PATH)));
    }

    @Test
    void testElementWithoutReferences() throws Exception {
        stubSharedElementReferences();

        assertTrue(getConsumerElementInfos().isEmpty());

        // without any reference, nothing is left to describe: no other server is reached
        wireMockServer.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo(NODES_INFOS_PATH)));
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATH)));
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(ELEMENTS_PATHS_PATH)));
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(USERS_IDENTITIES_PATH)));
    }
}
