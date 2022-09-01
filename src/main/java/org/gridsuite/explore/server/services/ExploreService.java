/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;

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
    static final String DIRECTORY = "DIRECTORY";

    private DirectoryService directoryService;
    private StudyService studyService;
    private ContingencyListService contingencyListService;
    private FilterService filterService;
    private CaseService caseService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);

    public ExploreService(
            DirectoryService directoryService,
            StudyService studyService,
            ContingencyListService contingencyListService,
            FilterService filterService,
            CaseService caseService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
        this.caseService = caseService;
    }

    public void createStudy(String studyName, UUID caseUuid, String description, String userId, UUID parentDirectoryUuid, Map<String, Object> importParams) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, null, userId, 0L, description);
        try {
            studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), userId, caseUuid, importParams);
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (HttpStatusCodeException e) {
            throw new ExploreException(INSERT_STUDY_FAILED);
        }
    }

    public void createStudy(String studyName, MultipartFile caseFile, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY,
                null, userId, 0L, description);

        studyService.insertStudyWithCaseFile(elementAttributes.getElementUuid(), userId, caseFile);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);

    }

    public void createStudy(UUID sourceStudyUuid, String studyName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY,
                null, userId, 0L, description);

        studyService.insertStudy(sourceStudyUuid, elementAttributes.getElementUuid(), userId);
        directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
    }

    public void createCase(String caseName, MultipartFile caseFile, String description, String userId, UUID parentDirectoryUuid) {

        UUID uuid = caseService.importCase(caseFile);
        directoryService.createElement(new ElementAttributes(uuid, caseName, CASE, null, userId, 0L, description),
                parentDirectoryUuid, userId);
    }

    public void createCase(String caseName, String description, String userId, UUID sourceCaseUuid, UUID parentDirectoryUuid) {
        try {
            UUID uuid = caseService.createCase(sourceCaseUuid);
            directoryService.createElement(new ElementAttributes(uuid, caseName, CASE,
                    null, userId, 0L, description), parentDirectoryUuid, userId);
        } catch (HttpStatusCodeException e) {
            throw new ExploreException(INSERT_STUDY_FAILED);
        }
    }

    public void createScriptContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        try {
            contingencyListService.insertScriptContingencyList(elementAttributes.getElementUuid(), content);
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_CONTINGENCY_LIST_FAILED);
        }
    }

    public void createScriptContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        try {
            contingencyListService.insertScriptContingencyList(sourceListId, elementAttributes.getElementUuid());
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_CONTINGENCY_LIST_FAILED);
        }
    }

    public void createFormContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        try {
            contingencyListService.insertFormContingencyList(elementAttributes.getElementUuid(), content);
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_CONTINGENCY_LIST_FAILED);
        }
    }

    public void createFormContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);
        try {
            contingencyListService.insertFormContingencyList(sourceListId, elementAttributes.getElementUuid());
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_CONTINGENCY_LIST_FAILED);
        }
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
        contingencyListService.replaceFormContingencyListWithScript(id);
        directoryService.notifyDirectoryChanged(id, userId);
    }

    public void createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);
        try {
            filterService.insertFilter(filter, elementAttributes.getElementUuid(), userId);
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_FILTER_FAILED);
        }
    }

    public void createFilter(String filterName, String description, UUID sourceFilterUuid, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);
        try {
            filterService.insertFilter(sourceFilterUuid, elementAttributes.getElementUuid(), userId);
            directoryService.createElement(elementAttributes, parentDirectoryUuid, userId);
        } catch (Exception e) {
            throw new ExploreException(CREATE_FILTER_FAILED);
        }
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
        filterService.replaceFilterWithScript(id);
        directoryService.notifyDirectoryChanged(id, userId);
    }

    public void deleteElement(UUID id, String userId) {
        try {
            directoryService.deleteElement(id, userId);
            directoryService.deleteDirectoryElement(id, userId);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            directoryService.deleteDirectoryElement(id, userId);
        }
    }
}
