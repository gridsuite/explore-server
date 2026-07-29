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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.IMPORT_STUDY_FAILED;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Service
public class StudyImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyImportService.class);

    private final CaseService caseService;
    private final StudyService studyService;
    private final ObjectMapper objectMapper;

    public StudyImportService(CaseService caseService, StudyService studyService, ObjectMapper objectMapper) {
        this.caseService = caseService;
        this.studyService = studyService;
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
    public void importStudyArchive(MultipartFile archiveFile, String studyName, String description, String userId, UUID parentDirectoryUuid) {
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
        Path studyJsonPath = tempDir.resolve("study.json");
        if (!Files.exists(studyJsonPath)) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "study.json not found in archive");
        }

        StudyExportInfos studyExportInfos = objectMapper.readValue(studyJsonPath.toFile(), StudyExportInfos.class);
        if (studyExportInfos.rootNetworks() == null || studyExportInfos.rootNetworks().isEmpty()) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "No root networks found in archive");
        }

        Map<UUID, UUID> caseUuidMapping = importCasesFromArchive(studyExportInfos, tempDir);
        checkAllCasesWereImported(studyExportInfos, caseUuidMapping);

        var firstRootNetwork = studyExportInfos.rootNetworks().getFirst();
        UUID newCaseUuid = caseUuidMapping.get(firstRootNetwork.caseInfos().uuid());
        StudyExportInfos updatedExportInfos = updateCaseUuidsInExportInfos(studyExportInfos, caseUuidMapping);
        LOGGER.info("Updated export infos with {} root networks", updatedExportInfos.rootNetworks().size());

        importStudyAndCleanupOnFailure(userId, parentDirectoryUuid, studyName, description, firstRootNetwork, newCaseUuid, updatedExportInfos);
    }

    private Map<UUID, UUID> importCasesFromArchive(StudyExportInfos studyExportInfos, Path tempDir) {
        Map<UUID, UUID> caseUuidMapping = new HashMap<>();
        Path casesDir = tempDir.resolve("cases");
        if (!Files.exists(casesDir) || !Files.isDirectory(casesDir)) {
            LOGGER.warn("Cases directory not found or not a directory: {}", casesDir);
            return caseUuidMapping;
        }

        LOGGER.info("Cases directory found: {}", casesDir);
        for (var rootNetwork : studyExportInfos.rootNetworks()) {
            importCaseForRootNetwork(rootNetwork, casesDir, caseUuidMapping);
        }
        return caseUuidMapping;
    }

    private void importCaseForRootNetwork(RootNetworkExportInfos rootNetwork, Path casesDir, Map<UUID, UUID> caseUuidMapping) {
        UUID oldCaseUuid = rootNetwork.caseInfos().uuid();
        String caseName = rootNetwork.caseInfos().name();
        Path caseDir = casesDir.resolve(oldCaseUuid.toString());

        if (!Files.exists(caseDir) || !Files.isDirectory(caseDir)) {
            LOGGER.error("Case directory not found or not a directory: {}", caseDir);
            return;
        }

        Path caseFile = caseDir.resolve(caseName);
        if (!Files.exists(caseFile) || !Files.isRegularFile(caseFile)) {
            LOGGER.error("Expected case file not found or not a regular file: {}", caseFile);
            return;
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

    private void checkAllCasesWereImported(StudyExportInfos studyExportInfos, Map<UUID, UUID> caseUuidMapping) {
        for (var rootNetwork : studyExportInfos.rootNetworks()) {
            UUID oldCaseUuid = rootNetwork.caseInfos().uuid();
            if (!caseUuidMapping.containsKey(oldCaseUuid)) {
                throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import case: " + rootNetwork.caseInfos().name());
            }
        }
    }

    private void importStudyAndCleanupOnFailure(String userId, UUID parentDirectoryUuid, String studyName, String description,
                                                RootNetworkExportInfos firstRootNetwork, UUID newCaseUuid, StudyExportInfos updatedExportInfos) {
        UUID createdStudyUuid = UUID.randomUUID();
        try {
            LOGGER.info("Importing study {} with {} root networks using STUDY_IMPORT action",
                    createdStudyUuid, updatedExportInfos.rootNetworks().size());
            studyService.importStudyWithCaseImportAction(createdStudyUuid, userId, newCaseUuid, firstRootNetwork.caseFormat(),
                    firstRootNetwork.importParameters(), updatedExportInfos, studyName, description, parentDirectoryUuid);
            LOGGER.info("Study import initiated for study {}", createdStudyUuid);
        } catch (Exception e) {
            try {
                studyService.delete(createdStudyUuid, userId);
            } catch (Exception cleanupException) {
                LOGGER.error("Failed to cleanup study after error", cleanupException);
            }
            throw new ExploreException(IMPORT_STUDY_FAILED, "Failed to import study: " + e.getMessage());
        }
    }

    /**
     * Extract zip archive to directory
     */
    private void extractArchive(InputStream inputStream, Path destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
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
                .toList();

        LOGGER.info("Updated StudyExportInfos with {} root networks", updatedRootNetworks.size());
        return new StudyExportInfos(original.studyUuid(), updatedRootNetworks, original.nodeTree());
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
