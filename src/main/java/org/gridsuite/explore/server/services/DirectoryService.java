/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.UNKNOWN_ELEMENT_TYPE;
import static org.gridsuite.explore.server.services.ExploreService.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class DirectoryService implements IDirectoryElementsService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private static final String DIRECTORIES_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "directories";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "elements";

    private final WebClient webClient;
    private final Map<String, IDirectoryElementsService> genericServices;
    private String directoryServerBaseUri;

    @Autowired
    public DirectoryService(@Value("${backing-services.directory-server.base-uri:http://directory-server/}") String directoryServerBaseUri,
                            WebClient.Builder webClientBuilder, FilterService filterService, ContingencyListService contingencyListService, StudyService studyService) {
        this.directoryServerBaseUri = directoryServerBaseUri;
        this.webClient = webClientBuilder.build();
        this.genericServices = Map.of(
            FILTER, filterService,
            CONTINGENCY_LIST, contingencyListService,
            STUDY, studyService,
            DIRECTORY, this);

    }

    public void setDirectoryServerBaseUri(String directoryServerBaseUri) {
        this.directoryServerBaseUri = directoryServerBaseUri;
    }

    public Mono<ElementAttributes> createElement(ElementAttributes elementAttributes, UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements")
            .buildAndExpand(directoryUuid)
            .toUriString();

        return webClient.post()
                .uri(directoryServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, userId)
                .body(BodyInserters.fromValue(elementAttributes))
                .retrieve()
                .bodyToMono(ElementAttributes.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    private Mono<Void> deleteDirectoryElement(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{elementUuid}")
            .buildAndExpand(elementUuid)
            .toUriString();

        return webClient.delete()
                .uri(directoryServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .onStatus(httpStatus -> httpStatus != HttpStatus.OK, ClientResponse::createException)
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<ElementAttributes> getElementInfos(UUID elementUuid) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{directoryUuid}")
            .buildAndExpand(elementUuid)
            .toUriString();

        return webClient.get()
                .uri(directoryServerBaseUri + path)
                .retrieve()
                .bodyToMono(ElementAttributes.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    private Flux<ElementAttributes> getElementsInfos(List<UUID> elementsUuids) {
        var ids = elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH).toUriString() + "?ids=" + ids;
        return webClient.get()
                .uri(directoryServerBaseUri + path)
                .retrieve()
                .bodyToFlux(ElementAttributes.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> notifyDirectoryChanged(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{elementUuid}/notification?type={update_directory}")
            .buildAndExpand(elementUuid, NotificationType.UPDATE_DIRECTORY.name())
            .toUriString();

        return webClient.post()
                .uri(directoryServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    private Flux<ElementAttributes> getDirectoryElements(UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements")
            .buildAndExpand(directoryUuid)
            .toUriString();
        return webClient.get()
            .uri(directoryServerBaseUri + path)
            .header(HEADER_USER_ID, userId)
            .retrieve()
            .bodyToFlux(ElementAttributes.class)
            .publishOn(Schedulers.boundedElastic())
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);

    }

    public Mono<Void> deleteElement(UUID id, String userId) {
        return getElementInfos(id).flatMap(elementAttributes ->
            getGenericService(elementAttributes.getType())
                .flatMap(s -> s.delete(id, userId))
                .doOnSuccess(e -> deleteDirectoryElement(id, userId).subscribe())
        );
    }

    private Mono<IDirectoryElementsService> getGenericService(String type) {
        return Mono.justOrEmpty(genericServices.get(type))
            .switchIfEmpty(Mono.error(() -> new ExploreException(UNKNOWN_ELEMENT_TYPE, "Unknown element type " + type)));
    }

    public Flux<ElementAttributes> getElementsMetadata(List<UUID> ids) {
        return getElementsInfos(ids).groupBy(ElementAttributes::getType)
            .flatMap(grpListIds -> getGenericService(grpListIds.key())
                .flatMapMany(service -> grpListIds.collect(Collectors.toList())
                    .flatMapMany(service::completeElementAttribute)));
    }

    // TODO get id/type recursively then do batch delete
    @Override
    public Mono<Void> delete(UUID id, String userId) {
        return getDirectoryElements(id, userId).flatMap(e -> deleteElement(e.getElementUuid(), userId))
            .then();
    }

}
