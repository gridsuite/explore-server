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
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.ExploreException.Type.NOT_ALLOWED;

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

    public Mono<Void> createStudy(String studyName, UUID caseUuid, String description, String userId, UUID parentDirectoryUuid, Map<String, Object> importParams) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, null, userId, 0L, description);

        return studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), userId, caseUuid, importParams)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createStudy(String studyName, Mono<FilePart> caseFile, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY,
                null, userId, 0L, description);

        return studyService.insertStudyWithCaseFile(elementAttributes.getElementUuid(), userId, caseFile)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createStudy(UUID sourceStudyUuid, String studyName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY,
                null, userId, 0L, description);

        return studyService.insertStudy(sourceStudyUuid, elementAttributes.getElementUuid(), userId)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createCase(String caseName, Mono<FilePart> caseFile, String description, String userId, UUID parentDirectoryUuid) {
        return caseService.importCase(caseFile).doOnSuccess(uuid ->
            directoryService.createElement(new ElementAttributes(uuid, caseName, CASE, null, userId, 0L, description),
                parentDirectoryUuid, userId).subscribe()).then();
    }

    public Mono<Void> createCase(String caseName, String description, String userId, UUID sourceCaseUuid, UUID parentDirectoryUuid) {
        return caseService.createCase(sourceCaseUuid).doOnSuccess(uuid ->
                directoryService.createElement(new ElementAttributes(uuid, caseName, CASE,
                        null, userId, 0L, description), parentDirectoryUuid, userId).subscribe()).then();
    }

    public Mono<Void> createScriptContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);

        return contingencyListService.insertScriptContingencyList(elementAttributes.getElementUuid(), content)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createScriptContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);

        return contingencyListService.insertScriptContingencyList(sourceListId, elementAttributes.getElementUuid())
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createFormContingencyList(String listName, String content, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);

        return contingencyListService.insertFormContingencyList(elementAttributes.getElementUuid(), content)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createFormContingencyList(UUID sourceListId, String listName, String description, String userId, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST,
                null, userId, 0L, description);

        return contingencyListService.insertFormContingencyList(sourceListId, elementAttributes.getElementUuid())
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> newScriptFromFormContingencyList(UUID id, String scriptName, String userId, UUID parentDirectoryUuid) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!elementAttributes.getType().equals(CONTINGENCY_LIST)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                    CONTINGENCY_LIST, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0L, null);

            return contingencyListService.newScriptFromFormContingencyList(id, newElementAttributes.getElementUuid())
                    .doOnSuccess(unused -> directoryService.createElement(newElementAttributes, parentDirectoryUuid, userId).subscribe());
        });
    }

    public Mono<Void> replaceFormContingencyListWithScript(UUID id, String userId) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!userId.equals(elementAttributes.getOwner())) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            if (!elementAttributes.getType().equals(CONTINGENCY_LIST)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            return contingencyListService.replaceFormContingencyListWithScript(id).doOnSuccess(unused ->
                directoryService.notifyDirectoryChanged(id, userId).subscribe());
        });
    }

    public Mono<Void> createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);

        return filterService.insertFilter(filter, elementAttributes.getElementUuid(), userId)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> createFilter(String filterName, String description, UUID sourceFilterUuid, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER,
                null, userId, 0, description);

        return filterService.insertFilter(sourceFilterUuid, elementAttributes.getElementUuid(), userId)
                .doOnSuccess(unused -> directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).subscribe());
    }

    public Mono<Void> newScriptFromFilter(UUID filterId, String scriptName, String userId, UUID parentDirectoryUuid) {
        return directoryService.getElementInfos(filterId).flatMap(elementAttributes -> {
            if (!elementAttributes.getType().equals(FILTER)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            ElementAttributes newElementAttributes = new ElementAttributes(UUID.randomUUID(), scriptName,
                    FILTER, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0, null);

            return filterService.insertNewScriptFromFilter(filterId, newElementAttributes.getElementUuid())
                    .doOnSuccess(unused -> directoryService.createElement(newElementAttributes, parentDirectoryUuid,  userId).subscribe());
        });
    }

    public Mono<Void> replaceFilterWithScript(UUID id, String userId) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!userId.equals(elementAttributes.getOwner())) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            if (!elementAttributes.getType().equals(FILTER)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            return filterService.replaceFilterWithScript(id).doOnSuccess(unused ->
                directoryService.notifyDirectoryChanged(id, userId).subscribe());
        });
    }

    public Mono<Void> deleteElement(UUID id, String userId) {
        return directoryService.deleteElement(id, userId)
                .doOnSuccess(e -> directoryService.deleteDirectoryElement(id, userId).subscribe())
                // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
                .onErrorResume(e -> {
                    LOGGER.error(e.toString(), e);
                    return directoryService.deleteDirectoryElement(id, userId);
                });
    }
}
