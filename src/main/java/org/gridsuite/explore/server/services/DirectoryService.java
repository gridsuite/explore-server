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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
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

    private final WebClient webClient;
    private final Map<String, IDirectoryElementsService> genericServices;
    private String directoryServerBaseUri;

    @Autowired
    public DirectoryService(@Value("${backing-services.directory-server.base-uri:http://directory-server/}") String directoryServerBaseUri,
                            FilterService filterService, ContingencyListService contingencyListService, StudyService studyService) {
        this.directoryServerBaseUri = directoryServerBaseUri;
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .build();
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
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{directoryUuid}")
                .buildAndExpand(directoryUuid)
                .toUriString();

        return webClient.post()
                .uri(directoryServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, userId)
                .body(BodyInserters.fromValue(elementAttributes))
                .retrieve()
                .bodyToMono(ElementAttributes.class)
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    private Mono<Void> deleteDirectoryElement(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{elementUuid}")
                .buildAndExpand(elementUuid)
                .toUriString();
        return webClient.delete()
                .uri(directoryServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .onStatus(httpStatus -> httpStatus != HttpStatus.OK, ClientResponse::createException)
                .bodyToMono(Void.class)
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<ElementAttributes> getElementInfos(UUID directoryUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{directoryUuid}")
                .buildAndExpand(directoryUuid)
                .toUriString();

        return webClient.get()
                .uri(directoryServerBaseUri + path)
                .retrieve()
                .bodyToMono(ElementAttributes.class)
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Flux<ElementAttributes> getElementsAttribute(List<UUID> ids) {
        var idsStr = new StringJoiner("&id=");
        ids.forEach(id -> idsStr.add(id.toString()));
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/elements").toUriString() + "?id=" + idsStr;
        return webClient.get()
                .uri(directoryServerBaseUri + path)
                .retrieve()
                .bodyToFlux(ElementAttributes.class)
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> notifyDirectoryChanged(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{elementUuid}/notify-parent")
                .buildAndExpand(elementUuid)
                .toUriString();
        return webClient.put()
                .uri(directoryServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .bodyToMono(Void.class)
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Flux<ElementAttributes> listDirectoryContent(UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{directoryUuid}/content")
            .buildAndExpand(directoryUuid)
            .toUriString();
        return webClient.get()
            .uri(directoryServerBaseUri + path)
            .header(HEADER_USER_ID, userId)
            .retrieve()
            .bodyToFlux(ElementAttributes.class)
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
        return getElementsAttribute(ids).groupBy(ElementAttributes::getType)
            .flatMap(grpListIds -> getGenericService(grpListIds.key())
                .flatMapMany(service -> grpListIds.collect(Collectors.toList())
                    .flatMapMany(service::completeElementAttribute)));
    }

    // TODO get id/type recursively then do batch delete
    @Override
    public Mono<Void> delete(UUID id, String userId) {
        return listDirectoryContent(id, userId).flatMap(e -> deleteElement(e.getElementUuid(), userId))
            .then();
    }

}
