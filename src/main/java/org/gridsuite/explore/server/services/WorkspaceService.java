/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
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

    private final RestClient restClient;

    @Setter
    private String studyConfigServerBaseUri;

    public WorkspaceService(RestClient restClient, RemoteServicesProperties remoteServicesProperties) {
        this.studyConfigServerBaseUri = remoteServicesProperties.getServiceUri("study-config-server");
        this.restClient = restClient;
    }

    public UUID duplicateWorkspace(UUID sourceWorkspaceId) {
        Objects.requireNonNull(sourceWorkspaceId);

        return restClient.post()
            .uri(studyConfigServerBaseUri + WORKSPACES_PATH + "/{uuid}/duplicate", sourceWorkspaceId)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    public void replaceWorkspace(UUID workspaceId, UUID sourceWorkspaceId) {
        Objects.requireNonNull(workspaceId);
        Objects.requireNonNull(sourceWorkspaceId);

        restClient.put()
            .uri(studyConfigServerBaseUri + WORKSPACES_PATH + DELIMITER + "{uuid}/replace?" + DUPLICATE_FROM_PARAMETER + "={duplicateFrom}",
                workspaceId, sourceWorkspaceId)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID workspaceUuid) {
        Objects.requireNonNull(workspaceUuid);

        restClient.delete()
            .uri(studyConfigServerBaseUri + WORKSPACES_PATH + DELIMITER + "{uuid}", workspaceUuid)
            .retrieve()
            .toBodilessEntity();
    }
}
