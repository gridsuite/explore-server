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
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.services.CaseService;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.StudyService;
import org.gridsuite.explore.server.services.UserAdminService;
import org.gridsuite.explore.server.utils.WireMockUtils;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
class StudyImportExportTest {

    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID STUDY_UUID = UUID.randomUUID();
    private static final String USER_ID = "testUser";
    private static final String STUDY_NAME = "Test Study";
    private static final String DESCRIPTION = "Test Description";

    @Autowired
    private MockMvc mockMvc;

    private WireMockServer wireMockServer;

    protected WireMockUtils wireMockUtils;

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

    @BeforeEach
    void setUp() throws JsonProcessingException {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockUtils = new WireMockUtils(wireMockServer);
        wireMockServer.start();
        studyService.setStudyServerBaseUri(wireMockServer.baseUrl());
        caseService.setBaseUri(wireMockServer.baseUrl());
        directoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        userAdminService.setUserAdminServerBaseUri(wireMockServer.baseUrl());

        // Stub case-server
        wireMockServer.stubFor(post(urlPathMatching("/v1/cases"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(objectMapper.writeValueAsString(CASE_UUID))));
        wireMockServer.stubFor(get(urlPathMatching("/v1/users/.*/cases/count"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("0")));
        // Stub study-server: import-with-case-import-action
        wireMockServer.stubFor(post(urlPathMatching("/v1/studies/import-with-case-import-action/.*"))
                .willReturn(aResponse().withStatus(200)));
        // Stub directory-server
        wireMockServer.stubFor(get(urlPathMatching("/v1/elements/authorized"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("true")));
        wireMockServer.stubFor(get(urlPathMatching("/v1/cases-alert-threshold"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("10")));
        // Stub user-admin-server max quota
        wireMockServer.stubFor(get(urlPathMatching("/v1/users/.*/quota/max"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Map.of(QuotaType.CASES, 10)))));
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
                "study-export.gz",
                "application/gzip",
                archiveContent
        );

        // Import the study
        MvcResult result = mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        // Verify the import was initiated
        assertNotNull(result);
    }

    @Test
    void testImportStudyArchiveMissingStudyJson() throws Exception {
        // Create an archive without study.json
        byte[] archiveContent = createArchiveWithoutStudyJson();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "invalid-study.gz",
                "application/gzip",
                archiveContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testImportStudyArchiveInvalidZipFile() throws Exception {
        // Create an invalid zip file
        byte[] invalidContent = "This is not a valid zip file".getBytes();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "invalid.gz",
                "application/gzip",
                invalidContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testImportStudyArchiveMultipleRootNetworks() throws Exception {
        // Create an archive with multiple root networks
        byte[] archiveContent = createArchiveWithMultipleRootNetworks();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "multi-root-study.gz",
                "application/gzip",
                archiveContent
        );

        // Import the study
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void testImportStudyArchiveEmptyRootNetworks() throws Exception {
        // Create an archive with no root networks
        byte[] archiveContent = createArchiveWithEmptyRootNetworks();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "empty-roots.gz",
                "application/gzip",
                archiveContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testImportStudyArchiveMissingCaseFile() throws Exception {
        // Create an archive where the case file referenced in study.json doesn't exist
        byte[] archiveContent = createArchiveWithMissingCaseFile();
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "missing-case.gz",
                "application/gzip",
                archiveContent
        );

        // Attempt to import - should fail
        mockMvc.perform(multipart("/v1/explore/studies/import")
                        .file(archiveFile)
                        .param("studyName", STUDY_NAME)
                        .param("description", DESCRIPTION)
                        .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                        .header("userId", USER_ID))
                .andExpect(status().is5xxServerError());
    }

    // Helper methods to create test archives

    private byte[] createValidStudyArchive() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add study.json
            StudyExportInfos exportInfos = createStudyExportInfos();
            addJsonEntry(zos, exportInfos);

            // Add case file
            String caseName = "testCase.xiidm";
            addFileEntry(zos, "cases/" + CASE_UUID + "/" + caseName, "<network></network>".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithoutStudyJson() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add only case file, no study.json
            addFileEntry(zos, "cases/" + CASE_UUID + "/test.xiidm", "<network></network>".getBytes());
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithMultipleRootNetworks() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Create export info with 3 root networks
            StudyExportInfos exportInfos = createStudyExportInfosWithMultipleRoots();
            addJsonEntry(zos, exportInfos);

            // Add case files for each root network
            for (RootNetworkExportInfos rootNetwork : exportInfos.rootNetworks()) {
                UUID caseUuid = rootNetwork.caseInfos().uuid();
                String caseName = rootNetwork.caseInfos().name();
                addFileEntry(zos, "cases/" + caseUuid + "/" + caseName, "<network></network>".getBytes());
            }
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithEmptyRootNetworks() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Create export info with empty root networks list
            StudyExportInfos exportInfos = new StudyExportInfos(
                    STUDY_UUID,
                    Collections.emptyList(),
                    createNodeTree()
            );
            addJsonEntry(zos, exportInfos);
        }
        return baos.toByteArray();
    }

    private byte[] createArchiveWithMissingCaseFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add study.json with case reference
            StudyExportInfos exportInfos = createStudyExportInfos();
            addJsonEntry(zos, exportInfos);
            // But don't add the actual case file
        }
        return baos.toByteArray();
    }

    private void addJsonEntry(ZipOutputStream zos, Object content) throws IOException {
        ZipEntry entry = new ZipEntry("study.json");
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

    private StudyExportInfos createStudyExportInfos() {
        CaseExportInfos caseInfo = new CaseExportInfos(CASE_UUID, "testCase.xiidm");
        RootNetworkExportInfos rootNetwork = new RootNetworkExportInfos(
                "Network 1",
                "1",
                "XIIDM",
                caseInfo,
                Collections.emptyMap()
        );
        return new StudyExportInfos(
                STUDY_UUID,
                Collections.singletonList(rootNetwork),
                createNodeTree()
        );
    }

    private StudyExportInfos createStudyExportInfosWithMultipleRoots() {
        List<RootNetworkExportInfos> rootNetworks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UUID caseUuid = UUID.randomUUID();
            CaseExportInfos caseInfo = new CaseExportInfos(caseUuid, "case" + i + ".xiidm");
            RootNetworkExportInfos rootNetwork = new RootNetworkExportInfos(
                    "Network " + (i + 1),
                    String.valueOf(i + 1),
                    "XIIDM",
                    caseInfo,
                    Collections.emptyMap()
            );
            rootNetworks.add(rootNetwork);
        }
        return new StudyExportInfos(STUDY_UUID, rootNetworks, createNodeTree());
    }

    private NodeTreeExportInfos createNodeTree() {
        List<NodeTreeExportInfos> children = new ArrayList<>();
        children.add(new NodeTreeExportInfos(
                UUID.randomUUID(),
                "Node 1",
                "NETWORK_MODIFICATION",
                UUID.randomUUID(),
                "BUILT",
                Collections.emptyList()
        ));
        return new NodeTreeExportInfos(
                UUID.randomUUID(),
                "Root",
                "ROOT",
                null,
                null,
                children
        );
    }

    public CaseService getCaseService() {
        return caseService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }
}
