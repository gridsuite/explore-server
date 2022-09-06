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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class FilterService implements IDirectoryElementsService {
    private static final String FILTER_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private String filterServerBaseUri;

    private final RestTemplate restTemplate;

    @Autowired
    public FilterService(@Value("${backing-services.filter-server.base-uri:http://filter-server/}") String filterServerBaseUri, RestTemplate restTemplate) {
        this.filterServerBaseUri = filterServerBaseUri;
        this.restTemplate = restTemplate;
    }

    public void setFilterServerBaseUri(String filterServerBaseUri) {
        this.filterServerBaseUri = filterServerBaseUri;
    }

    @SuppressWarnings("checkstyle:WhitespaceAround")
    public void replaceFilterWithScript(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/replace-with-script")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.PUT, null, Void.class);
    }

    public void insertNewScriptFromFilter(UUID id, UUID newId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}/new-script?newId={newId}")
                .buildAndExpand(id, newId)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, null, Void.class);
    }

    public void delete(UUID id, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{id}")
                .buildAndExpand(id)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    public void insertFilter(String filter, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters?id={id}")
                .buildAndExpand(filterId)
                .toUriString();
        HttpHeaders headers = getHeaders(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(filter, headers);
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, httpEntity, Void.class);
    }

    public void insertFilter(UUID sourceFilterId, UUID filterId, String userId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters")
                .queryParam("duplicateFrom", sourceFilterId)
                .queryParam("id", filterId)
                .toUriString();
        restTemplate.exchange(filterServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(getHeaders(userId)), Void.class);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> filtersUuids) {
        var ids = filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();
        return restTemplate.exchange(filterServerBaseUri + path, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {
            }).getBody();

    }

    private HttpHeaders getHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }
}
