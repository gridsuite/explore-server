/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class ContingencyListService implements IDirectoryElementsService {
    private static final String ACTIONS_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;
    private String actionsServerBaseUri;

    public ContingencyListService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.actionsServerBaseUri = remoteServicesProperties.getServiceUri("actions-server");
        this.restClient = restClientBuilder.baseUrl(actionsServerBaseUri).build();
    }

    public void setActionsServerBaseUri(String actionsServerBaseUri) {
        this.actionsServerBaseUri = actionsServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(actionsServerBaseUri).build();
    }

    @Override
    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/{id}")
                .buildAndExpand(id)
                .toUriString();

        restClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }

    public void insertIdentifierContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/identifier-contingency-lists?id={id}")
                .buildAndExpand(id)
                .toUriString();

        restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(content)
            .retrieve()
            .toBodilessEntity();
    }

    public void insertFilterBasedContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists?id={id}")
            .buildAndExpand(id)
            .toUriString();

        restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(content)
            .retrieve()
            .toBodilessEntity();
    }

    public UUID duplicateIdentifierContingencyList(UUID identifierContingencyListsId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/identifier-contingency-lists/{uuid}/duplicate")
                .buildAndExpand(identifierContingencyListsId)
                .toUriString();

        return restClient.post()
            .uri(path)
            .retrieve()
            .body(UUID.class);
    }

    public UUID duplicateFilterBasedContingencyList(UUID filterContingencyListsId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists/{uuid}/duplicate")
            .buildAndExpand(filterContingencyListsId)
            .toUriString();

        return restClient.post()
            .uri(path)
            .retrieve()
            .body(UUID.class);
    }

    public String getIdentifierContingencyList(UUID id) {
        return getContingencyList("/identifier-contingency-lists/{id}", id);
    }

    public String getFilterBasedContingencyList(UUID id) {
        return getContingencyList("/filters-contingency-lists/{id}", id);
    }

    private String getContingencyList(String endpoint, UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + endpoint)
                .buildAndExpand(id)
                .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(String.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> contingencyListsUuids) {
        var ids = contingencyListsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public void updateContingencyList(UUID id, String content, String element) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + element)
                .buildAndExpand(id)
                .toUriString();

        restClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(content)
            .retrieve()
            .toBodilessEntity();
    }
}
