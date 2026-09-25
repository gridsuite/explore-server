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
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.services.CaseService;
import org.gridsuite.explore.server.services.ContingencyListService;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.FilterService;
import org.gridsuite.explore.server.services.StudyService;
import org.gridsuite.explore.server.services.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
class StudyImportTest {

    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID STUDY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String STUDY_NAME = "Test Study";
    private static final String DESCRIPTION = "Test Description";

    @Autowired
    private MockMvc mockMvc;

    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyService studyService;

    @Autowired
    private CaseService caseService;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private FilterService filterService;

    @Autowired
    private ContingencyListService contingencyListService;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        studyService.setStudyServerBaseUri(wireMockServer.baseUrl());
        caseService.setBaseUri(wireMockServer.baseUrl());
        directoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        userAdminService.setUserAdminServerBaseUri(wireMockServer.baseUrl());
        filterService.setFilterServerBaseUri(wireMockServer.baseUrl());
        contingencyListService.setActionsServerBaseUri(wireMockServer.baseUrl());

        // Stub case-server
        wireMockServer.stubFor(post(urlPathMatching("/v1/cases"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(objectMapper.writeValueAsString(CASE_UUID))));
        wireMockServer.stubFor(get(urlPathMatching("/v1/users/.*/cases/count"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("0")));
        // Stub study-server: import
        wireMockServer.stubFor(post(urlPathMatching("/v1/studies/import"))
                .willReturn(aResponse().withStatus(200)));
        // Stub study-server: set the study computation parameters
        wireMockServer.stubFor(post(urlPathMatching("/v1/studies/.*/.*/parameters"))
                .willReturn(aResponse().withStatus(200)));
        // Stub directory-server
        wireMockServer.stubFor(get(urlPathMatching("/v1/elements/authorized"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("true")));
        wireMockServer.stubFor(post(urlPathMatching("/v1/directories/.*/elements"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(new ElementAttributes(UUID.randomUUID(), STUDY_NAME, "DIRECTORY", USER_ID, 0L, null)))));
        wireMockServer.stubFor(get(urlPathMatching("/v1/cases-alert-threshold"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("10")));
        // Stub user-admin-server quota state
        wireMockServer.stubFor(get(urlPathMatching("/v1/users/.*/quota/state"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Map.of(QuotaType.CASES, new QuotaState(0, 10))))));
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testImportStudyArchive() throws Exception {
        // Create a valid archive
        byte[] archiveContent = createValidStudyArchive();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "study-export.zip",
                "application/zip",
                archiveContent
        );

        // Import the study
        MvcResult result = mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isOk())
                .andReturn();

        // Verify the import was initiated
        assertNotNull(result);
    }

    @Test
    void testImportStudyFiltersReferencedByFiltersAndContingencyLists() throws Exception {
        UUID oldReferencedFilter = UUID.randomUUID();
        UUID oldExpertFilter = UUID.randomUUID();
        UUID oldIdentifierContingencyList = UUID.randomUUID();
        UUID oldFilterBasedContingencyList = UUID.randomUUID();
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/filters")).willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/identifier-contingency-lists")).willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/filters-contingency-lists")).willReturn(aResponse().withStatus(200)));

