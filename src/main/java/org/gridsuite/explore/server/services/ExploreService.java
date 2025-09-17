/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.apache.commons.lang3.StringUtils;
import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Stream;

import static org.gridsuite.explore.server.ExploreException.Type.*;


/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Etienne Homer <jacques.borsenberger at rte-france.com>
 */

@Service
public class ExploreService {
    static final String STUDY = "STUDY";
    static final String CASE = "CASE";
    static final String CONTINGENCY_LIST = "CONTINGENCY_LIST";
    static final String FILTER = "FILTER";
    static final String MODIFICATION = "MODIFICATION";
    static final String DIRECTORY = "DIRECTORY";
    static final String SPREADSHEET_CONFIG = "SPREADSHEET_CONFIG";
    static final String SPREADSHEET_CONFIG_COLLECTION = "SPREADSHEET_CONFIG_COLLECTION";
    static final String DIAGRAM_CONFIG = "DIAGRAM_CONFIG";

    private final DirectoryService directoryService;
    private final StudyService studyService;
    private final ContingencyListService contingencyListService;
    private final NetworkModificationService networkModificationService;
    private final FilterService filterService;
    private final CaseService caseService;
    private final ParametersService parametersService;
    private final SpreadsheetConfigService spreadsheetConfigService;
    private final SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;
    private final UserIdentityService userIdentityService;
    private final NotificationService notificationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);
    private final UserAdminService userAdminService;
    private final SingleLineDiagramService singleLineDiagramService;

    public ExploreService(
        DirectoryService directoryService,
        StudyService studyService,
        ContingencyListService contingencyListService,
        FilterService filterService,
        NetworkModificationService networkModificationService,
        CaseService caseService,
        ParametersService parametersService,
        UserAdminService userAdminService,
        SpreadsheetConfigService spreadsheetConfigService,
        SpreadsheetConfigCollectionService spreadsheetConfigCollectionService,
        UserIdentityService userIdentityService,
        NotificationService notificationService,
        SingleLineDiagramService singleLineDiagramService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
        this.networkModificationService = networkModificationService;
        this.caseService = caseService;
        this.parametersService = parametersService;
        this.userAdminService = userAdminService;
        this.spreadsheetConfigService = spreadsheetConfigService;
        this.spreadsheetConfigCollectionService = spreadsheetConfigCollectionService;
        this.userIdentityService = userIdentityService;
        this.notificationService = notificationService;
        this.singleLineDiagramService = singleLineDiagramService;
    }

    public void createStudy(String studyName, CaseInfo caseInfo, String description, String userId, UUID parentDirectoryUuid, Map<String, Object> importParams, Boolean duplicateCase) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, userId, 0L, description);
        // Two scenarios to handle.
        // Scenario 1: the study is created from an existing case, so the case is available in the directory server.
        // Scenario 2: the study is not created from an existing case, in which case the directory throws exception because no element with the given uuid.
        Optional<ElementAttributes> caseAttributes = directoryService.getElementInfos(caseInfo.caseUuid());
        String elementName = caseAttributes.map(ElementAttributes::getElementName).orElse(null);
        studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), userId, caseInfo.caseUuid(), caseInfo.caseFormat(), importParams, duplicateCase, elementName);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateStudy(UUID sourceStudyUuid, UUID targetDirectoryId, String userId) {
        UUID newStudyId = studyService.duplicateStudy(sourceStudyUuid, userId);
        directoryService.duplicateElement(sourceStudyUuid, newStudyId, targetDirectoryId, userId);
    }

    public void createCase(String caseName, MultipartFile caseFile, String description, String userId, UUID parentDirectoryUuid) {
        UUID uuid = caseService.importCase(caseFile);
        directoryService.createElement(new ElementAttributes(uuid, caseName, CASE, userId, 0L, description),
                parentDirectoryUuid, userId);
    }

    public void duplicateCase(UUID sourceCaseUuid, UUID targetDirectoryId, String userId) {
        UUID newCaseId = caseService.duplicateCase(sourceCaseUuid);
        directoryService.duplicateElement(sourceCaseUuid, newCaseId, targetDirectoryId, userId);
    }

    public void duplicateContingencyList(UUID contingencyListsId, UUID targetDirectoryId, String userId, ContingencyListType contingencyListType) {
        UUID newId = switch (contingencyListType) {
            case FORM -> contingencyListService.duplicateFormContingencyList(contingencyListsId);
            case IDENTIFIERS -> contingencyListService.duplicateIdentifierContingencyList(contingencyListsId);
        };
        directoryService.duplicateElement(contingencyListsId, newId, targetDirectoryId, userId);
    }

    public void createFormContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST, userId, 0L, description);
        contingencyListService.insertFormContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createIdentifierContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST, userId, 0L, description);
        contingencyListService.insertIdentifierContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER, userId, 0, description);
        filterService.insertFilter(filter, elementAttributes.getElementUuid(), userId);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateFilter(UUID sourceFilterId, UUID targetDirectoryId, String userId) {
        UUID newFilterId = filterService.duplicateFilter(sourceFilterId);
        directoryService.duplicateElement(sourceFilterId, newFilterId, targetDirectoryId, userId);
    }

    public void deleteElement(UUID id, String userId) {
        try {
            directoryService.deleteElement(id, userId);
            directoryService.deleteDirectoryElement(id, userId);
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            directoryService.deleteDirectoryElement(id, userId);
        }
    }

    public void deleteElementsFromDirectory(List<UUID> uuids, UUID parentDirectoryUuids, String userId) {

        try {
            uuids.forEach(id -> directoryService.deleteElement(id, userId));
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        } finally {
            directoryService.deleteElementsFromDirectory(uuids, parentDirectoryUuids, userId);
        }
    }

    public void updateFilter(UUID id, String filter, String userId, String name, String description) {
        // check if the  user have the right to update the filter
        filterService.updateFilter(id, filter, userId);

        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes, userId);
    }

    public void updateContingencyList(UUID id, String content, String userId, String name, String description, ContingencyListType contingencyListType) {
        // check if the  user have the right to update the contingency
        contingencyListService.updateContingencyList(id, content, userId, getProperPath(contingencyListType));
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes, userId);
    }

    public void updateCompositeModification(UUID id, List<UUID> modificationUuids, String userId, String name, String description) {
        networkModificationService.updateCompositeModification(id, modificationUuids);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        return networkModificationService.getCompositeModificationContent(compositeModificationId);
    }

    private void updateElementNameAndDescription(UUID id, String name, String description, String userId) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(name);
        elementAttributes.setDescription(description);
        directoryService.updateElement(id, elementAttributes, userId);
    }

    private String getProperPath(ContingencyListType contingencyListType) {
        return switch (contingencyListType) {
            case FORM -> "/form-contingency-lists/{id}";
            case IDENTIFIERS -> "/identifier-contingency-lists/{id}";
        };
    }

    public void createParameters(String parameters, ParametersType parametersType, String parametersName, String description, UUID parentDirectoryUuid, String userId) {
        UUID parametersUuid = parametersService.createParameters(parameters, parametersType);
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(), userId, 0, description);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void updateParameters(UUID id, String parameters, ParametersType parametersType, String userId, String name, String description) {
        parametersService.updateParameters(id, parameters, parametersType);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public void duplicateParameters(UUID sourceId, UUID targetDirectoryId, ParametersType parametersType, String userId) {
        UUID newParametersUuid = parametersService.duplicateParameters(sourceId, parametersType);
        directoryService.duplicateElement(sourceId, newParametersUuid, targetDirectoryId, userId);
    }

    public void createDiagramConfig(String diagramConfig, String diagramConfigName, String description, UUID parentDirectoryUuid, String userId) {
        UUID diagramConfigUuid = singleLineDiagramService.createDiagramConfig(diagramConfig);
        ElementAttributes elementAttributes = new ElementAttributes(diagramConfigUuid, diagramConfigName, DIAGRAM_CONFIG, userId, 0, description);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateDiagramConfig(UUID sourceId, UUID targetDirectoryId, String userId) {
        UUID newConfigUuid = singleLineDiagramService.duplicateDiagramConfig(sourceId);
        directoryService.duplicateElement(sourceId, newConfigUuid, targetDirectoryId, userId);
    }

    public void updateDiagramConfig(UUID id, String diagramConfig, String userId, String name, String description) {
        singleLineDiagramService.updateDiagramConfig(id, diagramConfig);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public void createSpreadsheetConfig(String spreadsheetConfigDto, String configName, String description, UUID parentDirectoryUuid, String userId) {
        UUID spreadsheetConfigUuid = spreadsheetConfigService.createSpreadsheetConfig(spreadsheetConfigDto);
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, configName, SPREADSHEET_CONFIG, userId, 0, description);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createSpreadsheetConfigCollection(String spreadsheetConfigCollectionDto, String collectionName, String description, UUID parentDirectoryUuid, String userId) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionService.createSpreadsheetConfigCollection(spreadsheetConfigCollectionDto);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid, userId);
    }

    public void createSpreadsheetConfigCollectionFromConfigIds(List<UUID> configIds, String collectionName, String description, UUID parentDirectoryUuid, String userId) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionService.createSpreadsheetConfigCollectionFromConfigIds(configIds);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid, userId);
    }

    private void createSpreadsheetConfigCollectionElement(UUID spreadsheetConfigUuid, String collectionName, String description, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, collectionName, SPREADSHEET_CONFIG_COLLECTION, userId, 0, description);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void updateSpreadsheetConfig(UUID id, String spreadsheetConfigDto, String userId, String name, String description) {
        spreadsheetConfigService.updateSpreadsheetConfig(id, spreadsheetConfigDto);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public void updateSpreadsheetConfigCollection(UUID id, String spreadsheetConfigCollectionDto, String userId, String name, String description) {
        spreadsheetConfigCollectionService.updateSpreadsheetConfigCollection(id, spreadsheetConfigCollectionDto);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public void replaceAllSpreadsheetConfigsInCollection(UUID id, List<UUID> configIds, String userId, String name, String description) {
        spreadsheetConfigCollectionService.replaceAllSpreadsheetConfigsInCollection(id, configIds);
        updateElementNameAndDescription(id, name, description, userId);
    }

    public void duplicateSpreadsheetConfig(UUID sourceId, UUID targetDirectoryId, String userId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigService.duplicateSpreadsheetConfig(sourceId);
        directoryService.duplicateElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, userId);
    }

    public void duplicateSpreadsheetConfigCollection(UUID sourceId, UUID targetDirectoryId, String userId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigCollectionService.duplicateSpreadsheetConfigCollection(sourceId);
        directoryService.duplicateElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, userId);
    }

    public void createCompositeModification(List<UUID> modificationUuids, String userId, String name,
                                            String description, UUID parentDirectoryUuid) {

        // create composite modifications
        UUID modificationsUuid = networkModificationService.createCompositeModification(modificationUuids);
        ElementAttributes elementAttributes = new ElementAttributes(modificationsUuid, name, MODIFICATION,
                        userId, 0L, description);
        directoryService.createElementWithNewName(elementAttributes, parentDirectoryUuid, userId, true);
    }

    public void duplicateCompositeModification(UUID sourceId, UUID parentDirectoryUuid, String userId) {
        // create duplicated modification
        Map<UUID, UUID> newModificationsUuids = networkModificationService.duplicateCompositeModifications(List.of(sourceId));
        UUID newNetworkModification = newModificationsUuids.get(sourceId);
        // create corresponding directory element
        directoryService.duplicateElement(sourceId, newNetworkModification, parentDirectoryUuid, userId);
    }

    public void assertCanCreateCase(String userId) {
        Integer userMaxAllowedStudiesAndCases = userAdminService.getUserMaxAllowedCases(userId);
        if (userMaxAllowedStudiesAndCases != null) {
            int userCasesCount = directoryService.getUserCasesCount(userId);
            if (userCasesCount >= userMaxAllowedStudiesAndCases) {
                throw new ExploreException(MAX_ELEMENTS_EXCEEDED, "max allowed cases : " + userMaxAllowedStudiesAndCases);
            }
            notifyCasesThresholdReached(userCasesCount, userMaxAllowedStudiesAndCases, userId);
        }
    }

    public void notifyCasesThresholdReached(int userCasesCount, int userMaxAllowedStudiesAndCases, String userId) {
        Integer casesAlertThreshold = userAdminService.getCasesAlertThreshold();
        if (casesAlertThreshold != null) {
            int userCasesUsagePercentage = (100 * userCasesCount) / userMaxAllowedStudiesAndCases;
            if (userCasesUsagePercentage >= casesAlertThreshold) {
                CaseAlertThresholdMessage caseAlertThresholdMessage = new CaseAlertThresholdMessage(userCasesUsagePercentage, userCasesCount);
                notificationService.emitUserMessage(userId, "casesAlertThreshold", caseAlertThresholdMessage);
            }
        }
    }

    public void updateElement(UUID id, ElementAttributes elementAttributes, String userId) {
        // The check to know if the  user have the right to update the element is done in the directory-server
        directoryService.updateElement(id, elementAttributes, userId);
        ElementAttributes elementsInfos = directoryService.getElementInfos(id).orElseThrow(() -> new ExploreException(NOT_FOUND));
        // send notification if the study name was updated
        notifyStudyUpdate(elementsInfos, userId);
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid, String userId) {
        directoryService.moveElementsDirectory(elementsUuids, targetDirectoryUuid, userId);
        //send notification to all studies
        List<ElementAttributes> elementsAttributes = directoryService.getElementsInfos(elementsUuids, null, userId);
        elementsAttributes.forEach(elementAttributes -> notifyStudyUpdate(elementAttributes, userId));

    }

    private void notifyStudyUpdate(ElementAttributes element, String userId) {
        if (STUDY.equals(element.getType())) {
            studyService.notifyStudyUpdate(element.getElementUuid(), userId);
        }
    }

    public String getUsersIdentities(List<UUID> elementsUuids, String userId) {
        // this returns names for owner and lastmodifiedby,
        // if we need it in the future, we can do separate requests.
        List<String> subs = directoryService.getElementsInfos(elementsUuids, null, userId).stream()
                .flatMap(x -> Stream.of(x.getOwner(), x.getLastModifiedBy())).distinct().filter(Objects::nonNull).toList();
        return userIdentityService.getUsersIdentities(subs);
    }
}
