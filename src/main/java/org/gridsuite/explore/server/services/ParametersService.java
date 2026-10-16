/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class ParametersService implements IDirectoryElementsService {
    private static final String SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private static final String COMPUTATION_PARAMETERS = "/parameters";
    private static final String NETWORK_VISU_PARAMETERS = "/network-visualizations-params";

    private final RestClient restClient;

    private final DirectoryService directoryService;

    @Getter
    @AllArgsConstructor
    private static final class ParameterServerConfig {
        private String serverName;
        private String parametersBaseUrl;
    }

    private final Map<ParametersType, ParameterServerConfig> genericParametersServices = Map.of(
            ParametersType.VOLTAGE_INIT_PARAMETERS, new ParameterServerConfig("voltage-init-server", COMPUTATION_PARAMETERS),
            ParametersType.SECURITY_ANALYSIS_PARAMETERS, new ParameterServerConfig("security-analysis-server", COMPUTATION_PARAMETERS),
            ParametersType.LOADFLOW_PARAMETERS, new ParameterServerConfig("loadflow-server", COMPUTATION_PARAMETERS),
            ParametersType.SENSITIVITY_PARAMETERS, new ParameterServerConfig("sensitivity-analysis-server", COMPUTATION_PARAMETERS),
            ParametersType.SHORT_CIRCUIT_PARAMETERS, new ParameterServerConfig("shortcircuit-server", COMPUTATION_PARAMETERS),
            ParametersType.PCC_MIN_PARAMETERS, new ParameterServerConfig("pcc-min-server", COMPUTATION_PARAMETERS),
            ParametersType.NETWORK_VISUALIZATIONS_PARAMETERS, new ParameterServerConfig("study-config-server", NETWORK_VISU_PARAMETERS));

    private final RemoteServicesProperties remoteServicesProperties;

    public ParametersService(RemoteServicesProperties remoteServicesProperties, @Lazy DirectoryService directoryService, RestClient restClient) {
        this.restClient = restClient;
        this.remoteServicesProperties = remoteServicesProperties;
        this.directoryService = directoryService;
    }

    public UUID createParameters(String parameters, ParametersType parametersType) {
        Objects.requireNonNull(parameters);

        ParameterServerConfig serverConfig = genericParametersServices.get(parametersType);
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(serverConfig.getServerName());

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + serverConfig.getParametersBaseUrl())
                .buildAndExpand()
                .toUriString();

        return restClient
            .post()
            .uri(parametersServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(parameters)
            .retrieve()
            .body(UUID.class);
    }

    public void updateParameters(UUID parametersUuid, String parameters, ParametersType parametersType) {
        Objects.requireNonNull(parametersUuid);
        Objects.requireNonNull(parameters);

        ParameterServerConfig serverConfig = genericParametersServices.get(parametersType);
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(serverConfig.getServerName());

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + serverConfig.getParametersBaseUrl() + "/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        restClient
            .put()
            .uri(parametersServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(parameters)
            .retrieve()
            .toBodilessEntity();
    }

    public UUID duplicateParameters(UUID sourceParametersUuid, ParametersType parametersType) {
        Objects.requireNonNull(sourceParametersUuid);

        ParameterServerConfig serverConfig = genericParametersServices.get(parametersType);
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(serverConfig.getServerName());

        var path = UriComponentsBuilder
                    .fromPath(DELIMITER + SERVER_API_VERSION + serverConfig.getParametersBaseUrl() + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
                    .buildAndExpand(sourceParametersUuid)
                    .toUriString();

        return restClient
            .post()
            .uri(parametersServerBaseUri + path)
            .retrieve()
            .body(UUID.class);
    }

    @Override
    public void delete(UUID parametersUuid) {
        ElementAttributes elementAttributes = directoryService.getElementInfos(parametersUuid);
        ParametersType parametersType = ParametersType.valueOf(elementAttributes.getType());
        ParameterServerConfig serverConfig = genericParametersServices.get(parametersType);
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(serverConfig.getServerName());

        String path = UriComponentsBuilder.fromPath(DELIMITER + SERVER_API_VERSION + serverConfig.getParametersBaseUrl() + "/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        restClient
            .delete()
            .uri(parametersServerBaseUri + path)
            .retrieve()
            .toBodilessEntity();
    }
}
