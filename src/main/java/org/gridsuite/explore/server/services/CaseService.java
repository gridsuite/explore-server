/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.error.ExploreBusinessErrorCode;
import org.gridsuite.explore.server.error.ExploreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseService implements IDirectoryElementsService {
    private static final String CASE_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    private String caseServerBaseUri;

    public CaseService(@Value("${powsybl.services.case-server.base-uri:http://case-server/}") String studyServerBaseUri,
                       RestTemplate restTemplate) {
        this.caseServerBaseUri = studyServerBaseUri;
        this.restTemplate = restTemplate;
    }

    public void setBaseUri(String actionsServerBaseUri) {
        this.caseServerBaseUri = actionsServerBaseUri;
    }

    UUID importCase(MultipartFile multipartFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        UUID caseUuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (multipartFile != null) {
            Objects.requireNonNull(multipartFile.getOriginalFilename());
            body.add("file", multipartFile.getResource());
        }
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(
            body, headers);
        try {
            caseUuid = restTemplate.postForObject(caseServerBaseUri + "/" + CASE_SERVER_API_VERSION + "/cases", request,
                UUID.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw ExploreException.of(ExploreBusinessErrorCode.EXPLORE_INCORRECT_CASE_FILE, e.getMessage());
            }
            throw e;
        }
        return caseUuid;
    }

    UUID duplicateCase(UUID caseId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases")
            .queryParam("duplicateFrom", caseId)
            .buildAndExpand()
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(caseServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), UUID.class)
            .getBody();
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

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> casesUuids) {
        var ids = casesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
            .fromPath(DELIMITER + CASE_SERVER_API_VERSION + "/cases/metadata" + "?ids=" + ids)
            .buildAndExpand()
            .toUriString();
        return restTemplate.exchange(caseServerBaseUri + path, HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {
            }).getBody();
    }
}
