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
public class ContingencyListService implements IDirectoryElementsService {
    private static final String HEADER_DUPLICATE_FROM = "duplicateFrom";
    private final RestTemplate restTemplate;

    @Autowired
    public ContingencyListService(RestTemplateBuilder restTemplateBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restTemplate = restTemplateBuilder.rootUri(remoteServicesProperties.getServiceUri("actions-server") + "/v1").build();
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath("/contingency-lists/{id}")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public void insertScriptContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath("/script-contingency-lists")
                .queryParam("id", id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(content, headers);
        restTemplate.exchange(path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertScriptContingencyList(UUID sourceListId, UUID id) {
        String path = UriComponentsBuilder.fromPath("/script-contingency-lists")
                .queryParam(HEADER_DUPLICATE_FROM, sourceListId)
                .queryParam("id", id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, null, Void.class);
    }

    public void insertFormContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath("/form-contingency-lists")
                .queryParam("id", id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(content, headers);
        restTemplate.exchange(path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertIdentifierContingencyList(UUID id, String content) {
        String path = UriComponentsBuilder.fromPath("/identifier-contingency-lists")
                .queryParam("id", id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(content, headers);
        restTemplate.exchange(path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertFormContingencyList(UUID sourceListId, UUID id) {
        String path = UriComponentsBuilder.fromPath("/form-contingency-lists")
                .queryParam(HEADER_DUPLICATE_FROM, sourceListId)
                .queryParam("id", id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, null, Void.class);
    }

    public void insertIdentifierContingencyList(UUID sourceListId, UUID id) {
        String path = UriComponentsBuilder.fromPath("/identifier-contingency-lists")
                .queryParam(HEADER_DUPLICATE_FROM, sourceListId)
                .queryParam("id", id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, null, Void.class);
    }

    public void newScriptFromFormContingencyList(UUID id, UUID newId) {
        String path = UriComponentsBuilder.fromPath("/form-contingency-lists/{id}/new-script")
                .queryParam("newId", newId)
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, null, Void.class);
    }

    public void replaceFormContingencyListWithScript(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath("/form-contingency-lists/{id}/replace-with-script")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> contingencyListsUuids) {
        var ids = contingencyListsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath("/contingency-lists/metadata")
                .queryParam("ids", ids)
                .toUriString();
        return restTemplate.exchange(path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
    }

    public void updateContingencyList(UUID id, String content, String userId, String element) {
        String path = UriComponentsBuilder.fromPath(element)
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.PUT, getHttpEntityWithUserHeader(userId, content), Void.class);
    }

    private HttpEntity<String> getHttpEntityWithUserHeader(String userId, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(content, headers);
    }
}
