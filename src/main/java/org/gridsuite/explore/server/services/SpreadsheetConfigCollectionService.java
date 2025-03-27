/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class SpreadsheetConfigCollectionService implements IDirectoryElementsService {

    private static final String SPREADSHEET_CONFIG_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SPREADSHEET_CONFIG_COLLECTIONS_PATH = DELIMITER + SPREADSHEET_CONFIG_API_VERSION + DELIMITER + "spreadsheet-config-collections";
    private static final String DUPLICATE_FROM_PARAMETER = "duplicateFrom";

    private final RestTemplate restTemplate;

    @Setter
    private String spreadsheetConfigServerBaseUri;

    @Autowired
    public SpreadsheetConfigCollectionService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.spreadsheetConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restTemplate = restTemplate;
    }

    public UUID createSpreadsheetConfigCollectionFromConfigIds(List<UUID> configIds) {
        Objects.requireNonNull(configIds);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + "/merge")
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<UUID>> httpEntity = new HttpEntity<>(configIds, headers);

        return restTemplate.postForObject(spreadsheetConfigServerBaseUri + path, httpEntity, UUID.class);
    }

    public UUID createSpreadsheetConfigCollection(String collection) {
        Objects.requireNonNull(collection);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(collection, headers);

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public UUID duplicateSpreadsheetConfigCollection(UUID collectionId) {
        Objects.requireNonNull(collectionId);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH)
                .queryParam(DUPLICATE_FROM_PARAMETER, collectionId)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public void updateSpreadsheetConfigCollection(UUID collectionId, String collection) {
        Objects.requireNonNull(collectionId);
        Objects.requireNonNull(collection);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + collectionId)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(collection, headers);

        restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }

    public void replaceAllSpreadsheetConfigsInCollection(UUID collectionId, List<UUID> configIds) {
        Objects.requireNonNull(collectionId);
        Objects.requireNonNull(configIds);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + collectionId + "/spreadsheet-configs/replace-all")
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<UUID>> httpEntity = new HttpEntity<>(configIds, headers);

        restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }

    @Override
    public void delete(UUID configUuid, String userId) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(spreadsheetConfigServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
