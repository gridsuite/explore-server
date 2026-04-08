/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class MonitorService implements IDirectoryElementsService {

    private static final String MONITOR_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    public static final String PROCESS_CONFIGS_PATH = DELIMITER + "process-configs";

    private String monitorServerBaseUri;

    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    public MonitorService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.monitorServerBaseUri = remoteServicesProperties.getServiceUri("monitor-server");
        this.restClient = restClientBuilder.baseUrl(monitorServerBaseUri + DELIMITER + MONITOR_API_VERSION).build();
    }

    public void setMonitorServerBaseUri(String monitorServerBaseUri) {
        this.monitorServerBaseUri = monitorServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(monitorServerBaseUri + DELIMITER + MONITOR_API_VERSION).build();
    }

    public UUID createProcessConfig(String processConfig) {
        return restClient.post()
            .uri(PROCESS_CONFIGS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(processConfig)
            .retrieve()
            .body(UUID.class);
    }

    public void updateProcessConfig(UUID uuid, String processConfig) {
        restClient.put()
            .uri(PROCESS_CONFIGS_PATH + DELIMITER + "{uuid}", uuid)
            .contentType(MediaType.APPLICATION_JSON)
            .body(processConfig)
            .retrieve()
            .toBodilessEntity();
    }

    public UUID duplicateProcessConfig(UUID sourceProcessConfigUuid) {
        return restClient.post()
            .uri(uriBuilder ->
                uriBuilder.path(PROCESS_CONFIGS_PATH + DELIMITER + "duplication")
                    .queryParam("duplicateFrom", sourceProcessConfigUuid)
                    .build())
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        restClient.delete()
            .uri(PROCESS_CONFIGS_PATH + DELIMITER + "{id}", id)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> processConfigUuids) {
        return restClient.get()
            .uri(uriBuilder ->
                uriBuilder.path(PROCESS_CONFIGS_PATH + DELIMITER + "metadata")
                    .queryParam("ids", processConfigUuids)
                    .build())
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }
}
