/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.explore.server.ExploreException.Type.*;

@Service
public class CaseService implements IDirectoryElementsService {
    private static final String CASE_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    private String caseServerBaseUri;

    @Autowired
    public CaseService(@Value("${backing-services.case-server.base-uri:http://case-server/}") String studyServerBaseUri, RestTemplate restTemplate) {
        this.caseServerBaseUri = studyServerBaseUri;
        this.restTemplate = restTemplate;
    }

    private static ExploreException wrapRemoteError(String response, HttpStatus statusCode) {
        if (!"".equals(response)) {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, response);
        } else {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, "{\"message\": " + statusCode + "\"}");
        }
    }

    public void setBaseUri(String actionsServerBaseUri) {
        this.caseServerBaseUri = actionsServerBaseUri;
    }

    UUID importCase(MultipartFile multipartFile) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        UUID caseUuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            if (multipartFile != null) {
                multipartBodyBuilder.part("file", multipartFile.getBytes()).filename(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            }
        } catch (IOException e) {
            throw new ExploreException(IMPORT_CASE_FAILED);
        }
        HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(
                multipartBodyBuilder.build(), headers);
        try {
            caseUuid = restTemplate.postForObject(caseServerBaseUri + "/" + CASE_SERVER_API_VERSION + "/cases/private", request, UUID.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                throw new ExploreException(INCORRECT_CASE_FILE, e.getMessage());
            }
            throw wrapRemoteError(e.getMessage(), e.getStatusCode());
        }
        return caseUuid;
    }

    UUID createCase(UUID sourceCaseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases")
                .queryParam("duplicateFrom", sourceCaseUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(caseServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), UUID.class).getBody();
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases/{id}")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(caseServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
