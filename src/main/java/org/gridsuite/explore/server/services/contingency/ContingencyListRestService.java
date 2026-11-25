/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.contingency;

import lombok.Setter;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
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
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
@Service
public class ContingencyListRestService {
    private static final String ACTIONS_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    @Setter
    private String actionsServerBaseUri;
    private final RestClient restClient;

    public ContingencyListRestService(RestClient restClient, RemoteServicesProperties remoteServicesProperties) {
        this.actionsServerBaseUri = remoteServicesProperties.getServiceUri("actions-server");
        this.restClient = restClient;
    }

    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/{id}")
                .buildAndExpand(id)
                .toUriString();

        restClient.delete()
                .uri(actionsServerBaseUri + path)
                .retrieve()
                .toBodilessEntity();
    }

    public void insertIdentifierContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/identifier-contingency-lists?id={id}")
                .buildAndExpand(id)
                .toUriString();

        var request = restClient.post()
                .uri(actionsServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON);
        if (content != null) {
            request.body(content);
        }
        request.retrieve().toBodilessEntity();
    }

    public void insertFilterBasedContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists?id={id}")
                .buildAndExpand(id)
                .toUriString();

        var request = restClient.post()
                .uri(actionsServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON);
        if (content != null) {
            request.body(content);
        }
        request.retrieve().toBodilessEntity();
    }

    public UUID duplicateIdentifierContingencyList(UUID identifierContingencyListsId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/identifier-contingency-lists/{uuid}/duplicate")
                .buildAndExpand(identifierContingencyListsId)
                .toUriString();

        return restClient.post()
                .uri(actionsServerBaseUri + path)
                .retrieve()
                .body(UUID.class);
    }

    public UUID duplicateFilterBasedContingencyList(UUID filterContingencyListsId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + "/filters-contingency-lists/{uuid}/duplicate")
                .buildAndExpand(filterContingencyListsId)
                .toUriString();

        return restClient.post()
                .uri(actionsServerBaseUri + path)
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
                .uri(actionsServerBaseUri + path)
                .retrieve()
                .body(String.class);
    }

    public List<Map<String, Object>> getMetadata(List<UUID> contingencyListsUuids) {
        var ids = contingencyListsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + ACTIONS_API_VERSION + "/contingency-lists/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
                .uri(actionsServerBaseUri + path)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
    }

    public void updateContingencyList(UUID id, String content, String element) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + ACTIONS_API_VERSION + element)
                .buildAndExpand(id)
                .toUriString();

        restClient.put()
                .uri(actionsServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }
}
