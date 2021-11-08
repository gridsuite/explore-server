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
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                new AccessRightsAttributes(isPrivate), userId, 0L, description);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        studyService.insertStudyWithExistingCaseFile(elementAttributes1.getElementUuid(), userId, isPrivate, caseUuid)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> createStudy(String studyName, Mono<FilePart> caseFile, String description, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, studyName, STUDY,
                new AccessRightsAttributes(isPrivate), userId, 0L, description);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        studyService.insertStudyWithCaseFile(elementAttributes1.getElementUuid(), userId, isPrivate, caseFile)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> createScriptContingencyList(String listName, String content, String description, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, listName, CONTINGENCY_LIST,
                new AccessRightsAttributes(isPrivate), userId, 0L, description);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        contingencyListService.insertScriptContingencyList(elementAttributes1.getElementUuid(), content)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> createFiltersContingencyList(String listName, String content, String description, String userId, Boolean isPrivate, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(null, listName, CONTINGENCY_LIST,
                new AccessRightsAttributes(isPrivate), userId, 0L, description);
        return directoryService.createElement(elementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                        contingencyListService.insertFiltersContingencyList(elementAttributes1.getElementUuid(), content)
                                .doOnError(err ->
                                    directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                )
        );
    }

    public Mono<Void> newScriptFromFiltersContingencyList(UUID id, String scriptName, String userId, UUID parentDirectoryUuid) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!elementAttributes.getType().equals(CONTINGENCY_LIST)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            ElementAttributes newElementAttributes = new ElementAttributes(null, scriptName,
                    CONTINGENCY_LIST, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0L, null);
            return directoryService.createElement(newElementAttributes, parentDirectoryUuid, userId).flatMap(elementAttributes1 ->
                            contingencyListService.newScriptFromFiltersContingencyList(id, elementAttributes1.getElementUuid())
                                    .doOnError(err ->
                                        directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
                                    )
            );
        });
    }

    public Mono<Void> replaceFilterContingencyListWithScript(UUID id, String userId) {
        return directoryService.getElementInfos(id).flatMap(elementAttributes -> {
            if (!userId.equals(elementAttributes.getOwner())) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            if (!elementAttributes.getType().equals(CONTINGENCY_LIST)) {
                return Mono.error(new ExploreException(NOT_ALLOWED));
            }
            return contingencyListService.replaceFilterContingencyListWithScript(id).doOnSuccess(unused ->
                directoryService.notifyDirectoryChanged(id, userId).subscribe());
        });
    }

    public Mono<Void> createFilter(String filter, String filterName, String description, Boolean isPrivate, UUID parentDirectoryUuid, String userId) {
        ElementAttributes elementAttributes = new ElementAttributes(null, filterName, FILTER,
                new AccessRightsAttributes(isPrivate), userId, 0, description);

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
                    FILTER, new AccessRightsAttributes(elementAttributes.getAccessRights().isPrivate()), userId, 0, null);
            return directoryService.createElement(newElementAttributes, parentDirectoryUuid,  userId).flatMap(elementAttributes1 ->
                filterService.insertNewScriptFromFilter(filterId, elementAttributes1.getElementUuid())
                    .doOnError(err ->
                        directoryService.deleteElement(elementAttributes1.getElementUuid(), userId).subscribe()
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
            return filterService.replaceFilterWithScript(id).doOnSuccess(unused ->
                directoryService.notifyDirectoryChanged(id, userId).subscribe());
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

    public Mono<List<ElementAttributes>> getElementsMetadata(List<UUID> ids) {
        Mono<Map<UUID, ElementAttributes>> elementsAttributesMono = directoryService.getElementsAttribute(ids).collect(Collectors.toMap(ElementAttributes::getElementUuid, Function.identity()));

        return elementsAttributesMono.flatMap(elementsAttributes -> {
            List<UUID> filtersUuids = elementsAttributes.values().stream().filter(elementAttributes -> elementAttributes.getType().equals(FILTER)).map(ElementAttributes::getElementUuid).collect(Collectors.toList());
            List<UUID> contingencyListsUuids = elementsAttributes.values().stream().filter(elementAttributes -> elementAttributes.getType().equals(CONTINGENCY_LIST)).map(ElementAttributes::getElementUuid).collect(Collectors.toList());

            Mono<List<Map<String, Object>>> filtersMetadataMono = filterService.getFilterMetadata(filtersUuids).collectList();
            Mono<List<Map<String, Object>>> contingencyListMetadataMono = contingencyListService.getContingencyListMetadata(contingencyListsUuids).collectList();

            Mono<Tuple2<List<Map<String, Object>>, List<Map<String, Object>>>> metadata = Mono.zip(filtersMetadataMono, contingencyListMetadataMono);

            return metadata.map(data -> {
                Map<String, Map<String, Object>> filtersMetadataMap = data.getT1().stream().collect(Collectors.toMap(e -> e.get("id").toString(), Function.identity()));
                Map<String, Map<String, Object>> contingenciesMetadataMap = data.getT2().stream().collect(Collectors.toMap(e -> e.get("id").toString(), Function.identity()));

                elementsAttributes.values().forEach(e -> {
                    if (e.getType().equals(FILTER)) {
                        e.setSpecificMetadata(filtersMetadataMap.get(e.getElementUuid().toString()));
                    } else if (e.getType().equals(CONTINGENCY_LIST)) {
                        e.setSpecificMetadata(contingenciesMetadataMap.get(e.getElementUuid().toString()));
                    }
                });
                return new ArrayList<>(elementsAttributes.values());
            });
        });
    }
}
