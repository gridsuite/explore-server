/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.ws.commons.SecuredZipInputStream;
import org.apache.commons.io.FileUtils;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.error.ExploreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.zip.ZipEntry;

import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.IMPORT_STUDY_FAILED;
import static org.gridsuite.explore.server.services.ExploreService.CASE;
import static org.gridsuite.explore.server.services.ExploreService.DIRECTORY;
import static org.gridsuite.explore.server.services.ExploreService.STUDY;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Service
public class StudyImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyImportService.class);
    public static final long MAX_UNCOMPRESSED_ARCHIVE_SIZE = 10000000000L;
    public static final int MAX_ARCHIVE_ENTRIES = 5000;
    public static final String TREE_EXPORT_FILE = "tree.json";
    public static final String CASES_DIR = "cases";
    private final CaseService caseService;
    private final StudyService studyService;
    private final ObjectMapper objectMapper;
    private final ExploreService exploreService;
    private final DirectoryService directoryService;

    public StudyImportService(CaseService caseService, StudyService studyService, ObjectMapper objectMapper, ExploreService exploreService, DirectoryService directoryService) {
        this.caseService = caseService;
        this.studyService = studyService;
        this.objectMapper = objectMapper;
        this.exploreService = exploreService;
        this.directoryService = directoryService;
    }

    /**
     * Import a study from an archive synchronously
     * @param archiveFile the zip archive file
     * @param studyName the name for the new study
     * @param description the description for the new study
     * @param userId the user ID
     * @param parentDirectoryUuid the parent directory UUID
     */
    public void importStudy(MultipartFile archiveFile, String studyName, String description, String userId, UUID parentDirectoryUuid) {
        Path tempDir = null;
        UUID createdDirectoryUuid = null;
        try {
            tempDir = extractArchiveToDisk(archiveFile);

            ElementAttributes directoryAttributes = new ElementAttributes(UUID.randomUUID(), studyName, DIRECTORY, userId, 0L, null);
            createdDirectoryUuid = directoryService.createElement(directoryAttributes, parentDirectoryUuid, userId).getElementUuid();

            TreeExportInfos treeExportInfos = objectMapper.readValue(tempDir.resolve(TREE_EXPORT_FILE).toFile(), TreeExportInfos.class);
            if (treeExportInfos.getRootNetworks() == null || treeExportInfos.getRootNetworks().isEmpty()) {
                throw new ExploreException(IMPORT_STUDY_FAILED, "No root networks found in archive");
            }

            createCases(treeExportInfos, tempDir.resolve(CASES_DIR), createdDirectoryUuid, userId, description);

            createStudy(treeExportInfos, studyName, createdDirectoryUuid, userId, description);
        } catch (Exception e) {
            if (createdDirectoryUuid != null) {
                directoryService.deleteElement(createdDirectoryUuid, userId);
            }
            throw new ExploreException(IMPORT_STUDY_FAILED, "Error while importing study '" + studyName + "': " + e.getMessage(), e);
        } finally {
            try {
                if (Files.exists(tempDir)) {
                    FileUtils.deleteDirectory(tempDir.toFile());
                }
            } catch (IOException e) {
                LOGGER.error("Error cleaning up temporary directory: " + tempDir, e);
            }
        }
    }

    private Path extractArchiveToDisk(MultipartFile archiveFile) throws IOException {
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        Path tempDir = Files.createTempDirectory("study-import-", attr);
        try (SecuredZipInputStream zipIn = new SecuredZipInputStream(archiveFile.getInputStream(), MAX_ARCHIVE_ENTRIES, MAX_UNCOMPRESSED_ARCHIVE_SIZE)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path outputPath = tempDir.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(tempDir)) {
                    throw new IOException("Zip entry is outside of the target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    if (outputPath.getParent() != null) {
                        Files.createDirectories(outputPath.getParent());
                    }
                    Files.copy(zipIn, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipIn.closeEntry();
            }
        }
        return tempDir;
    }

    private void createCases(TreeExportInfos treeExportInfos, Path casesDir, UUID createdDirectoryUuid, String userId, String description) {
        treeExportInfos.getRootNetworks().forEach(rootNetwork -> {
            CaseInfos caseInfos = rootNetwork.caseInfos();
            Path caseFile = casesDir.resolve(caseInfos.getCaseUuid().toString()).resolve(caseInfos.getCaseName()).normalize();
            UUID newCaseUuid = caseService.importFileCase(caseFile.toFile());
            ElementAttributes caseElementAttributes = new ElementAttributes(newCaseUuid, caseInfos.getCaseName(), CASE, userId, 0L, description);
            exploreService.createDirectoryElementWithNewNameOrDeleteElement(caseElementAttributes, createdDirectoryUuid, userId, caseService::delete);
            caseInfos.setCaseUuid(newCaseUuid);
        });
    }

    private void createStudy(TreeExportInfos treeExportInfos, String studyName, UUID createdDirectoryUuid, String userId, String description) {
        UUID createdStudyUuid = UUID.randomUUID();
        treeExportInfos.setStudyUuid(createdStudyUuid);
        ElementAttributes elementAttributes = new ElementAttributes(createdStudyUuid, studyName, STUDY, userId, 0L, description, DirectoryElementStatus.CREATING);
        studyService.importStudy(userId, treeExportInfos);
        exploreService.createDirectoryElementOrDeleteElement(elementAttributes, createdDirectoryUuid, userId, studyService::delete);
    }
}
