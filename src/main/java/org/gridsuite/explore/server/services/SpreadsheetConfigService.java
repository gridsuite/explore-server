/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
    private static final String DUPLICATE_FROM_PARAMETER = "duplicateFrom";

    private final RestTemplate restTemplate;

    @Setter
    private String spreadsheetConfigServerBaseUri;

    @Autowired
    public SpreadsheetConfigService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.spreadsheetConfigServerBaseUri = remoteServicesProperties.getServiceUri("spreadsheet-config-server");
        this.restTemplate = restTemplate;
    }

    public UUID createSpreadsheetConfig(String config) {
        Objects.requireNonNull(config);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(config, headers);

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public String getSpreadsheetConfig(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.GET, null, String.class).getBody();
    }

    public UUID duplicateSpreadsheetConfig(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + "duplicate")
                .queryParam(DUPLICATE_FROM_PARAMETER, configUuid)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public void updateSpreadsheetConfig(UUID configUuid, String config) {
        Objects.requireNonNull(configUuid);
        Objects.requireNonNull(config);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(config, headers);

        restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }

    @Override
    public void delete(UUID configUuid, String userId) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> configsUuids) {
        Objects.requireNonNull(configsUuids);

        var ids = configsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_SERVER_ROOT_PATH + "/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).getBody();
    }

}
