/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class FilterService implements IDirectoryElementsService {
    private final RestTemplate restTemplate;

    @Autowired
    public FilterService(RestTemplateBuilder restTemplateBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restTemplate = restTemplateBuilder.rootUri(remoteServicesProperties.getServiceUri("filter-server") + "/v1").build();
    }

    public void replaceFilterWithScript(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath("/filters/{id}/replace-with-script")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = getHeaders(userId);
        restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(headers), Void.class);
    }

    public void insertNewScriptFromFilter(UUID id, UUID newId) {
        String path = UriComponentsBuilder.fromPath("/filters/{id}/new-script")
                .queryParam("newId", newId)
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, null, Void.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath("/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    public void insertFilter(String filter, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath("/filters")
                .queryParam("id", filterId)
                .toUriString();
        HttpEntity<String> httpEntity = getHttpEntityWithUserHeaderAndJsonMediaType(filter, userId);
        restTemplate.exchange(path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertFilter(UUID sourceFilterId, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath("/filters")
                .queryParam("duplicateFrom", sourceFilterId)
                .queryParam("id", filterId)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> filtersUuids) {
        var ids = filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath("/filters/metadata")
                .queryParam("ids", ids)
                .toUriString();
        return restTemplate.exchange(path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
    }

    public void updateFilter(UUID id, String filter, String userId) {
        String path = UriComponentsBuilder.fromPath("/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.PUT, getHttpEntityWithUserHeaderAndJsonMediaType(userId, filter), Void.class);
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
