/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
    private static final String HEADER_USER_ID = "userId";
    public static final String UUIDS = "uuids";
    public static final String NETWORK_COMPOSITE_MODIFICATIONS_PATH = "network-composite-modifications";
    private static final String NETWORK_MODIFICATIONS_PATH = "network-modifications";
    private String networkModificationServerBaseUri;
    private final RestTemplate restTemplate;

    public NetworkModificationService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.networkModificationServerBaseUri = remoteServicesProperties.getServiceUri("network-modification-server");
        this.restTemplate = restTemplate;
    }

    public void setNetworkModificationServerBaseUri(String networkModificationServerBaseUri) {
        this.networkModificationServerBaseUri = networkModificationServerBaseUri;
    }

    public Map<UUID, UUID> duplicateCompositeModifications(List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + "duplication")
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(modificationUuids, headers), new ParameterizedTypeReference<Map<UUID, UUID>>() { })
            .getBody();
    }

    public UUID createCompositeModification(List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(modificationUuids, headers), new ParameterizedTypeReference<UUID>() { })
                .getBody();
    }

    public void updateCompositeModification(UUID compositeModificationId, List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + compositeModificationId)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.PUT, new HttpEntity<>(modificationUuids, headers), Void.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .queryParam(UUIDS, List.of(id))
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> modificationUuids) {
        var ids = modificationUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH + "/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody();
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER +
                        NETWORK_MODIFICATION_API_VERSION +
                        DELIMITER +
                        "/network-composite-modification/" +
                        compositeModificationId +
                        "/network-modifications")
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Object>>() {
                }).getBody();
    }
}
