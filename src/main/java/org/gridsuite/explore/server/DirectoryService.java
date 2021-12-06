/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class DirectoryService {
    private static final String ROOT_CATEGORY_REACTOR = "reactor.";

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    static final String HEADER_USER_ID = "userId";

    private final WebClient webClient;
    private String directoryServerBaseUri;

    @Autowired
    public DirectoryService(@Value("${backing-services.directory-server.base-uri:http://directory-server/}") String directoryServerBaseUri) {
        this.directoryServerBaseUri = directoryServerBaseUri;
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .build();
    }

    public void setDirectyServerBaseUri(String directoryServerBaseUri) {
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
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }

    public Mono<Void> deleteElement(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + "/directories/{elementUuid}")
                .buildAndExpand(elementUuid)
                .toUriString();
        return webClient.delete()
                .uri(directoryServerBaseUri + path)
                .header(HEADER_USER_ID, userId)
                .retrieve()
                .onStatus(httpStatus -> httpStatus != HttpStatus.OK, r -> Mono.empty())
                .bodyToMono(Void.class)
                .publishOn(Schedulers.boundedElastic())
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
                .publishOn(Schedulers.boundedElastic())
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
                .publishOn(Schedulers.boundedElastic())
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
                .publishOn(Schedulers.boundedElastic())
                .log(ROOT_CATEGORY_REACTOR, Level.FINE);
    }
}
