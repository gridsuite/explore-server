/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
import org.springframework.web.client.RestTemplate;
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
    private static final String HEADER_USER_ID = "userId";
    private static final String DUPLICATE_FROM_PARAMETER = "duplicateFrom";
    private static final String COMPUTATION_PARAMETERS = "/parameters";
    private static final String NETWORK_VISU_PARAMETERS = "/network-visualizations-params";

    private final RestTemplate restTemplate;

    private final DirectoryService directoryService;

    @Getter
    @AllArgsConstructor
    private static class ParameterServerConfig {
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

    public ParametersService(RemoteServicesProperties remoteServicesProperties, @Lazy DirectoryService directoryService, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.remoteServicesProperties = remoteServicesProperties;
        this.directoryService = directoryService;
    }

    public UUID createParameters(String parameters, ParametersType parametersType) {
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType).getServerName());
        Objects.requireNonNull(parameters);

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + genericParametersServices.get(parametersType).getParametersBaseUrl())
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(parameters, headers);

        UUID parametersUuid;

        parametersUuid = restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();

        return parametersUuid;
    }

    public void updateParameters(UUID parametersUuid, String parameters, ParametersType parametersType) {
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType).getServerName());
        Objects.requireNonNull(parameters);

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + genericParametersServices.get(parametersType).getParametersBaseUrl() + "/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(parameters, headers);

        restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.PUT, httpEntity, UUID.class);
    }

    public UUID duplicateParameters(UUID sourceParametersUuid, ParametersType parametersType) {
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType).getServerName());
        Objects.requireNonNull(sourceParametersUuid);
        var path = UriComponentsBuilder
                    .fromPath(DELIMITER + SERVER_API_VERSION + genericParametersServices.get(parametersType).getParametersBaseUrl())
                    .queryParam(DUPLICATE_FROM_PARAMETER, sourceParametersUuid)
                    .buildAndExpand()
                    .toUriString();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    @Override
    public void delete(UUID parametersUuid, String userId) {
        ElementAttributes elementAttributes = directoryService.getElementInfos(parametersUuid);
        ParametersType parametersType = ParametersType.valueOf(elementAttributes.getType());
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType).getServerName());
        String path = UriComponentsBuilder.fromPath(DELIMITER + SERVER_API_VERSION + genericParametersServices.get(parametersType).getParametersBaseUrl() + "/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers),
                Void.class);
    }

}
