/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UncheckedIOException;
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
    private static final String NETWORK_MODIFICATIONS_PATH = "network-modifications";
    private String networkModificationServerBaseUri;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NetworkModificationService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties, ObjectMapper objectMapper) {
        this.networkModificationServerBaseUri = remoteServicesProperties.getServiceUri("network-modification-server");
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void setNetworkModificationServerBaseUri(String networkModificationServerBaseUri) {
        this.networkModificationServerBaseUri = networkModificationServerBaseUri;
    }

    public Map<UUID, UUID> createModifications(List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity;
        try {
            httpEntity = new HttpEntity<>(objectMapper.writeValueAsString(modificationUuids), headers);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        return restTemplate.exchange(networkModificationServerBaseUri + path, HttpMethod.POST, httpEntity, new ParameterizedTypeReference<Map<UUID, UUID>>() { })
            .getBody();
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
}
