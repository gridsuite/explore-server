/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
public class NetworkConversionService {

    private static final String NETWORK_CONVERSION_API_VERSION = "v1";
    private static final String DELIMITER = "/";

    @Setter
    private String networkConversionServerBaseUri;
    private final RestClient restClient;

    public NetworkConversionService(@Value("${powsybl.services.network-conversion-server.base-uri:http://network-conversion-server/}") String networkConversionServerBaseUri,
                                    RestClient restClient) {
        this.networkConversionServerBaseUri = networkConversionServerBaseUri;
        this.restClient = restClient;
    }

    public String getCaseImportParameters(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/cases/{caseUuid}/import-parameters")
            .buildAndExpand(caseUuid)
            .toUriString();

        return restClient.get()
            .uri(networkConversionServerBaseUri + path)
            .retrieve()
            .body(String.class);
    }

    public UUID convertCase(UUID caseUuid, String format, String fileName, String formatParameters) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/cases/{caseUuid}/convert/{format}")
            .queryParam("fileName", fileName)
            .buildAndExpand(caseUuid, format)
            .toUriString();

        var request = restClient.post()
            .uri(networkConversionServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON);
        if (formatParameters != null) {
            request.body(formatParameters);
        }
        return request.retrieve().body(UUID.class);
    }

    public ResponseEntity<Resource> downloadFile(UUID exportUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/download-file/{exportUuid}")
            .buildAndExpand(exportUuid)
            .toUriString();

        return restClient.get()
            .uri(networkConversionServerBaseUri + path)
            .retrieve()
            .toEntity(Resource.class);
    }

    public String getExportFormats() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/export/formats")
            .buildAndExpand()
            .toUriString();

        return restClient.get()
            .uri(networkConversionServerBaseUri + path)
            .retrieve()
            .body(String.class);
    }
}
