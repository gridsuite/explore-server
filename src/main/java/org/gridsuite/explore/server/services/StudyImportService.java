/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.error.ExploreException;
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
    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;
    private final ExploreService exploreService;

    public StudyImportService(CaseService caseService, StudyService studyService, DirectoryService directoryService, ObjectMapper objectMapper, ExploreService exploreService) {
        this.caseService = caseService;
        this.studyService = studyService;
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
        this.exploreService = exploreService;
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
                var firstRootNetwork = studyExportInfos.rootNetworks().getFirst();
                UUID oldCaseUuid = firstRootNetwork.caseInfos().uuid();
                UUID newCaseUuid = caseUuidMapping.get(oldCaseUuid);

                // Update the StudyExportInfos with new case UUIDs
                StudyExportInfos updatedExportInfos = updateCaseUuidsInExportInfos(studyExportInfos, caseUuidMapping);
                LOGGER.info("Updated export infos with {} root networks", updatedExportInfos.rootNetworks().size());

                // Create study UUID
                UUID createdStudyUuid = UUID.randomUUID();

                try {
                    // Import the complete study using STUDY_IMPORT action (async via consumer)
                    // This will trigger the consumer which will:
                    // 1. Insert the study with the first root network
                    // 2. Create the directory element
                    // 3. Import the node tree and additional root networks
                    LOGGER.info("Importing study {} with {} root networks using STUDY_IMPORT action",
                            createdStudyUuid, updatedExportInfos.rootNetworks().size());

                    studyService.importStudyWithCaseImportAction(
                            createdStudyUuid,
                            userId,
                            newCaseUuid,
                            firstRootNetwork.caseFormat(),
                            firstRootNetwork.importParameters(),
                            updatedExportInfos,
                            studyName,
                            description,
                            parentDirectoryUuid);

                    LOGGER.info("Study import initiated for study {}", createdStudyUuid);
                } catch (Exception e) {
                    LOGGER.error("Failed to import study", e);
                    // Cleanup on error
                    try {
                        studyService.delete(createdStudyUuid, userId);
                    } catch (Exception cleanupException) {
                        LOGGER.error("Failed to cleanup study after error", cleanupException);
                    }
                    throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import study: " + e.getMessage());
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
