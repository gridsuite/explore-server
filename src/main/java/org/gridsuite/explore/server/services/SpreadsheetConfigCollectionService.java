/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    private String spreadsheetConfigServerBaseUri;

    @Autowired
    public SpreadsheetConfigCollectionService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.spreadsheetConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restClient = restClientBuilder.baseUrl(spreadsheetConfigServerBaseUri).build();
    }

    public void setSpreadsheetConfigServerBaseUri(String spreadsheetConfigServerBaseUri) {
        this.spreadsheetConfigServerBaseUri = spreadsheetConfigServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(spreadsheetConfigServerBaseUri).build();
    }

    public UUID createSpreadsheetConfigCollectionFromConfigIds(List<UUID> configIds) {
        Objects.requireNonNull(configIds);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + "/merge")
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(configIds)
            .retrieve()
            .body(UUID.class);
    }

    public UUID createSpreadsheetConfigCollection(String collection) {
        Objects.requireNonNull(collection);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH)
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(collection)
            .retrieve()
            .body(UUID.class);
    }

    public UUID duplicateSpreadsheetConfigCollection(UUID collectionId) {
        Objects.requireNonNull(collectionId);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + "/{uuid}/duplicate")
                .buildAndExpand(collectionId)
                .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    public void updateSpreadsheetConfigCollection(UUID collectionId, String collection) {
        Objects.requireNonNull(collectionId);
        Objects.requireNonNull(collection);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + collectionId)
                .buildAndExpand()
                .toUriString();

        restClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(collection)
            .retrieve()
            .toBodilessEntity();
    }

    public void replaceAllSpreadsheetConfigsInCollection(UUID collectionId, List<UUID> configIds) {
        Objects.requireNonNull(collectionId);
        Objects.requireNonNull(configIds);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + collectionId + "/spreadsheet-configs/replace-all")
                .buildAndExpand()
                .toUriString();

        restClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(configIds)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SPREADSHEET_CONFIG_COLLECTIONS_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        restClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }
}
