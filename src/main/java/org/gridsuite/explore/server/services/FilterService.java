/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class FilterService extends AbstractDirectoryElementsService {
    private static final String FILTER_SERVER_API_VERSION = "v1";

    @Autowired
    public FilterService(
            @Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String filterServerBaseUri,
            RestTemplate restTemplate) {
        super(filterServerBaseUri, restTemplate);
    }

    public void replaceFilterWithScript(UUID id, String userId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/replace-with-script")
                .buildAndExpand(id)
                .toUriString();
        HttpHeaders headers = getUserHeaders(userId);
        restTemplate.exchange(serverBaseUri + path, HttpMethod.PUT, new HttpEntity<>(headers), Void.class);
    }

    public void insertNewScriptFromFilter(UUID id, UUID newId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/new-script?newId={newId}")
                .buildAndExpand(id, newId)
                .toUriString();
        restTemplate.exchange(serverBaseUri + path, HttpMethod.POST, null, Void.class);
    }

    @Override
    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(serverBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(getUserHeaders(userId)),
                Void.class);
    }

    public void insertFilter(String filter, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters?id={id}")
                .buildAndExpand(filterId)
                .toUriString();
        HttpEntity<String> httpEntity = getHttpEntityWithHeaders(userId, filter);
        restTemplate.exchange(serverBaseUri + path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertFilter(UUID sourceFilterId, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters")
                .queryParam("duplicateFrom", sourceFilterId)
                .queryParam("id", filterId)
                .toUriString();
        restTemplate.exchange(serverBaseUri + path, HttpMethod.POST, new HttpEntity<>(getUserHeaders(userId)),
                Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> filtersUuids) {
        var ids = filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(serverBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    public void updateFilter(UUID id, String filter, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(serverBaseUri + path, HttpMethod.PUT, getHttpEntityWithHeaders(userId, filter), Void.class);
    }
}
