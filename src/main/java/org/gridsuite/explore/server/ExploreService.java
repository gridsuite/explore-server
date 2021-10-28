/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.gridsuite.explore.server.ExploreException.Type.NOT_ALLOWED;

/**
 * @author Etienne Homer <etienne.homre at rte-france.com>
 */
@Service
class ExploreService {
    static final String HEADER_USER_ID = "userId";
    static final String STUDY = "STUDY";
    static final String CONTINGENCY_LIST = "CONTINGENCY_LIST";
    static final String FILTER = "FILTER";

    private DirectoryService directoryService;
    private StudyService studyService;
    private ContingencyListService contingencyListService;
    private FilterService filterService;

    public ExploreService(
            DirectoryService directoryService,
            StudyService studyService,
            ContingencyListService contingencyListService,
            FilterService filterService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
    }

    public Mono<Void> createStudy(String studyName, UUID caseUuid, String description, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, studyName, STUDY,
                new AccessRightsAttributes(isPrivate), userId, 0L);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        studyService.insertStudyWithExistingCaseFile(elementAttributes1.getElementUuid(), studyName, description, userId, isPrivate, caseUuid)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> createStudy(String studyName, Mono<FilePart> caseFile, String description, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, studyName, STUDY,
                new AccessRightsAttributes(isPrivate), userId, 0L);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        studyService.insertStudyWithCaseFile(elementAttributes1.getElementUuid(), studyName, description, userId, isPrivate, caseFile)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> createScriptContingencyList(String listName, String content, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, listName, CONTINGENCY_LIST,
                new AccessRightsAttributes(isPrivate), userId, 0L);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        contingencyListService.insertScriptContingencyList(elementAttributes1.getElementUuid(), content)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId)
                                )
        );
    }

    public Mono<Void> createFiltersContingencyList(String listName, String content, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, listName, CONTINGENCY_LIST,
                new AccessRightsAttributes(isPrivate), userId, 0L);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        contingencyListService.insertFiltersContingencyList(elementAttributes1.getElementUuid(), content)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId)
                                )
        );
    }

    public Mono<Void> newScriptFromFiltersContingencyList(UUID id, String scriptName, String userId, UUID parentDirectoryUuid) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            ElementAttributes newElementAttributes = new ElementAttributes(null, scriptName,
                    CONTINGENCY_LIST, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0L);
            return directoryService.createElement(newElementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                            contingencyListService.newScriptFromFiltersContingencyList(id, scriptName, elementAttributes1.getElementUuid())
                                    .doOnError(err ->
                                        directoryService.deleteElement(elementAttributes1.getElementUuid(), userId)
                                    )
            );
        });
    }

    public Mono<Void> replaceFilterContingencyListWithScript(UUID id, String userId) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!userId.equals(elementAttributes.getOwner())) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            return contingencyListService.replaceFilterContingencyListWithScript(id);
        });
    }

    public Mono<Void> createFilter(String filter, String filterName, String filterType, Boolean isPrivate, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(null, filterName, FILTER,
                new AccessRightsAttributes(isPrivate), userId, 0);

        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        filterService.insertFilter(filter, elementAttributes1.getElementUuid(), userId)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> newScriptFromFilter(UUID filterId, String scriptName, String userId, UUID parentDirectoryUuid) {
        return directoryService.getElementInfos(filterId).flatMap(elementAttributes -> {
            if (!elementAttributes.getType().equals(FILTER)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            ElementAttributes newElementAttributes = new ElementAttributes(null, scriptName,
                    FILTER, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0);
            return directoryService.createElement(newElementAttributes, parentDirectoryUuid,  userId).flatMap(elementAttributes1 ->
                filterService.insertNewScriptFromFilter(filterId, scriptName, elementAttributes1.getElementUuid())
                    .doOnError(err ->
                        directoryService.deleteElement(elementAttributes1.getElementUuid(), userId)
                    )
            );
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
            return filterService.replaceFilterWithScript(id)
                    .doOnSuccess(unused ->
                        directoryService.updateElementType(id, FILTER, userId)
                    );
        });
    }

    public Mono<Void> deleteElement(UUID id, String userId) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (elementAttributes.getType().equals(STUDY)) {
                studyService.deleteFromStudyServer(elementAttributes.getElementUuid(), userId).subscribe();
            } else if (elementAttributes.getType().equals(CONTINGENCY_LIST)) {
                contingencyListService.deleteContingencyList(elementAttributes.getElementUuid()).subscribe();
            } else if (elementAttributes.getType().equals(FILTER)) {
                filterService.deleteFilter(elementAttributes.getElementUuid()).subscribe();
            }
            return directoryService.deleteElement(id, userId);
        });
    }

    public Mono<Void> setAccessRights(UUID elementUuid, boolean isPrivate, String userId) {
        return directoryService.getElementInfos(elementUuid).flatMap(elementAttributes -> {
            if (elementAttributes.getType().equals(STUDY)) {
                studyService.setStudyAccessRight(elementUuid, userId, isPrivate);
            }
            return directoryService.setAccessRights(elementUuid, isPrivate, userId);
        });
    }
}
