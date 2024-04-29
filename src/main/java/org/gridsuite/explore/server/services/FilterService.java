/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class FilterService implements IDirectoryElementsService {
    private static final String FILTER_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private static final String HEADER_USER_ID = "userId";

    private String filterServerBaseUri;

    private final RestTemplate restTemplate;

    @Autowired
    public FilterService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.filterServerBaseUri = remoteServicesProperties.getServiceUri("filter-server");
        this.restTemplate = restTemplate;
    }

    public void setFilterServerBaseUri(String filterServerBaseUri) {
        this.filterServerBaseUri = filterServerBaseUri;
    }

    public void replaceFilterWithScript(UUID id, String userId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/replace-with-script")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.PUT, new HttpEntity<>(headers), Void.class);
    }

    public void insertNewScriptFromFilter(UUID id, UUID newId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/new-script?newId={newId}")
                .buildAndExpand(id, newId)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, null, Void.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(getHeaders(userId)),
                Void.class);
    }

    public void insertFilter(String filter, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters?id={id}")
                .buildAndExpand(filterId)
                .toUriString();
        HttpHeaders headers = getHeaders(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(filter, headers);
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, httpEntity, Void.class);
    }

    public UUID duplicateFilter(UUID filterId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters")
                .queryParam("duplicateFrom", filterId)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers),
                UUID.class).getBody();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> filtersUuids) {
        var ids = filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(filterServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody();
    }

    public void updateFilter(UUID id, String filter, String userId) {

        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}")
                .buildAndExpand(id)
                .toUriString();

        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.PUT, getHttpEntityWithUserHeaderAndJsonMediaType(userId, filter), Void.class);

    }

    private HttpHeaders getHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }

    private HttpEntity<String> getHttpEntityWithUserHeaderAndJsonMediaType(String userId, String content) {
        HttpHeaders headers = getHeaders(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(content, headers);
    }

}
