/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class StudyService implements IDirectoryElementsService {
    private static final String STUDY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private String studyServerBaseUri;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public StudyService(@Value("${backing-services.study-server.base-uri:http://study-server/}") String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public void setStudyServerBaseUri(String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public void insertStudyWithExistingCaseFile(UUID studyUuid, String userId, UUID caseUuid, Map<String, Object> importParams) {

        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies/cases/{caseUuid}?studyUuid={studyUuid}")
                .buildAndExpand(caseUuid, studyUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                importParams, headers);
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, request, Void.class);
    }

    public void insertStudyWithCaseFile(UUID studyUuid, String userId, @Nullable MultipartFile caseFile) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies?studyUuid={studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();

        try {
            if (caseFile != null) {
                String filename = caseFile.getOriginalFilename();
                multipartBodyBuilder.part("file", caseFile.getBytes()).filename(filename);
            }
        } catch (IOException e) {
            throw new ExploreException(IMPORT_CASE_FAILED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(HEADER_USER_ID, userId);
        HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(
                multipartBodyBuilder.build(), headers);
        try {
            restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new ExploreException(INSERT_STUDY_FAILED);
        }
    }

    public void insertStudy(UUID sourceStudyUuid, UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies")
                .queryParam("duplicateFrom", sourceStudyUuid)
                .queryParam("studyUuid", studyUuid)
                .toUriString();
        restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    @Override
    public void delete(UUID studyUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();

        try {
            restTemplate.exchange(studyServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.OK != e.getStatusCode()) {
                throw new ExploreException(DELETE_STUDY_FAILED);
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        var ids = studiesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return Collections.singletonList(restTemplate.exchange(studyServerBaseUri + path, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {
        }).getBody());
    }

    private HttpHeaders getHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }
}
