/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Kevin Le Saulnier <kevin.lesaulnier at rte-france.com>
 */
@Service
public class SupervisionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionService.class);
    private final DirectoryService directoryService;
    private final String directoryServerBaseUri;
    private final RestTemplate restTemplate;

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SUPERVISION_PATH = DELIMITER + "supervision";
    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + SUPERVISION_PATH + DELIMITER
        + "elements";

    public SupervisionService(DirectoryService directoryService, RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.directoryServerBaseUri = remoteServicesProperties.getServiceUri("directory-server");
        this.directoryService = directoryService;
        this.restTemplate = restTemplate;
    }

    public void deleteElements(List<UUID> uuids, String userId) {
        uuids.forEach(id -> {
            try {
                directoryService.deleteElement(id, userId);
            } catch (Exception e) {
                // if deletion fails (element does not exist, server is down...), the process keeps proceeding to at least delete references in directory-server
                // orphan elements will be deleted in a dedicated script
                LOGGER.error(e.toString(), e);
            }
        });
        deleteDirectoryElements(uuids);
    }

    // DOES NOT CHECK OWNER BEFORE DELETING
    private void deleteDirectoryElements(List<UUID> elementUuids) {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH)
            .queryParam("ids", ids)
            .buildAndExpand()
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}
