/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class StudyService implements IDirectoryElementsService {
    private static final String STUDY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    private String studyServerBaseUri;

    @Autowired
    public StudyService(
            @Value("${gridsuite.services.study-server.base-uri:http://study-server/}") String studyServerBaseUri,
            RestTemplate restTemplate) {
        this.studyServerBaseUri = studyServerBaseUri;
        this.restTemplate = restTemplate;
    }

    public void setStudyServerBaseUri(String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public void insertStudyWithExistingCaseFile(UUID studyUuid, String userId, UUID caseUuid,
            Map<String, Object> importParams, Boolean duplicateCase) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/cases/{caseUuid}")
                .queryParam("studyUuid", studyUuid)
                .queryParam("duplicateCase", duplicateCase)
                .buildAndExpand(caseUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                importParams, headers);
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, request, Void.class);
    }

    public void duplicateStudy(UUID sourceStudyUuid, UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies")
                .queryParam("duplicateFrom", sourceStudyUuid)
                .queryParam("studyUuid", studyUuid)
                .toUriString();
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)),
                Void.class);
    }

    @Override
    public void delete(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(getHeaders(userId)),
                Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        var ids = studiesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(studyServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody();
    }

    private HttpHeaders getHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }
}
