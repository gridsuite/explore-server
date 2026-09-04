/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.ws.commons.SecuredZipInputStream;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.error.ExploreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Stream;
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
    public static final int MAX_UNCOMPRESSED_ARCHIVE_SIZE = 2000000000;
    public static final int MAX_ARCHIVE_ENTRIES = 1000;
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
     * Import a study from an archive asynchronously
     * @param archiveFile the zip archive file
     * @param studyName the name for the new study
     * @param description the description for the new study
     * @param userId the user ID
     * @param parentDirectoryUuid the parent directory UUID
     */
    @Async
    public void importStudy(MultipartFile archiveFile, String studyName, String description, String userId, UUID parentDirectoryUuid) {
        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            Path tempDir = Files.createTempDirectory("study-import-", attr);
            try {
                importStudyFromArchive(archiveFile, studyName, description, userId, parentDirectoryUuid, tempDir);
            } finally {
                deleteDirectory(tempDir);
            }
        } catch (ExploreException e) {
            throw new ExploreException(e.getBusinessErrorCode(), "Error importing study archive '" + studyName + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "Error while importing study: " + e.getMessage(), e);
        }
    }

    private void importStudyFromArchive(MultipartFile archiveFile, String studyName, String description, String userId,
                                        UUID parentDirectoryUuid, Path tempDir) throws IOException {
        extractArchive(archiveFile.getInputStream(), tempDir);
        Path studyJsonPath = tempDir.resolve("tree.json");
        if (!Files.exists(studyJsonPath)) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "tree.json not found in archive");
        }
        TreeExportInfos treeExportInfos = objectMapper.readValue(studyJsonPath.toFile(), TreeExportInfos.class);
        if (treeExportInfos == null || treeExportInfos.rootNetworks() == null || treeExportInfos.rootNetworks().isEmpty()) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "No root networks found in archive");
        }
        Map<UUID, UUID> oldCaseUuidToNewCaseUuid = new HashMap<>();
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, DIRECTORY, userId, 0L, null);
        ElementAttributes newElementAttributes = directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        UUID importDirectoryUuid = newElementAttributes.getElementUuid();
        try {
            Path casesDir = tempDir.resolve("cases");
            for (var rootNetwork : treeExportInfos.rootNetworks()) {
                importCaseForRootNetwork(rootNetwork, casesDir, oldCaseUuidToNewCaseUuid, description, userId, importDirectoryUuid);
            }
            checkAllCasesWereImported(treeExportInfos, oldCaseUuidToNewCaseUuid);
            UUID createdStudyUuid = UUID.randomUUID();
            TreeExportInfos updatedExportInfos = updateCaseUuidsAndStudyUuidInExportInfos(treeExportInfos, oldCaseUuidToNewCaseUuid, createdStudyUuid);
            createStudyFromImport(createdStudyUuid, studyName, userId, description, importDirectoryUuid, updatedExportInfos);
        } catch (Exception e) {
            directoryService.deleteElement(importDirectoryUuid, userId);
            throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import study: " + e.getMessage());
        }
    }

    private void createStudyFromImport(UUID createdStudyUuid, String studyName, String userId, String description,
                                                 UUID parentDirectoryUuid, TreeExportInfos updatedExportInfos) {
        ElementAttributes elementAttributes = new ElementAttributes(createdStudyUuid, studyName, STUDY, userId, 0L, description, DirectoryElementStatus.CREATING);
        studyService.importStudy(userId, updatedExportInfos);
        exploreService.createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, userId, studyService::delete);
    }

    private void importCaseForRootNetwork(RootNetworkExportInfos rootNetwork, Path casesDir, Map<UUID, UUID> oldCaseUuidToNewCaseUuid, String description, String userId, UUID parentDirectoryUuid) {
        UUID oldCaseUuid = rootNetwork.caseInfos().caseUuid();
        String caseName = rootNetwork.caseInfos().caseName();
        Path caseDir = casesDir.resolve(oldCaseUuid.toString());
        Path caseFile = caseDir.resolve(caseName);
        try {
            UUID newCaseUuid = caseService.importFileCase(caseFile.toFile());
            ElementAttributes elementAttributes = new ElementAttributes(newCaseUuid, caseName, CASE, userId, 0L, description);
            oldCaseUuidToNewCaseUuid.put(oldCaseUuid, newCaseUuid);
            exploreService.createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, userId, caseService::delete);
        } catch (ExploreException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to import case file {}: {}", caseFile, e.getMessage(), e);
            throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import case file: " + caseName + ": " + e.getMessage());
        }
    }

    private void checkAllCasesWereImported(TreeExportInfos treeExportInfos, Map<UUID, UUID> oldCaseUuidToNewCaseUuid) {
        for (var rootNetwork : treeExportInfos.rootNetworks()) {
            UUID oldCaseUuid = rootNetwork.caseInfos().caseUuid();
            if (!oldCaseUuidToNewCaseUuid.containsKey(oldCaseUuid)) {
                throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import case: " + rootNetwork.caseInfos().caseName());
            }
        }
    }

    /**
     * Extract zip archive to directory
     */
    private void extractArchive(InputStream inputStream, Path destDir) throws IOException {
        try (SecuredZipInputStream zipIn = new SecuredZipInputStream(inputStream, MAX_ARCHIVE_ENTRIES, MAX_UNCOMPRESSED_ARCHIVE_SIZE)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path outputPath = destDir.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(destDir)) {
                    throw new IOException("Invalid zip entry: " + entry.getName());
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
    }

    /**
     * Update case UUIDs in StudyExportInfos with new imported case UUIDs
     */
    private TreeExportInfos updateCaseUuidsAndStudyUuidInExportInfos(TreeExportInfos original, Map<UUID, UUID> oldCaseUuidToNewCaseUuid, UUID createdStudyUuid) {
        List<RootNetworkExportInfos> updatedRootNetworks = original.rootNetworks().stream().map(rootNetwork -> {
            UUID oldCaseUuid = rootNetwork.caseInfos().caseUuid();
            UUID newCaseUuid = oldCaseUuidToNewCaseUuid.get(oldCaseUuid);
            CaseInfos updatedCaseInfo = new CaseInfos(newCaseUuid, rootNetwork.caseInfos().originalCaseUuid(),
                    rootNetwork.caseInfos().caseName(), rootNetwork.caseInfos().caseFormat());
            return new RootNetworkExportInfos(rootNetwork.name(), rootNetwork.tag(), rootNetwork.index(), updatedCaseInfo, rootNetwork.importParameters());
        }).toList();
        return new TreeExportInfos(createdStudyUuid, updatedRootNetworks, original.nodeTree(), original.computationParameters());
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete {}", path, e);
                    }
                });
            }
        }
    }
}
