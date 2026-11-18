/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.diagram;

import lombok.Setter;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
@Service
public class SingleLineDiagramRestService {

    private static final String SINGLE_LINE_DIAGRAM_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH = DELIMITER + SINGLE_LINE_DIAGRAM_API_VERSION + DELIMITER + "network-area-diagram/config";

    @Setter
    String singleLineDiagramServerBaseUri;

    private final RestClient restClient;

    public SingleLineDiagramRestService(RemoteServicesProperties remoteServicesProperties, RestClient restClient) {
        this.singleLineDiagramServerBaseUri = remoteServicesProperties.getServiceUri("single-line-diagram-server");
        this.restClient = restClient;
    }

    public UUID createDiagramConfig(String diagramConfig) {
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
                .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH)
                .buildAndExpand()
                .toUriString();

        return restClient.post()
                .uri(singleLineDiagramServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(diagramConfig)
                .retrieve()
                .body(UUID.class);
    }

    public UUID duplicateDiagramConfig(UUID sourceUuid) {
        Objects.requireNonNull(sourceUuid);

        String path = UriComponentsBuilder.fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
                .buildAndExpand(sourceUuid)
                .toUriString();

        return restClient.post()
                .uri(singleLineDiagramServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(UUID.class);
    }

    public void updateDiagramConfig(UUID configUuid, String diagramConfig) {
        Objects.requireNonNull(configUuid);
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
                .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        restClient.put()
                .uri(singleLineDiagramServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(diagramConfig)
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
                .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
                .buildAndExpand()
                .toUriString();

        restClient.delete()
                .uri(singleLineDiagramServerBaseUri + path)
                .retrieve()
                .toBodilessEntity();
    }
}
