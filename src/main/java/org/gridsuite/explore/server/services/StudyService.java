/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.apache.commons.lang3.StringUtils;
import org.gridsuite.explore.server.dto.NodeInfos;
import org.gridsuite.explore.server.dto.TreeExportInfos;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class StudyService implements IDirectoryElementsService {
    private static final String STUDY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private static final String NOTIFICATION_TYPE_METADATA_UPDATED = "metadata_updated";
    private final RestClient restClient;
    private String studyServerBaseUri;

    public StudyService(RestClient restClient, RemoteServicesProperties remoteServicesProperties) {
        this.studyServerBaseUri = remoteServicesProperties.getServiceUri("study-server");
        this.restClient = restClient;
    }

    public void setStudyServerBaseUri(String studyServerBaseUri) {
        this.studyServerBaseUri = studyServerBaseUri;
    }

    public void insertStudyWithExistingCaseFile(UUID studyUuid, UUID caseUuid, String caseFormat,
            Map<String, Object> importParams, Boolean duplicateCase, String firstRootNetworkName) {
        var uriComponentsBuilder = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/cases/{caseUuid}")
            .queryParam("studyUuid", studyUuid)
            .queryParam("duplicateCase", duplicateCase)
            .queryParam("caseFormat", caseFormat);

        if (!StringUtils.isBlank(firstRootNetworkName)) {
            uriComponentsBuilder.queryParam("firstRootNetworkName", firstRootNetworkName);
        }

        String path = uriComponentsBuilder.buildAndExpand(caseUuid).toUriString();
        var request = restClient.post()
            .uri(studyServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON);
        if (importParams != null) {
            request.body(importParams);
        }

        request.retrieve().toBodilessEntity();
    }

    public UUID duplicateStudy(UUID studyId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                "/studies/{uuid}/duplicate")
            .buildAndExpand(studyId)
            .toUriString();

        return restClient.post()
            .uri(studyServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    @Override
    public void delete(UUID studyUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}")
                .buildAndExpand(studyUuid)
                .toUriString();
        restClient.delete()
            .uri(studyServerBaseUri + path)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        var ids = studiesUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(studyServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public List<NodeInfos> getNodesInfos(List<UUID> nodeUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/nodes/infos")
                .queryParam("ids", nodeUuids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(studyServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public ResponseEntity<Void> notifyStudyUpdate(UUID studyUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION +
                        "/studies/{studyUuid}/notification?type={metadata_updated}")
                .buildAndExpand(studyUuid, NOTIFICATION_TYPE_METADATA_UPDATED)
                .toUriString();

        return restClient.post()
            .uri(studyServerBaseUri + path)
            .retrieve()
            .toBodilessEntity();
    }

    public void importStudy(TreeExportInfos treeExportInfos) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/import").toUriString();

        restClient.post()
                    .uri(studyServerBaseUri + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(treeExportInfos)
                    .retrieve()
                    .toBodilessEntity();
    }

    public void setStudyParameters(UUID studyUuid, String computationPath, String parameters) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + STUDY_SERVER_API_VERSION + "/studies/{studyUuid}/{computationPath}/parameters")
                .buildAndExpand(studyUuid, computationPath)
                .toUriString();

        restClient.post()
                    .uri(studyServerBaseUri + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(parameters)
                    .retrieve()
                    .toBodilessEntity();
    }
}
