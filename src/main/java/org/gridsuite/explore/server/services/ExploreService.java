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

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final DirectoryService directoryService;
    private final StudyService studyService;
    private final ContingencyListService contingencyListService;
    private final NetworkModificationService networkModificationService;
    private final FilterService filterService;
    private final CaseService caseService;
    private final ParametersService parametersService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);
    private final UserAdminService userAdminService;

    public ExploreService(
            DirectoryService directoryService,
            StudyService studyService,
            ContingencyListService contingencyListService,
            FilterService filterService,
            NetworkModificationService networkModificationService,
            CaseService caseService,
            ParametersService parametersService, UserAdminService userAdminService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
        this.networkModificationService = networkModificationService;
        this.caseService = caseService;
        this.parametersService = parametersService;
        this.userAdminService = userAdminService;
    }

    public void createStudy(String studyName, CaseInfo caseInfo, String description, String userId, UUID parentDirectoryUuid, Map<String, Object> importParams, Boolean duplicateCase) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, userId, 0L, description);
        studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), userId, caseInfo.caseUuid(), caseInfo.caseFormat(), importParams, duplicateCase);
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

    public void createScriptContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST, userId, 0L, description);
        contingencyListService.insertScriptContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateContingencyList(UUID contingencyListsId, UUID targetDirectoryId, String userId, ContingencyListType contingencyListType) {
        UUID newId = switch (contingencyListType) {
            case SCRIPT -> contingencyListService.duplicateScriptContingencyList(contingencyListsId);
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

    public void newScriptFromFormContingencyList(UUID id, String scriptName, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(id);
        if (!elementAttribute.getType().equals(CONTINGENCY_LIST)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                CONTINGENCY_LIST, userId, 0L, null);
        contingencyListService.newScriptFromFormContingencyList(id, newElementAttributes.getElementUuid());
        directoryService.createElement(newElementAttributes, parentDirectoryUuid, userId);
    }

    public void replaceFormContingencyListWithScript(UUID id, String userId) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(id);
        if (!userId.equals(elementAttribute.getOwner())) {
            throw new ExploreException(NOT_ALLOWED);
        }
        if (!elementAttribute.getType().equals(CONTINGENCY_LIST)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        contingencyListService.replaceFormContingencyListWithScript(id, userId);
        directoryService.notifyDirectoryChanged(id, userId);
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

    public void newScriptFromFilter(UUID filterId, String scriptName, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(filterId);
        if (!elementAttribute.getType().equals(FILTER)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                FILTER, userId, 0, null);
        filterService.insertNewScriptFromFilter(filterId, newElementAttributes.getElementUuid());
        directoryService.createElement(newElementAttributes, parentDirectoryUuid, userId);
    }

    public void replaceFilterWithScript(UUID id, String userId) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(id);
        if (!userId.equals(elementAttribute.getOwner())) {
            throw new ExploreException(NOT_ALLOWED);
        }
        if (!elementAttribute.getType().equals(FILTER)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        filterService.replaceFilterWithScript(id, userId);
        directoryService.notifyDirectoryChanged(id, userId);
    }

    public void deleteElement(UUID id, String userId) {
        // Verify if the user is allowed to delete the element.
        // FIXME: to be deleted when it's properly handled by the gateway
        directoryService.areDirectoryElementsDeletable(List.of(id), userId);
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

        // Verify if the user is allowed to delete the elements.
        // FIXME: to be deleted when it's properly handled by the gateway
        directoryService.areDirectoryElementsDeletable(uuids, userId);
        try {
            uuids.forEach(id -> directoryService.deleteElement(id, userId));
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        } finally {
            directoryService.deleteElementsFromDirectory(uuids, parentDirectoryUuids, userId);
        }
    }

    public void updateFilter(UUID id, String filter, String userId, String name) {
        filterService.updateFilter(id, filter, userId);
        updateElementName(id, name, userId);
    }

    public void updateContingencyList(UUID id, String content, String userId, String name, ContingencyListType contingencyListType) {
        contingencyListService.updateContingencyList(id, content, userId, getProperPath(contingencyListType));
        updateElementName(id, name, userId);
    }

    private void updateElementName(UUID id, String name, String userId) {
        // if the name is empty, no need to call directory-server
        if (StringUtils.isNotBlank(name)) {
            ElementAttributes elementAttributes = new ElementAttributes();
            elementAttributes.setElementName(name);
            directoryService.updateElement(id, elementAttributes, userId);
        }
    }

    private String getProperPath(ContingencyListType contingencyListType) {
        switch (contingencyListType) {
            case SCRIPT:
                return "/script-contingency-lists/{id}";
            case FORM:
                return "/form-contingency-lists/{id}";
            case IDENTIFIERS:
                return "/identifier-contingency-lists/{id}";
            default:
                throw new ExploreException(UNKNOWN_ELEMENT_TYPE);
        }
    }

    public void createParameters(String parameters, ParametersType parametersType, String parametersName, UUID parentDirectoryUuid, String userId) {
        UUID parametersUuid = parametersService.createParameters(parameters, parametersType);
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(), userId, 0, null);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void updateParameters(UUID id, String parameters, ParametersType parametersType, String userId, String name) {
        parametersService.updateParameters(id, parameters, parametersType);
        updateElementName(id, name, userId);
    }

    public void duplicateParameters(UUID sourceId, UUID targetDirectoryId, ParametersType parametersType, String userId) {
        UUID newParametersUuid = parametersService.duplicateParameters(sourceId, parametersType);
        directoryService.duplicateElement(sourceId, newParametersUuid, targetDirectoryId, userId);
    }

    public void createCompositeModifications(List<UUID> modificationUuids, String userId, String name,
                                           String description, UUID parentDirectoryUuid) {

        // create composite modifications
        UUID modificationsUuid = networkModificationService.createCompositeModifications(modificationUuids);
        ElementAttributes elementAttributes = new ElementAttributes(modificationsUuid, name, MODIFICATION,
                        userId, 0L, description);
        directoryService.createElementWithNewName(elementAttributes, parentDirectoryUuid, userId, true);
    }

    public void duplicateNetworkModifications(UUID sourceId, UUID parentDirectoryUuid, String userId) {
        // create duplicated modification
        Map<UUID, UUID> newModificationsUuids = networkModificationService.duplicateModifications(List.of(sourceId));
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
        }
    }

    public void updateElement(UUID id, ElementAttributes elementAttributes, String userId) {
        directoryService.updateElement(id, elementAttributes, userId);
        ElementAttributes elementsInfos = directoryService.getElementInfos(id);
        // send notification if the study name was updated
        notifyStudyUpdate(elementsInfos, userId);
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid, String userId) {
        directoryService.moveElementsDirectory(elementsUuids, targetDirectoryUuid, userId);
        //send notification to all studies
        List<ElementAttributes> elementsAttributes = directoryService.getElementsInfos(elementsUuids, null);
        elementsAttributes.forEach(elementAttributes -> notifyStudyUpdate(elementAttributes, userId));

    }

    private void notifyStudyUpdate(ElementAttributes element, String userId) {
        if (STUDY.equals(element.getType())) {
            studyService.notifyStudyUpdate(element.getElementUuid(), userId);
        }
    }

}
