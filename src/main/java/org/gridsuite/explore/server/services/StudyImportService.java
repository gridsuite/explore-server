/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.explore.server.dto.CaseExportInfos;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.RootNetworkExportInfos;
import org.gridsuite.explore.server.dto.StudyExportInfos;
import org.gridsuite.explore.server.error.ExploreException;
import org.gridsuite.explore.server.dto.CaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.IMPORT_STUDY_FAILED;
import static org.gridsuite.explore.server.services.ExploreService.STUDY;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Service
public class StudyImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyImportService.class);

    private final CaseService caseService;
    private final StudyService studyService;
    private final ExploreService exploreService;
    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;

    public StudyImportService(CaseService caseService, StudyService studyService, ExploreService exploreService, DirectoryService directoryService, ObjectMapper objectMapper) {
        this.caseService = caseService;
        this.studyService = studyService;
        this.exploreService = exploreService;
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
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
    public void importStudyArchive(
            MultipartFile archiveFile,
            String studyName,
            String description,
            String userId,
            UUID parentDirectoryUuid) {

        LOGGER.info("Starting import of study archive: {}", studyName);

        try {
            Path tempDir = Files.createTempDirectory("study-import-");

            try {
                extractArchive(archiveFile.getInputStream(), tempDir);

                Path studyJsonPath = tempDir.resolve("study.json");
                if (!Files.exists(studyJsonPath)) {
                    throw new ExploreException(IMPORT_STUDY_FAILED, "study.json not found in archive");
                }

                StudyExportInfos studyExportInfos = objectMapper.readValue(studyJsonPath.toFile(), StudyExportInfos.class);

                if (studyExportInfos.rootNetworks() == null || studyExportInfos.rootNetworks().isEmpty()) {
                    throw new ExploreException(IMPORT_STUDY_FAILED, "No root networks found in archive");
                }

                Map<UUID, UUID> caseUuidMapping = new HashMap<>();
                Path casesDir = tempDir.resolve("cases");

                if (Files.exists(casesDir) && Files.isDirectory(casesDir)) {
                    LOGGER.info("Cases directory found: {}", casesDir);
                    for (var rootNetwork : studyExportInfos.rootNetworks()) {
                        UUID oldCaseUuid = rootNetwork.caseInfos().uuid();
                        String caseName = rootNetwork.caseInfos().name();

                        Path caseDir = casesDir.resolve(oldCaseUuid.toString());
                        if (!Files.exists(caseDir) || !Files.isDirectory(caseDir)) {
                            LOGGER.error("Case directory not found or not a directory: {}", caseDir);
                            continue;
                        }

                        Path caseFile = caseDir.resolve(caseName);
                        if (!Files.exists(caseFile) || !Files.isRegularFile(caseFile)) {
                            LOGGER.error("Expected case file not found or not a regular file: {}", caseFile);
                            continue;
                        }

                        try {
                            LOGGER.info("Importing case file: {}", caseFile);
                            UUID newCaseUuid = caseService.importCaseFromFile(caseFile.toFile());
                            if (newCaseUuid == null) {
                                LOGGER.error("Case import returned null UUID for file: {}", caseFile);
                                throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import case: " + caseName);
                            }
                            caseUuidMapping.put(oldCaseUuid, newCaseUuid);
                            LOGGER.info("Successfully imported case {} with new UUID {}", caseName, newCaseUuid);
                        } catch (ExploreException e) {
                            throw e;
                        } catch (Exception e) {
                            LOGGER.error("Failed to import case file {}: {}", caseFile, e.getMessage(), e);
                            throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import case file: " + caseName);
                        }
                    }
                } else {
                    LOGGER.warn("Cases directory not found or not a directory: {}", casesDir);
                }

                // Verify all cases were imported successfully
                for (var rootNetwork : studyExportInfos.rootNetworks()) {
                    UUID oldCaseUuid = rootNetwork.caseInfos().uuid();
                    if (!caseUuidMapping.containsKey(oldCaseUuid)) {
                        throw new ExploreException(IMPORT_STUDY_FAILED,
                            "Failed to import case: " + rootNetwork.caseInfos().name());
                    }
                }

                // Get the first root network's new case UUID
                var firstRootNetwork = studyExportInfos.rootNetworks().get(0);
                UUID oldCaseUuid = firstRootNetwork.caseInfos().uuid();
                UUID newCaseUuid = caseUuidMapping.get(oldCaseUuid);

                // Create the study with the first root network using the correct root network name
                UUID createdStudyUuid = UUID.randomUUID();
                ElementAttributes elementAttributes = new ElementAttributes(createdStudyUuid, studyName, STUDY, userId, 0L, description);

                try {
                    // Insert study with the first root network name from export
                    studyService.insertStudyWithExistingCaseFile(
                            createdStudyUuid,
                            userId,
                            newCaseUuid,
                            firstRootNetwork.caseFormat(),
                            firstRootNetwork.importParameters(),
                            false, // Don't duplicate case
                            firstRootNetwork.name() // Use the name from the export (e.g., "n1")
                    );

                    // Create directory element
                    directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
                } catch (Exception e) {
                    // Rollback: delete the study if directory creation fails
                    try {
                        studyService.delete(createdStudyUuid, userId);
                    } catch (Exception cleanupException) {
                        LOGGER.error("Failed to cleanup study after error", cleanupException);
                    }
                    throw e;
                }

                LOGGER.info("Study creation initiated with UUID: {}", createdStudyUuid);

                // Wait for the study to be fully created (asynchronous case import processing)
                LOGGER.info("Waiting for study to be fully created in study-server...");
                waitForStudyCreation(createdStudyUuid, userId);
                LOGGER.info("Study creation confirmed");

                // Update the StudyExportInfos with new case UUIDs
                StudyExportInfos updatedExportInfos = updateCaseUuidsInExportInfos(studyExportInfos, caseUuidMapping);
                LOGGER.info("Updated export infos with {} root networks", updatedExportInfos.rootNetworks().size());

                // Call importStudy to import node tree and additional root networks
                LOGGER.info("Calling studyService.importStudy() to import node tree and additional root networks");
                try {
                    studyService.importStudy(createdStudyUuid, userId, updatedExportInfos);
                    LOGGER.info("studyService.importStudy() completed successfully");
                } catch (Exception e) {
                    LOGGER.error("Failed to import study tree and additional root networks", e);
                    throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import study tree: " + e.getMessage());
                }

                LOGGER.info("Successfully imported study {} with UUID {}", studyName, createdStudyUuid);

            } finally {
                deleteDirectory(tempDir);
            }

        } catch (ExploreException e) {
            LOGGER.error("Error importing study archive: {}", studyName, e);
            throw e;
        } catch (IOException e) {
            LOGGER.error("IO error importing study archive: {}", studyName, e);
            throw new ExploreException(IMPORT_STUDY_FAILED, "IO error while importing study: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error importing study archive: {}", studyName, e);
            throw new ExploreException(IMPORT_STUDY_FAILED, "Unexpected error while importing study: " + e.getMessage());
        }
    }

    /**
     * Extract zip archive to directory
     */
    private void extractArchive(InputStream inputStream, Path destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                // Prevent directory traversal attacks
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
     * Wait for study to be fully created in study-server (polling with timeout)
     */
    private void waitForStudyCreation(UUID studyUuid, String userId) {
        int maxAttempts = 60; // 60 seconds max
        int attemptDelayMs = 1000; // Check every second

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // Try to get study metadata - if it exists, the study is created
                List<Map<String, Object>> metadata = studyService.getMetadata(List.of(studyUuid));
                if (metadata != null && !metadata.isEmpty()) {
                    LOGGER.info("Study {} found after {} attempts", studyUuid, attempt + 1);
                    return;
                }
            } catch (Exception e) {
                // Study not found yet, wait and retry
                if (attempt < maxAttempts - 1) {
                    try {
                        Thread.sleep(attemptDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ExploreException(IMPORT_STUDY_FAILED, "Interrupted while waiting for study creation");
                    }
                } else {
                    LOGGER.error("Study {} not found after {} attempts", studyUuid, maxAttempts);
                    throw new ExploreException(IMPORT_STUDY_FAILED,
                            "Timeout waiting for study creation. Study may not have been created successfully.");
                }
            }
        }
    }

    /**
     * Update case UUIDs in StudyExportInfos with new imported case UUIDs
     */
    private StudyExportInfos updateCaseUuidsInExportInfos(StudyExportInfos original, Map<UUID, UUID> caseUuidMapping) {
        LOGGER.info("Updating case UUIDs for {} root networks", original.rootNetworks().size());
        List<RootNetworkExportInfos> updatedRootNetworks = original.rootNetworks().stream()
                .map(rootNetwork -> {
                    UUID oldCaseUuid = rootNetwork.caseInfos().uuid();
                    UUID newCaseUuid = caseUuidMapping.get(oldCaseUuid);
                    if (newCaseUuid == null) {
                        LOGGER.warn("No mapping found for case UUID {}, keeping original", oldCaseUuid);
                        return rootNetwork;
                    }
                    LOGGER.info("Mapping root network '{}': old case UUID {} -> new case UUID {}",
                            rootNetwork.name(), oldCaseUuid, newCaseUuid);
                    CaseExportInfos updatedCaseInfo = new CaseExportInfos(newCaseUuid, rootNetwork.caseInfos().name());
                    return new RootNetworkExportInfos(
                            rootNetwork.name(),
                            rootNetwork.tag(),
                            rootNetwork.caseFormat(),
                            updatedCaseInfo,
                            rootNetwork.importParameters()
                    );
                })
                .collect(Collectors.toList());

        LOGGER.info("Updated StudyExportInfos with {} root networks", updatedRootNetworks.size());
        return new StudyExportInfos(original.studyUuid(), updatedRootNetworks, original.nodeTree());
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete {}", path, e);
                        }
                    });
        }
    }
}
