/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.filter;

import lombok.Setter;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
@Service
public class FilterRestService {
    private static final String FILTER_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";
    private static final String FILTERS_ID_URL = "/filters/{id}";

    @Setter
    private String filterServerBaseUri;

    private final RestClient restClient;

    public FilterRestService(RestClient restClient, RemoteServicesProperties remoteServicesProperties) {
        this.restClient = restClient;
        this.filterServerBaseUri = remoteServicesProperties.getServiceUri("filter-server");
    }

    public void insertFilter(String filter, UUID filterId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters?id={id}")
                .buildAndExpand(filterId)
                .toUriString();

        restClient.post()
                .uri(filterServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(filter)
                .retrieve()
                .toBodilessEntity();
    }

    public UUID duplicateFilter(UUID filterId) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/{uuid}/duplicate")
                .buildAndExpand(filterId)
                .toUriString();

        return restClient.post()
                .uri(filterServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(UUID.class);
    }

    public void updateFilter(UUID id, String filter) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + FILTERS_ID_URL)
                .buildAndExpand(id)
                .toUriString();

        restClient.put()
                .uri(filterServerBaseUri + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(filter)
                .retrieve()
                .toBodilessEntity();
    }

    public String getFilter(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + FILTERS_ID_URL)
                .buildAndExpand(id)
                .toUriString();

        return restClient.get()
                .uri(filterServerBaseUri + path)
                .retrieve()
                .body(String.class);
    }

    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_SERVER_API_VERSION + FILTERS_ID_URL)
                .buildAndExpand(id)
                .toUriString();

        restClient.delete()
                .uri(filterServerBaseUri + path)
                .retrieve()
                .toBodilessEntity();
    }

    public List<Map<String, Object>> getMetadata(List<UUID> filtersUuids) {
        var ids = filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_SERVER_API_VERSION + "/filters/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
                .uri(filterServerBaseUri + path)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
    }
}
