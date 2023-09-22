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
public class StudyService implements IDirectoryElementsService {
    private final RestTemplate restTemplate;

    @Autowired
    public StudyService(RestTemplateBuilder restTemplateBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restTemplate = restTemplateBuilder.rootUri(remoteServicesProperties.getServiceUri("study-server") + "/v1").build();
    }

    public void insertStudyWithExistingCaseFile(UUID studyUuid, String userId, UUID caseUuid,
            Map<String, Object> importParams, Boolean duplicateCase) {
        String path = UriComponentsBuilder.fromPath("/studies/cases/{caseUuid}")
                .queryParam("studyUuid", studyUuid)
                .queryParam("duplicateCase", duplicateCase)
                .buildAndExpand(caseUuid)
                .toUriString();
        HttpHeaders headers = getHeaders(userId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(importParams, headers);
        restTemplate.exchange(path, HttpMethod.POST, request, Void.class);
    }

    public void duplicateStudy(UUID sourceStudyUuid, UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath("/studies")
                .queryParam("duplicateFrom", sourceStudyUuid)
                .queryParam("studyUuid", studyUuid)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    @Override
    public void delete(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath("/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();
        restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        var ids = studiesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath("/studies/metadata")
                .queryParam("ids", ids)
                .toUriString();
        return restTemplate.exchange(path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() { }).getBody();
    }

    private HttpHeaders getHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }
}