        String referencedFilter = "{\"id\":\"" + oldReferencedFilter + "\",\"type\":\"EXPERT\",\"equipmentType\":\"VOLTAGE_LEVEL\"}";
        String expertFilter = "{\"id\":\"" + oldExpertFilter + "\",\"type\":\"EXPERT\",\"equipmentType\":\"LINE\","
                + "\"rules\":{\"dataType\":\"FILTER_UUID\",\"values\":[\"" + oldReferencedFilter + "\"]}}";
        String identifiersList = "{\"metadata\":{\"id\":\"" + oldIdentifierContingencyList + "\",\"type\":\"IDENTIFIERS\"},"
                + "\"identifierContingencyList\":{\"name\":\"" + oldIdentifierContingencyList + "\"},\"type\":\"IDENTIFIERS\"}";
        String filtersList = "{\"metadata\":{\"id\":\"" + oldFilterBasedContingencyList + "\",\"type\":\"FILTERS\"},"
                + "\"filters\":[{\"id\":\"" + oldReferencedFilter + "\"}],"
                + "\"selectedEquipmentTypesByFilter\":[{\"filterId\":\"" + oldReferencedFilter + "\",\"equipmentTypes\":[\"LINE\"]}],\"type\":\"FILTERS\"}";
        // the expert filter is written before the filter it references: the import must not depend on the order
        byte[] archiveContent = createStudyArchiveWithDefinitions(
                List.of(new ExportedElementInfos(oldExpertFilter, "expert", objectMapper.readTree(expertFilter)),
                        new ExportedElementInfos(oldReferencedFilter, "referenced", objectMapper.readTree(referencedFilter))),
                List.of(new ExportedElementInfos(oldIdentifierContingencyList, "identifiers", objectMapper.readTree(identifiersList)),
                        new ExportedElementInfos(oldFilterBasedContingencyList, "filters", objectMapper.readTree(filtersList))));

        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(new MockMultipartFile("archiveFile", "study-export.zip", "application/zip", archiveContent))
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isOk());

        List<LoggedRequest> filterRequests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/v1/filters")));
        assertEquals(2, filterRequests.size());
        LoggedRequest newReferencedFilterRequest = findRequestContaining(filterRequests, "VOLTAGE_LEVEL");
        LoggedRequest newExpertFilterRequest = findRequestContaining(filterRequests, "FILTER_UUID");
        String newReferencedFilter = newReferencedFilterRequest.queryParameter("id").firstValue();
        String newExpertFilter = newExpertFilterRequest.queryParameter("id").firstValue();
        assertNotEquals(oldReferencedFilter.toString(), newReferencedFilter);
        assertNotEquals(oldExpertFilter.toString(), newExpertFilter);

        // each filter content now uses the new uuids, including the reference from the expert filter
        assertEquals(referencedFilter.replace(oldReferencedFilter.toString(), newReferencedFilter), newReferencedFilterRequest.getBodyAsString());
        assertEquals(expertFilter.replace(oldExpertFilter.toString(), newExpertFilter).replace(oldReferencedFilter.toString(), newReferencedFilter),
                newExpertFilterRequest.getBodyAsString());

        // the filter based contingency list references the recreated filter, in its filters and in its selected equipment types
        List<LoggedRequest> filterBasedRequests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/v1/filters-contingency-lists")));
        assertEquals(1, filterBasedRequests.size());
        String newFilterBasedContingencyList = filterBasedRequests.getFirst().queryParameter("id").firstValue();
        assertEquals(filtersList.replace(oldFilterBasedContingencyList.toString(), newFilterBasedContingencyList).replace(oldReferencedFilter.toString(), newReferencedFilter),
                filterBasedRequests.getFirst().getBodyAsString());

        List<LoggedRequest> identifierRequests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/v1/identifier-contingency-lists")));
        assertEquals(1, identifierRequests.size());
        String newIdentifierContingencyList = identifierRequests.getFirst().queryParameter("id").firstValue();
        assertEquals(identifiersList.replace(oldIdentifierContingencyList.toString(), newIdentifierContingencyList), identifierRequests.getFirst().getBodyAsString());
    }

    @Test
    void testImportStudyComputationParameters() throws Exception {
        UUID oldFilter = UUID.randomUUID();
        UUID oldContingencyList = UUID.randomUUID();
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/filters")).willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/identifier-contingency-lists")).willReturn(aResponse().withStatus(200)));
        String filter = "{\"id\":\"" + oldFilter + "\",\"type\":\"EXPERT\",\"equipmentType\":\"GENERATOR\"}";
        String contingencyList = "{\"id\":\"" + oldContingencyList + "\",\"type\":\"IDENTIFIERS\"}";
        String loadFlowParameters = "{\"provider\":\"OpenLoadFlow\"}";
        String securityAnalysisParameters = "{\"contingencyListsInfos\":[{\"contingencyLists\":[\"" + oldContingencyList + "\"],\"activated\":true}]}";
        String voltageInitParameters = "{\"variableQGenerators\":[{\"filterId\":\"" + oldFilter + "\",\"filterName\":\"generators\"}]}";
        byte[] archiveContent = createStudyArchiveWithDefinitions(
                List.of(new ExportedElementInfos(oldFilter, "generators", objectMapper.readTree(filter))),
                List.of(new ExportedElementInfos(oldContingencyList, "identifiers", objectMapper.readTree(contingencyList))),
                Map.of("LOAD_FLOW", loadFlowParameters, "SECURITY_ANALYSIS", securityAnalysisParameters, "VOLTAGE_INITIALIZATION", voltageInitParameters));

        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(new MockMultipartFile("archiveFile", "study-export.zip", "application/zip", archiveContent))
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isOk());

        String newFilter = wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/v1/filters"))).getFirst().queryParameter("id").firstValue();
        String newContingencyList = wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/v1/identifier-contingency-lists"))).getFirst().queryParameter("id").firstValue();

        // only the exported computation parameters are set, with the uuids of the imported filters and contingency lists
        wireMockServer.verify(3, postRequestedFor(urlPathMatching("/v1/studies/.*/.*/parameters")).withHeader("userId", equalTo(USER_ID)));
        wireMockServer.verify(0, postRequestedFor(urlPathMatching("/v1/studies/.*/(sensitivity-analysis|pcc-min|short-circuit-analysis)/parameters")));
        wireMockServer.verify(postRequestedFor(urlPathMatching("/v1/studies/.*/loadflow/parameters"))
                .withRequestBody(equalToJson(loadFlowParameters)));
        wireMockServer.verify(postRequestedFor(urlPathMatching("/v1/studies/.*/security-analysis/parameters"))
                .withRequestBody(equalToJson(securityAnalysisParameters.replace(oldContingencyList.toString(), newContingencyList))));
        // the voltage init parameters are wrapped in the study voltage init parameters
        wireMockServer.verify(postRequestedFor(urlPathMatching("/v1/studies/.*/voltage-init/parameters"))
                .withRequestBody(equalToJson("{\"computationParameters\":" + voltageInitParameters.replace(oldFilter.toString(), newFilter)
                        + ",\"applyModifications\":true}")));
    }

    private static LoggedRequest findRequestContaining(List<LoggedRequest> requests, String marker) {
        return requests.stream()
                .filter(request -> request.getBodyAsString().contains(marker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No request containing " + marker));
    }

    @Test
    void testImportStudyArchiveMissingTreeJson() throws Exception {
        // Create an archive without tree.json
        byte[] archiveContent = createArchiveWithoutTreeJson();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "invalid-study.zip",
                "application/zip",
                archiveContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testImportStudyArchiveInvalidZipFile() throws Exception {
        // Create an invalid zip file
        byte[] invalidContent = "This is not a valid zip file".getBytes();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "invalid.zip",
                "application/zip",
                invalidContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testImportStudyArchiveMultipleRootNetworks() throws Exception {
        // Create an archive with multiple root networks
        byte[] archiveContent = createArchiveWithMultipleRootNetworks();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "multi-root-study.zip",
                "application/zip",
                archiveContent
        );

        // Import the study
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testImportStudyArchiveEmptyRootNetworks() throws Exception {
        // Create an archive with no root networks
        byte[] archiveContent = createArchiveWithEmptyRootNetworks();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "empty-roots.zip",
                "application/zip",
                archiveContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testImportStudyFailure() throws Exception {
        byte[] archiveContent = createArchiveWithOneMissingCaseFileAmongTwoRoots();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "partial-study.zip",
                "application/zip",
                archiveContent
        );

        wireMockServer.stubFor(post(urlPathEqualTo("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements"))
                .atPriority(1)
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case-valid.xiidm", "CASE", USER_ID, 0L, DESCRIPTION)))));
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/elements/" + PARENT_DIRECTORY_UUID))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(new ElementAttributes(PARENT_DIRECTORY_UUID, "Parent directory", "DIRECTORY", USER_ID, 0L, null)))));
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(new ElementAttributes(CASE_UUID, "case-valid.xiidm", "CASE", USER_ID, 0L, DESCRIPTION))))));
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/elements/" + CASE_UUID))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case-valid.xiidm", "CASE", USER_ID, 0L, DESCRIPTION)))));
        wireMockServer.stubFor(delete(urlPathEqualTo("/v1/cases/" + CASE_UUID))
                .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString()))
                .andExpect(status().is5xxServerError());

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/v1/elements/" + PARENT_DIRECTORY_UUID)));
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")));
        wireMockServer.verify(deleteRequestedFor(urlPathEqualTo("/v1/cases/" + CASE_UUID)));
    }

    private byte[] createValidStudyArchive() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add tree.json
            TreeExportInfos exportInfos = createStudyExportInfos();
            addJsonEntry(zos, exportInfos);

            // Add case file
            String caseName = "testCase.xiidm";
            addFileEntry(zos, "cases/" + CASE_UUID + "/" + caseName, "<network></network>".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createStudyArchiveWithDefinitions(List<ExportedElementInfos> filters, List<ExportedElementInfos> contingencyLists) throws IOException {
        return createStudyArchiveWithDefinitions(filters, contingencyLists, Map.of());
    }

    private byte[] createStudyArchiveWithDefinitions(List<ExportedElementInfos> filters, List<ExportedElementInfos> contingencyLists,
                                                     Map<String, String> parametersByType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addJsonEntry(zos, createStudyExportInfos());
            addFileEntry(zos, "cases/" + CASE_UUID + "/testCase.xiidm", "<network></network>".getBytes());
            addFileEntry(zos, "computationParameters/filters.json", objectMapper.writeValueAsBytes(filters));
            addFileEntry(zos, "computationParameters/contingencyList.json", objectMapper.writeValueAsBytes(contingencyLists));
            for (Map.Entry<String, String> parameters : parametersByType.entrySet()) {
                addFileEntry(zos, "computationParameters/" + parameters.getKey() + ".json", parameters.getValue().getBytes());
            }
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithoutTreeJson() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add only case file, no tree.json
            addFileEntry(zos, "cases/" + CASE_UUID + "/test.xiidm", "<network></network>".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithMultipleRootNetworks() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Create export info with 3 root networks
            TreeExportInfos exportInfos = createStudyExportInfosWithMultipleRoots();
            addJsonEntry(zos, exportInfos);

            // Add case files for each root network
            for (RootNetworkExportInfos rootNetwork : exportInfos.getRootNetworks()) {
                UUID caseUuid = rootNetwork.caseInfos().getCaseUuid();
                String caseName = rootNetwork.caseInfos().getCaseName();
                addFileEntry(zos, "cases/" + caseUuid + "/" + caseName, "<network></network>".getBytes());
            }
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithEmptyRootNetworks() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Create export info with empty root networks list
            TreeExportInfos exportInfos = new TreeExportInfos(
                    STUDY_UUID,
                    Collections.emptyList(),
                    createNodeTree()
            );
            addJsonEntry(zos, exportInfos);
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithOneMissingCaseFileAmongTwoRoots() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            UUID validCaseUuid = UUID.randomUUID();
            CaseInfos validCaseInfo = new CaseInfos(validCaseUuid, UUID.randomUUID(), "case-valid.xiidm", "XIIDM");
            RootNetworkExportInfos validRootNetwork = new RootNetworkExportInfos("Network 1", "1", 0, validCaseInfo, Collections.emptyMap());

            UUID missingCaseUuid = UUID.randomUUID();
            CaseInfos missingCaseInfo = new CaseInfos(missingCaseUuid, UUID.randomUUID(), "case-missing.xiidm", "XIIDM");
            RootNetworkExportInfos missingRootNetwork = new RootNetworkExportInfos("Network 2", "2", 1, missingCaseInfo, Collections.emptyMap());

            TreeExportInfos exportInfos = new TreeExportInfos(STUDY_UUID, List.of(validRootNetwork, missingRootNetwork), createNodeTree());
            addJsonEntry(zos, exportInfos);

            // only the first root network's case file is present in the archive
            addFileEntry(zos, "cases/" + validCaseUuid + "/case-valid.xiidm", "<network></network>".getBytes());
        }
        return baos.toByteArray();
    }

    private void addJsonEntry(ZipOutputStream zos, Object content) throws IOException {
        ZipEntry entry = new ZipEntry("tree.json");
        zos.putNextEntry(entry);
        zos.write(objectMapper.writeValueAsBytes(content));
        zos.closeEntry();
    }

    private void addFileEntry(ZipOutputStream zos, String entryName, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private TreeExportInfos createStudyExportInfos() {
        CaseInfos caseInfo = new CaseInfos(CASE_UUID, UUID.randomUUID(), "testCase.xiidm", "XIIDM");
        RootNetworkExportInfos rootNetwork = new RootNetworkExportInfos(
                "Network 1",
                "1",
                0,
                caseInfo,
                Collections.emptyMap()
        );
        return new TreeExportInfos(
                STUDY_UUID,
                Collections.singletonList(rootNetwork),
                createNodeTree()
        );
    }

    private TreeExportInfos createStudyExportInfosWithMultipleRoots() {
        List<RootNetworkExportInfos> rootNetworks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UUID caseUuid = UUID.randomUUID();
            CaseInfos caseInfo = new CaseInfos(caseUuid, UUID.randomUUID(), "case" + i + ".xiidm", "XIIDM");
            RootNetworkExportInfos rootNetwork = new RootNetworkExportInfos(
                    "Network " + (i + 1),
                    String.valueOf(i + 1),
                    0,
                    caseInfo,
                    Collections.emptyMap()
            );
            rootNetworks.add(rootNetwork);
        }
        return new TreeExportInfos(STUDY_UUID, rootNetworks, createNodeTree());
    }

    private NodeTreeExportInfos createNodeTree() {
        List<NodeTreeExportInfos> children = new ArrayList<>();
        children.add(new NodeTreeExportInfos(
                "Node 1",
                "NETWORK_MODIFICATION",
                UUID.randomUUID(),
                "CONSTRUCTION",
                Collections.emptyList()
        ));
        return new NodeTreeExportInfos(
                "Root",
                "ROOT",
                null,
                "CONSTRUCTION",
                children
        );
    }
}
