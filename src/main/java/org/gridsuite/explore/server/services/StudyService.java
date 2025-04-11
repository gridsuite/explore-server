/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

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
    private static final String NOTIFICATION_TYPE_METADATA_UPDATED = "metadata_updated";
    private final RestTemplate restTemplate;
    private String studyServerBaseUri;

    public StudyService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.studyServerBaseUri = remoteServicesProperties.getServiceUri("study-server");
        this.restTemplate = restTemplate;
    }

    public void setStudyServerBaseUri(String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public void insertStudyWithExistingCaseFile(UUID studyUuid, String userId, UUID caseUuid, String caseFormat,
            Map<String, Object> importParams, Boolean duplicateCase, String caseName) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/cases/{caseUuid}")
                .queryParam("studyUuid", studyUuid)
                .queryParam("duplicateCase", duplicateCase)
                .queryParam("caseFormat", caseFormat)
                .queryParam("caseName", caseName)
                .buildAndExpand(caseUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                importParams, headers);
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, request, Void.class);
    }

    public UUID duplicateStudy(UUID studyId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies")
                .queryParam("duplicateFrom", studyId)
                .buildAndExpand(studyId)
                .toUriString();
        return restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)),
                UUID.class).getBody();
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

    public ResponseEntity<Void> notifyStudyUpdate(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies/{studyUuid}/notification?type={metadata_updated}")
                .buildAndExpand(studyUuid, NOTIFICATION_TYPE_METADATA_UPDATED)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        return restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
    }
}
