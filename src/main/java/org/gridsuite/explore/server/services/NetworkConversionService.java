/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

@Service
public class NetworkConversionService {

    private static final String NETWORK_CONVERSION_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String HEADER_USER_ID = "userId";

    private String networkConversionServerBaseUri;
    private final RestTemplate restTemplate;

    public NetworkConversionService(@Value("${powsybl.services.network-conversion-server.base-uri:http://network-conversion-server/}") String networkConversionServerBaseUri,
                                    RestTemplate restTemplate) {
        this.networkConversionServerBaseUri = networkConversionServerBaseUri;
        this.restTemplate = restTemplate;
    }

    public void setNetworkConversionServerBaseUri(String networkConversionServerBaseUri) {
        this.networkConversionServerBaseUri = networkConversionServerBaseUri;
    }

    public ResponseEntity<String> getCaseImportParameters(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/cases/{caseUuid}/import-parameters")
            .buildAndExpand(caseUuid)
            .toUriString();
        try {
            return restTemplate.exchange(networkConversionServerBaseUri + path, HttpMethod.GET, null, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                .body(e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<UUID> convertCase(UUID caseUuid, String format, String fileName, String formatParameters, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/cases/{caseUuid}/convert/{format}")
            .queryParam("fileName", fileName)
            .buildAndExpand(caseUuid, format)
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_USER_ID, userId);
        try {
            return restTemplate.exchange(networkConversionServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(formatParameters, headers), UUID.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                .build();
        }
    }

    public ResponseEntity<Resource> downloadFile(UUID exportUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/download-file/{exportUuid}")
            .buildAndExpand(exportUuid)
            .toUriString();
        try {
            return restTemplate.exchange(networkConversionServerBaseUri + path, HttpMethod.GET, null, Resource.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                .build();
        }
    }

    public ResponseEntity<String> getExportFormats() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/export/formats")
            .buildAndExpand()
            .toUriString();
        try {
            return restTemplate.exchange(networkConversionServerBaseUri + path, HttpMethod.GET, null, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                .body(e.getResponseBodyAsString());
        }
    }
}
