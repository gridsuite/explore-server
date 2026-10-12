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

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    private String studyConfigServerBaseUri;

    public WorkspaceService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.studyConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restClient = restClientBuilder.baseUrl(studyConfigServerBaseUri).build();
    }

    public void setStudyConfigServerBaseUri(String studyConfigServerBaseUri) {
        this.studyConfigServerBaseUri = studyConfigServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(studyConfigServerBaseUri).build();
    }

    public UUID duplicateWorkspace(UUID sourceWorkspaceId) {
        Objects.requireNonNull(sourceWorkspaceId);

        return restClient.post()
            .uri(WORKSPACES_PATH + "/{uuid}/duplicate", sourceWorkspaceId)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    public void replaceWorkspace(UUID workspaceId, UUID sourceWorkspaceId) {
        Objects.requireNonNull(workspaceId);
        Objects.requireNonNull(sourceWorkspaceId);

        restClient.put()
            .uri(uriBuilder -> uriBuilder
                .path(WORKSPACES_PATH + DELIMITER + "{uuid}/replace")
                .queryParam(DUPLICATE_FROM_PARAMETER, sourceWorkspaceId)
                .build(workspaceId))
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID workspaceUuid) {
        Objects.requireNonNull(workspaceUuid);

        restClient.delete()
            .uri(WORKSPACES_PATH + DELIMITER + "{uuid}", workspaceUuid)
            .retrieve()
            .toBodilessEntity();
    }
}
