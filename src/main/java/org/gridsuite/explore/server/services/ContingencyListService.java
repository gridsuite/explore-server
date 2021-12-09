/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class ContingencyListService implements IDirectoryElementsService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String ACTIONS_API_VERSION = "v1";
    private static final String DELIMITER = "/";

    private final WebClient webClient;
    private String actionsServerBaseUri;

    @Autowired
    public ContingencyListService(@Value("${backing-services.actions-server.base-uri:http://actions-server/}") String actionsServerBaseUri,
                                  WebClient.Builder webClientBuilder) {
        this.actionsServerBaseUri = actionsServerBaseUri;
        this.webClient = webClientBuilder.build();
    }

    public void setActionsServerBaseUri(String actionsServerBaseUri) {
        this.actionsServerBaseUri = actionsServerBaseUri;
    }

    public Mono<Void> delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/{id}")
            .buildAndExpand(id)
            .toUriString();

        return webClient.delete()
            .uri(actionsServerBaseUri + path)
            .header(HEADER_USER_ID, userId)
            .retrieve()
            .onStatus(httpStatus -> httpStatus != HttpStatus.OK, ClientResponse::createException)
            .bodyToMono(Void.class)
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> insertScriptContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/script-contingency-lists?id={id}")
            .buildAndExpand(id)
            .toUriString();

        return webClient.post()
            .uri(actionsServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(content))
            .retrieve()
            .bodyToMono(Void.class)
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> insertFiltersContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists?id={id}")
            .buildAndExpand(id)
            .toUriString();

        return webClient.post()
            .uri(actionsServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(content))
            .retrieve()
            .bodyToMono(Void.class)
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> newScriptFromFiltersContingencyList(UUID id, UUID newId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists/{id}/new-script?newId={newId}")
            .buildAndExpand(id, newId)
            .toUriString();

        return webClient.post()
            .uri(actionsServerBaseUri + path)
            .retrieve()
            .bodyToMono(Void.class)
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> replaceFilterContingencyListWithScript(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists/{id}/replace-with-script")
            .buildAndExpand(id)
            .toUriString();

        return webClient.post()
            .uri(actionsServerBaseUri + path)
            .retrieve()
            .bodyToMono(Void.class)
            .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    @Override
    public Flux<Map<String, Object>> getMetadata(List<UUID> contingencyListsUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/metadata")
                .buildAndExpand()
                .toUriString();
        return webClient.get()
                .uri(actionsServerBaseUri + path)
                .header("ids", contingencyListsUuids.stream().map(UUID::toString).collect(Collectors.joining(",")))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() { })
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }
}
