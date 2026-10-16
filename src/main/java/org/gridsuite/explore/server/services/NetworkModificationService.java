/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;
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
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class NetworkModificationService implements IDirectoryElementsService {
    private static final String NETWORK_MODIFICATION_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    public static final String UUIDS = "uuids";
    public static final String NAME = "name";
    public static final String NETWORK_COMPOSITE_MODIFICATIONS_PATH = "network-composite-modifications";
    private static final String NETWORK_MODIFICATIONS_PATH = "network-modifications";
    private static final String CONTAINERS_PATH = "containers";
    @Setter
    private String networkModificationServerBaseUri;
    private final RestClient restClient;

    public NetworkModificationService(RestClient restClient, RemoteServicesProperties remoteServicesProperties) {
        this.restClient = restClient;
        this.networkModificationServerBaseUri = remoteServicesProperties.getServiceUri("network-modification-server");
    }

    public Map<UUID, UUID> duplicateCompositeModifications(List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + "duplication")
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public UUID createCompositeModification(List<UUID> modificationUuids, String compositeModificationName) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH)
                .queryParam(NAME, compositeModificationName)
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .body(UUID.class);
    }

    public void createModification(UUID groupUuid, JsonNode modification) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .queryParam("groupUuid", groupUuid)
                .buildAndExpand()
                .toUriString();

        Map<String, Object> body = Map.of("first", modification, "second", List.of());

        restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    }

    public void replaceCompositeModification(UUID compositeModificationId, String newName, List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + compositeModificationId + "/replace")
                .queryParam(NAME, newName)
                .buildAndExpand()
                .toUriString();

        restClient.put()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .toBodilessEntity();
    }

    /**
     * @param newName null if the name shouldn't be updated
     */
    public void updateCompositeModification(UUID compositeModificationId, String newName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(
                DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + compositeModificationId
                );
        if (newName != null) {
            uriComponentsBuilder.queryParam(NAME, newName);
        }

        String path = uriComponentsBuilder.buildAndExpand().toUriString();

        restClient.put()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .queryParam(UUIDS, List.of(id))
                .buildAndExpand()
                .toUriString();

        restClient.delete()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> modificationUuids) {
        var ids = modificationUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH + "/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER +
                        NETWORK_MODIFICATION_API_VERSION +
                        DELIMITER +
                        CONTAINERS_PATH +
                        DELIMITER +
                        "{containerUuid}" +
                        DELIMITER +
                        NETWORK_MODIFICATIONS_PATH)
                .queryParam("onlyMetadata", true)
                .buildAndExpand(compositeModificationId)
                .toUriString();
        return restClient.get()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }
}
