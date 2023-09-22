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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class ParametersService implements IDirectoryElementsService {
    private final DirectoryService directoryService;

    private final Map<ParametersType, RestTemplate> genericParametersServices;

    @Autowired
    public ParametersService(RemoteServicesProperties remoteServicesProperties, DirectoryService directoryService, RestTemplateBuilder restTemplateBuilder) {
        genericParametersServices = Map.of(
                ParametersType.VOLTAGE_INIT_PARAMETERS, restTemplateBuilder.rootUri(remoteServicesProperties.getServiceUri(ParametersType.VOLTAGE_INIT_PARAMETERS.name().toLowerCase(Locale.ENGLISH)) + "/v1").build()
        );
        this.directoryService = directoryService;
    }

    public UUID createParameters(String parameters, ParametersType parametersType) {
        Objects.requireNonNull(parameters);
        var path = UriComponentsBuilder.fromPath("/parameters")
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(parameters, headers);
        return genericParametersServices.get(parametersType).exchange(path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public void updateParameters(UUID parametersUuid, String parameters, ParametersType parametersType) {
        Objects.requireNonNull(parameters);
        var path = UriComponentsBuilder.fromPath("/parameters/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(parameters, headers);
        genericParametersServices.get(parametersType).exchange(path, HttpMethod.PUT, httpEntity, UUID.class);
    }

    @Override
    public void delete(UUID parametersUuid, String userId) {
        ElementAttributes elementAttributes = directoryService.getElementInfos(parametersUuid);
        ParametersType parametersType = ParametersType.valueOf(elementAttributes.getType());
        String path = UriComponentsBuilder.fromPath("/parameters/{parametersUuid}")
                .buildAndExpand(parametersUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        genericParametersServices.get(parametersType).exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
