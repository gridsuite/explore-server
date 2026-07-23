/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
            Map<String, Object> importParams, Boolean duplicateCase, String firstRootNetworkName) {
        var uriComponentsBuilder = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/cases/{caseUuid}")
                .queryParam("studyUuid", studyUuid)
                .queryParam("duplicateCase", duplicateCase)
                .queryParam("caseFormat", caseFormat);

        if (!StringUtils.isBlank(firstRootNetworkName)) {
            uriComponentsBuilder.queryParam("firstRootNetworkName", firstRootNetworkName);
        }
        String path = uriComponentsBuilder.buildAndExpand(caseUuid).toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                importParams, headers);
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, request, Void.class);
    }

    public UUID duplicateStudy(UUID studyId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/{uuid}/duplicate")
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

    public ResponseEntity<String> evaluateFiltersOnFirstRootNetwork(UUID studyUuid, String filters) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}/filters/elements")
                .buildAndExpand(studyUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(filters, headers), String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                    .body(e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> getServersInfos(String view) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/servers/about")
                .queryParam("view", view)
                .buildAndExpand()
                .toUriString();
        try {
            return restTemplate.exchange(studyServerBaseUri + path, HttpMethod.GET, null, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                    .body(e.getResponseBodyAsString());
        }
    }
}
