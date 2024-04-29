/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final RestTemplate restTemplate;

    private DirectoryService directoryService;

    private final Map<ParametersType, String> genericParametersServices = Map.of(ParametersType.VOLTAGE_INIT_PARAMETERS, "voltage-init-server",
            ParametersType.SECURITY_ANALYSIS_PARAMETERS, "security-analysis-server",
            ParametersType.LOADFLOW_PARAMETERS, "loadflow-server",
            ParametersType.SENSITIVITY_PARAMETERS, "sensitivity-analysis-server");

    private RemoteServicesProperties remoteServicesProperties;

    @Autowired
    public ParametersService(RemoteServicesProperties remoteServicesProperties, @Lazy DirectoryService directoryService, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.remoteServicesProperties = remoteServicesProperties;
        this.directoryService = directoryService;
    }

    public UUID createParameters(String parameters, ParametersType parametersType) {
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType));
        Objects.requireNonNull(parameters);

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + "/parameters")
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
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType));
        Objects.requireNonNull(parameters);

        var path = UriComponentsBuilder
                .fromPath(DELIMITER + SERVER_API_VERSION + "/parameters/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(parameters, headers);

        restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.PUT, httpEntity, UUID.class);
    }

    public UUID duplicateParameters(UUID sourceParametersUuid, ParametersType parametersType) {
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType));
        Objects.requireNonNull(sourceParametersUuid);
        var path = UriComponentsBuilder
                    .fromPath(DELIMITER + SERVER_API_VERSION + "/parameters")
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
        String parametersServerBaseUri = remoteServicesProperties.getServiceUri(genericParametersServices.get(parametersType));
        String path = UriComponentsBuilder.fromPath(DELIMITER + SERVER_API_VERSION + "/parameters/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(parametersServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers),
                Void.class);
    }

}
