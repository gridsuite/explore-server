/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Achour BERRAHMA <achour.berrahma at rte-france.com>
 */
@Service
public class SpreadsheetConfigService implements IDirectoryElementsService {

    private static final String SPREADSHEET_CONFIG_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SPREADSHEET_CONFIG_SERVER_ROOT_PATH = DELIMITER + SPREADSHEET_CONFIG_API_VERSION + DELIMITER + "spreadsheet-configs";

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    private String spreadsheetConfigServerBaseUri;

    @Autowired
    public SpreadsheetConfigService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.spreadsheetConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restClient = restClientBuilder.baseUrl(spreadsheetConfigServerBaseUri).build();
    }

    public void setSpreadsheetConfigServerBaseUri(String spreadsheetConfigServerBaseUri) {
        this.spreadsheetConfigServerBaseUri = spreadsheetConfigServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(spreadsheetConfigServerBaseUri).build();
    }

    public UUID createSpreadsheetConfig(String config) {
        Objects.requireNonNull(config);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH)
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(config)
            .retrieve()
            .body(UUID.class);
    }

    public UUID duplicateSpreadsheetConfig(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
                .buildAndExpand(configUuid)
                .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    public void updateSpreadsheetConfig(UUID configUuid, String config) {
        Objects.requireNonNull(configUuid);
        Objects.requireNonNull(config);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        restClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(config)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID configUuid) {
        Objects.requireNonNull(configUuid);
        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        restClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> configsUuids) {
        Objects.requireNonNull(configsUuids);

        var ids = configsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + "/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }
}
