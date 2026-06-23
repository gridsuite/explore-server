/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMappingService implements IDirectoryElementsService {

    private static final String DYNAMIC_MAPPING_API_VERSION = "";
    private static final String DELIMITER = "/";
    public static final String MAPPING_PATH = DELIMITER + "mappings";

    private String dynamicMappingServerBaseUri;

    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    public DynamicMappingService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.dynamicMappingServerBaseUri = remoteServicesProperties.getServiceUri("dynamic-mapping-server");
        this.restClient = restClientBuilder.baseUrl(dynamicMappingServerBaseUri + DELIMITER + DYNAMIC_MAPPING_API_VERSION).build();
    }

    public void setDynamicMappingServerBaseUri(String dynamicMappingServerBaseUri) {
        this.dynamicMappingServerBaseUri = dynamicMappingServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(dynamicMappingServerBaseUri + DELIMITER + DYNAMIC_MAPPING_API_VERSION).build();
    }

    public UUID createMapping(String mapping) {
        return restClient.post()
            .uri(MAPPING_PATH + DELIMITER)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapping)
            .retrieve()
            .body(UUID.class);
    }

    public void updateMapping(UUID uuid, String mapping) {
        restClient.put()
            .uri(MAPPING_PATH + DELIMITER + "{uuid}", uuid)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapping)
            .retrieve()
            .toBodilessEntity();
    }

    public UUID duplicateMapping(UUID sourceMappingUuid) {
        return restClient.post()
            .uri(uriBuilder ->
                uriBuilder.path(MAPPING_PATH + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
                    .build(sourceMappingUuid))
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        restClient.delete()
            .uri(MAPPING_PATH + DELIMITER + "{id}", id)
            .retrieve()
            .toBodilessEntity();
    }

}
