/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseService implements IDirectoryElementsService {
    private static final String CASE_SERVER_API_VERSION = "v1";
    private static final String CASES_URL = "cases";
    private static final String DELIMITER = "/";
    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;
    private String caseServerBaseUri;

    public CaseService(@Value("${powsybl.services.case-server.base-uri:http://case-server/}") String caseServerBaseUri,
                       RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
        this.caseServerBaseUri = caseServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(this.caseServerBaseUri).build();
    }

    public void setBaseUri(String caseServerBaseUri) {
        this.caseServerBaseUri = caseServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(caseServerBaseUri).build();
    }

    private UUID importCaseResource(Resource resource) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (resource != null) {
            body.add("file", resource);
        }

        String path = UriComponentsBuilder
            .fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL)
            .buildAndExpand()
            .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(UUID.class);
    }

    UUID importMultipartCase(MultipartFile multipartFile) {
        Objects.requireNonNull(multipartFile);
        Objects.requireNonNull(multipartFile.getOriginalFilename());
        return importCaseResource(multipartFile.getResource());
    }

    public UUID importFileCase(File file) {
        return importCaseResource(new FileSystemResource(file));
    }

    public UUID importCaseWithoutDirectoryElementCreation(MultipartFile multipartFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (multipartFile != null) {
            Objects.requireNonNull(multipartFile.getOriginalFilename());
            body.add("file", multipartFile.getResource());
        }
        body.add("withExpiration", true);

        String path = UriComponentsBuilder
            .fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL)
            .buildAndExpand()
            .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(UUID.class);
    }

    public ResponseEntity<Resource> downloadCase(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + "{caseUuid}")
            .buildAndExpand(caseUuid)
            .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .toEntity(Resource.class);
    }

    public String getBaseName(String caseName) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + "caseBaseName")
            .queryParam("caseName", caseName)
            .buildAndExpand()
            .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(String.class);
    }

    void persistCase(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + caseUuid + "/disableExpiration")
            .buildAndExpand()
            .toUriString();

        restClient.put()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }

    UUID duplicateCase(UUID caseId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + "{uuid}/duplicate")
            .buildAndExpand(caseId)
            .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    @Override
    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + "{id}")
            .buildAndExpand(id)
            .toUriString();

        restClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> casesUuids) {
        var ids = casesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
            .fromPath(DELIMITER + CASE_SERVER_API_VERSION + DELIMITER + CASES_URL + DELIMITER + "metadata" + "?ids=" + ids)
            .buildAndExpand()
            .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }
}
