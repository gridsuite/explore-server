/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class MonitorService implements IDirectoryElementsService {

    private static final String MONITOR_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    public static final String PROCESS_CONFIGS_PATH = "process-configs";

    @Setter
    private String monitorServerBaseUri;

    private final RestTemplate restTemplate;

    public MonitorService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.monitorServerBaseUri = remoteServicesProperties.getServiceUri("monitor-server");
        this.restTemplate = restTemplate;
    }

    public UUID createProcessConfig(String processConfig) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + MONITOR_API_VERSION + DELIMITER + PROCESS_CONFIGS_PATH)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(monitorServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(processConfig, headers), UUID.class).getBody();
    }

    public void updateProcessConfig(UUID uuid, String processConfig) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + MONITOR_API_VERSION + DELIMITER + PROCESS_CONFIGS_PATH + DELIMITER + "{uuid}")
                .buildAndExpand(uuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(monitorServerBaseUri + path, HttpMethod.PUT, new HttpEntity<>(processConfig, headers), void.class);
    }

    public UUID duplicateProcessConfig(UUID sourceProcessConfigUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + MONITOR_API_VERSION + DELIMITER + PROCESS_CONFIGS_PATH + DELIMITER + "duplication")
                .queryParam("duplicateFrom", sourceProcessConfigUuid)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(monitorServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), UUID.class).getBody();
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + MONITOR_API_VERSION + DELIMITER + PROCESS_CONFIGS_PATH + DELIMITER + "{id}")
            .buildAndExpand(id)
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        restTemplate.exchange(monitorServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
