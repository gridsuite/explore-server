/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.function.Consumer;
import java.util.zip.ZipEntry;

import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.IMPORT_STUDY_FAILED;
import static org.gridsuite.explore.server.services.ExploreService.CASE;
import static org.gridsuite.explore.server.services.ExploreService.CONTINGENCY_LIST;
import static org.gridsuite.explore.server.services.ExploreService.FILTER;
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
    public static final String PARAMETERS_DIR = "computationParameters";
    public static final String FILTERS_FILE = "filters.json";
    public static final String CONTINGENCY_LISTS_FILE = "contingencyList.json";
    private static final String IDENTIFIERS_CONTINGENCY_LIST_TYPE = "IDENTIFIERS";
    private static final String FILTERS_CONTINGENCY_LIST_TYPE = "FILTERS";
    public static final String COMPUTATION_PARAMETERS = "computationParameters";
    public static final String APPLY_MODIFICATIONS = "applyModifications";
    public static final String JSON = ".json";
    public static final String TYPE = "type";
    private static final String VOLTAGE_INITIALIZATION = "VOLTAGE_INITIALIZATION";
    public static final String LOAD_FLOW = "LOAD_FLOW";
    public static final String SHORT_CIRCUIT = "SHORT_CIRCUIT";
    public static final String SECURITY_ANALYSIS = "SECURITY_ANALYSIS";
    public static final String SENSITIVITY_ANALYSIS = "SENSITIVITY_ANALYSIS";
    public static final String PCC_MIN = "PCC_MIN";
    // exported computation type -> study-server path used to set the study parameters
    private static final Map<String, String> COMPUTATION_TYPE_TO_STUDY_PATH = Map.of(
            LOAD_FLOW, "loadflow",
            SHORT_CIRCUIT, "short-circuit-analysis",
            VOLTAGE_INITIALIZATION, "voltage-init",
            SECURITY_ANALYSIS, "security-analysis",
            SENSITIVITY_ANALYSIS, "sensitivity-analysis",
            PCC_MIN, "pcc-min"
    );

    private final CaseService caseService;
    private final StudyService studyService;
    private final ObjectMapper objectMapper;
    private final ExploreService exploreService;
    private final DirectoryService directoryService;
    private final FilterService filterService;
    private final ContingencyListService contingencyListService;

    public StudyImportService(CaseService caseService, StudyService studyService, ObjectMapper objectMapper, ExploreService exploreService,
                              DirectoryService directoryService, FilterService filterService, ContingencyListService contingencyListService) {
        this.caseService = caseService;
        this.studyService = studyService;
        this.objectMapper = objectMapper;
        this.exploreService = exploreService;
        this.directoryService = directoryService;
        this.filterService = filterService;
        this.contingencyListService = contingencyListService;
    }

    /**
     * Import a study from an archive synchronously
     * @param archiveFile the zip archive file
     * @param studyName the name for the new study
     * @param description the description for the new study
     * @param parentDirectoryUuid the parent directory UUID
     */
    public void importStudy(MultipartFile archiveFile, String studyName, String description, UUID parentDirectoryUuid) {
        Path tempDir = null;
        try {
            tempDir = extractArchiveToDisk(archiveFile);

            TreeExportInfos treeExportInfos = objectMapper.readValue(tempDir.resolve(TREE_EXPORT_FILE).toFile(), TreeExportInfos.class);
            if (treeExportInfos.getRootNetworks() == null || treeExportInfos.getRootNetworks().isEmpty()) {
                throw new ExploreException(IMPORT_STUDY_FAILED, "No root networks found in archive");
            }
            createCases(treeExportInfos, tempDir.resolve(CASES_DIR), parentDirectoryUuid, description);

            UUID studyUuid = createStudy(treeExportInfos, studyName, parentDirectoryUuid, description);
            importComputationParameters(tempDir.resolve(PARAMETERS_DIR), parentDirectoryUuid, studyUuid, description);
        } catch (Exception e) {
            directoryService.deleteElement(parentDirectoryUuid);
            throw new ExploreException(IMPORT_STUDY_FAILED, "Error while importing study '" + studyName + "': " + e.getMessage(), e);
        } finally {
            try {
                if (tempDir != null && Files.exists(tempDir)) {
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

    private void createCases(TreeExportInfos treeExportInfos, Path casesDir, UUID parentDirectoryUuid, String description) {
        treeExportInfos.getRootNetworks().forEach(rootNetwork -> {
            CaseInfos caseInfos = rootNetwork.caseInfos();
            Path caseFile = casesDir.resolve(caseInfos.getCaseUuid().toString()).resolve(caseInfos.getCaseName()).normalize();
            UUID newCaseUuid = caseService.importFileCase(caseFile.toFile());
            ElementAttributes caseElementAttributes = new ElementAttributes(newCaseUuid, caseInfos.getCaseName(), CASE, 0L, description);
            exploreService.createDirectoryElementWithNewNameOrDeleteElement(caseElementAttributes, parentDirectoryUuid, caseService::delete);
            caseInfos.setCaseUuid(newCaseUuid);
        });
    }

    private UUID createStudy(TreeExportInfos treeExportInfos, String studyName, UUID parentDirectoryUuid, String description) {
        UUID createdStudyUuid = UUID.randomUUID();
        treeExportInfos.setStudyUuid(createdStudyUuid);
        ElementAttributes elementAttributes = new ElementAttributes(createdStudyUuid, studyName, STUDY, 0L, description, DirectoryElementStatus.CREATING);
        studyService.importStudy(treeExportInfos);
        exploreService.createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, studyService::delete);
        return createdStudyUuid;
    }

    private void importComputationParameters(Path parametersDir, UUID parentDirectoryUuid, UUID studyUuid, String description) throws IOException {
        Map<UUID, UUID> oldUuidsToNewUuids = new HashMap<>();
        importFilters(parametersDir, oldUuidsToNewUuids, parentDirectoryUuid, description);
        importContingencyLists(parametersDir, oldUuidsToNewUuids, parentDirectoryUuid, description);
        importComputationParametersFiles(parametersDir, studyUuid, oldUuidsToNewUuids);
    }

    private void importFilters(Path parametersDir, Map<UUID, UUID> oldUuidsToNewUuids, UUID parentDirectoryUuid, String description) {
        List<ExportedElementInfos> filters = readExportedElements(parametersDir.resolve(FILTERS_FILE));
        addNewUuids(filters, oldUuidsToNewUuids);
        for (ExportedElementInfos filter : filters) {
            UUID newFilterUuid = oldUuidsToNewUuids.get(filter.uuid());
            filterService.insertFilter(replaceWithNewUuids(filter.content().toString(), oldUuidsToNewUuids), newFilterUuid);
            createDirectoryElement(filter, newFilterUuid, FILTER, parentDirectoryUuid, description, filterService::delete);
        }
    }

    private void importContingencyLists(Path parametersDir, Map<UUID, UUID> oldUuidsToNewUuids, UUID parentDirectoryUuid, String description) {
        List<ExportedElementInfos> contingencyLists = readExportedElements(parametersDir.resolve(CONTINGENCY_LISTS_FILE));
        addNewUuids(contingencyLists, oldUuidsToNewUuids);
        for (ExportedElementInfos contingencyList : contingencyLists) {
            UUID newContingencyListUuid = oldUuidsToNewUuids.get(contingencyList.uuid());
            String remappedContent = replaceWithNewUuids(contingencyList.content().toString(), oldUuidsToNewUuids);
            switch (contingencyList.content().path(TYPE).asText()) {
                case IDENTIFIERS_CONTINGENCY_LIST_TYPE -> contingencyListService.insertIdentifierContingencyList(newContingencyListUuid, remappedContent);
                case FILTERS_CONTINGENCY_LIST_TYPE -> contingencyListService.insertFilterBasedContingencyList(newContingencyListUuid, remappedContent);
                default -> throw new ExploreException(IMPORT_STUDY_FAILED, "Unknown type for contingency list " + contingencyList.uuid());
            }
            createDirectoryElement(contingencyList, newContingencyListUuid, CONTINGENCY_LIST, parentDirectoryUuid, description, contingencyListService::delete);
        }
    }

    private void createDirectoryElement(ExportedElementInfos element, UUID newUuid, String type, UUID parentDirectoryUuid, String description, Consumer<UUID> rollback) {
        String name = Objects.requireNonNullElse(element.name(), element.uuid().toString());
        ElementAttributes elementAttributes = new ElementAttributes(newUuid, name, type, 0L, description);
        exploreService.createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, rollback);
    }

    private List<ExportedElementInfos> readExportedElements(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(file.toFile(), new TypeReference<List<ExportedElementInfos>>() { });
        } catch (IOException e) {
            throw new ExploreException(IMPORT_STUDY_FAILED, "Error while reading " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private void addNewUuids(List<ExportedElementInfos> elements, Map<UUID, UUID> oldUuidsToNewUuids) {
        for (ExportedElementInfos element : elements) {
            oldUuidsToNewUuids.put(element.uuid(), UUID.randomUUID());
        }
    }

    private void importComputationParametersFiles(Path parametersDir, UUID studyUuid, Map<UUID, UUID> oldUuidsToNewUuids) throws IOException {
        for (Map.Entry<String, String> computation : COMPUTATION_TYPE_TO_STUDY_PATH.entrySet()) {
            Path file = parametersDir.resolve(computation.getKey() + JSON);
            if (Files.exists(file)) {
                String parameters = replaceWithNewUuids(Files.readString(file), oldUuidsToNewUuids);
                if (VOLTAGE_INITIALIZATION.equals(computation.getKey())) {
                    // the study voltage init parameters wrap the computation parameters, applyModifications is not exported and keeps its default value
                    parameters = objectMapper.writeValueAsString(Map.of(COMPUTATION_PARAMETERS, objectMapper.readTree(parameters), APPLY_MODIFICATIONS, true));
                }
                studyService.setStudyParameters(studyUuid, computation.getValue(), parameters);
            }
        }
    }

    private String replaceWithNewUuids(String content, Map<UUID, UUID> oldUuidsToNewUuids) {
        String result = content;
        for (Map.Entry<UUID, UUID> entry : oldUuidsToNewUuids.entrySet()) {
            result = result.replace(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }
}
