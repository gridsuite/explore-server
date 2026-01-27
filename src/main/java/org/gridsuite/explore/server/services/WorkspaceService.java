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

import java.util.Objects;
import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class WorkspaceService implements IDirectoryElementsService {

    private static final String WORKSPACES_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String WORKSPACES_PATH = DELIMITER + WORKSPACES_API_VERSION + DELIMITER + "workspaces";
    private static final String DUPLICATE_FROM_PARAMETER = "duplicateFrom";

    private final RestTemplate restTemplate;

    @Setter
    private String studyConfigServerBaseUri;

    public WorkspaceService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.studyConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restTemplate = restTemplate;
    }

    public UUID duplicateWorkspace(UUID sourceWorkspaceId) {
        Objects.requireNonNull(sourceWorkspaceId);

        var path = UriComponentsBuilder
                .fromPath(WORKSPACES_PATH)
                .queryParam(DUPLICATE_FROM_PARAMETER, sourceWorkspaceId)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(studyConfigServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public void replaceWorkspace(UUID workspaceId, UUID sourceWorkspaceId) {
        Objects.requireNonNull(workspaceId);
        Objects.requireNonNull(sourceWorkspaceId);

        var path = UriComponentsBuilder
                .fromPath(WORKSPACES_PATH + DELIMITER + workspaceId + "/replace")
                .queryParam(DUPLICATE_FROM_PARAMETER, sourceWorkspaceId)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        restTemplate.exchange(studyConfigServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }

    @Override
    public void delete(UUID workspaceUuid, String userId) {
        Objects.requireNonNull(workspaceUuid);

        var path = UriComponentsBuilder
                .fromPath(WORKSPACES_PATH + DELIMITER + workspaceUuid)
                .buildAndExpand()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(studyConfigServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
