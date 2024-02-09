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

import static org.gridsuite.explore.server.ExploreException.Type.NOT_ALLOWED;
import static org.gridsuite.explore.server.ExploreException.Type.UNKNOWN_ELEMENT_TYPE;


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

    private DirectoryService directoryService;
    private StudyService studyService;
    private ContingencyListService contingencyListService;
    private NetworkModificationService networkModificationService;
    private FilterService filterService;
    private CaseService caseService;
    private ParametersService parametersService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);

    public ExploreService(
        DirectoryService directoryService,
        StudyService studyService,
        ContingencyListService contingencyListService,
        FilterService filterService,
        NetworkModificationService networkModificationService,
        CaseService caseService,
        ParametersService parametersService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
        this.networkModificationService = networkModificationService;
        this.caseService = caseService;
        this.parametersService = parametersService;
    }

    public void createStudy(String studyName, UUID caseUuid, String description, String userId, UUID parentDirectoryUuid, Map<String, Object> importParams, Boolean duplicateCase) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, null, userId, 0L, description);
        studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), userId, caseUuid, importParams, duplicateCase);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateStudy(UUID sourceStudyUuid, String studyName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY,
                null, userId, 0L, description);
        studyService.duplicateStudy(sourceStudyUuid, elementAttributes.getElementUuid(), userId);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createCase(String caseName, MultipartFile caseFile, String description, String userId, UUID parentDirectoryUuid) {
        UUID uuid = caseService.importCase(caseFile);
        directoryService.createElement(new ElementAttributes(uuid, caseName, CASE, null, userId, 0L, description),
                parentDirectoryUuid, userId);
    }

    public void duplicateCase(String caseName, String description, String userId, UUID sourceCaseUuid, UUID parentDirectoryUuid) {
        UUID uuid = caseService.duplicateCase(sourceCaseUuid);
        directoryService.createElement(new ElementAttributes(uuid, caseName, CASE,
                null, userId, 0L, description), parentDirectoryUuid, userId);
    }

    public void createScriptContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertScriptContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateScriptContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertScriptContingencyList(sourceListId, elementAttributes.getElementUuid());
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createFormContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertFormContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateFormContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertFormContingencyList(sourceListId, elementAttributes.getElementUuid());
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateIdentifierContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertIdentifierContingencyList(sourceListId, elementAttributes.getElementUuid());
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void newScriptFromFormContingencyList(UUID id, String scriptName, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(id);
        if (!elementAttribute.getType().equals(CONTINGENCY_LIST)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                CONTINGENCY_LIST, new AccessRightsAttributes(elementAttribute.getAccessRights().isPrivate()), userId, 0L, null);
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
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        contingencyListService.insertIdentifierContingencyList(elementAttributes.getElementUuid(), content);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);
        filterService.insertFilter(filter, elementAttributes.getElementUuid(), userId);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void duplicateFilter(String filterName, String description, UUID sourceFilterUuid, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);
        filterService.insertFilter(sourceFilterUuid, elementAttributes.getElementUuid(), userId);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void newScriptFromFilter(UUID filterId, String scriptName, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttribute = directoryService.getElementInfos(filterId);
        if (!elementAttribute.getType().equals(FILTER)) {
            throw new ExploreException(NOT_ALLOWED);
        }
        ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                FILTER, new AccessRightsAttributes(elementAttribute.getAccessRights().isPrivate()), userId, 0, null);
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
        try {
            directoryService.deleteElement(id, userId);
            directoryService.deleteDirectoryElement(id, userId);
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            directoryService.deleteDirectoryElement(id, userId);
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
        /** if the name is empty, no need to call directory-server */
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
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(),
                null, userId, 0, null);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void updateParameters(UUID id, String parameters, ParametersType parametersType, String userId, String name) {
        parametersService.updateParameters(id, parameters, parametersType);
        updateElementName(id, name, userId);
    }

    public void duplicateParameters(UUID parentParameterId, ParametersType parametersType, String parametersName, UUID parentDirectoryUuid, String userId) {
        UUID parametersUuid = parametersService.createParameters(parentParameterId, parametersType);
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(),
            null, userId, 0L, null);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createNetworkModifications(List<ElementAttributes> modificationAttributesList, String userId, UUID parentDirectoryUuid) {
        List<UUID> existingModificationsUuids = modificationAttributesList.stream()
                .map(ElementAttributes::getElementUuid)
                .toList();

        // create all duplicated modifications
        Map<UUID, UUID> newModificationsUuids = networkModificationService.createModifications(existingModificationsUuids);

        // create all corresponding directory elements
        modificationAttributesList.forEach(m -> {
            final UUID newId = newModificationsUuids.get(m.getElementUuid());
            ElementAttributes elementAttributes = new ElementAttributes(newId, m.getElementName(), MODIFICATION,
                    null, userId, 0L, m.getDescription());
            directoryService.createElementWithNewName(elementAttributes, parentDirectoryUuid, userId, true);
        });
    }
}
