/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.FilterService;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FilterServiceTest {

    private static final String BASE_URI = "http://filter-server";
    private static final UUID FILTER_UUID = UUID.randomUUID();
    private static final String FILTER = "{\"name\":\"filter\"}";

    private FilterService filterService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        filterService = new FilterService(restTemplate, new RemoteServicesProperties());
        filterService.setFilterServerBaseUri(BASE_URI);
    }

    @Test
    void getFilterForwardsFilterUuid() {
        server.expect(once(), requestTo(BASE_URI + "/v1/filters/" + FILTER_UUID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(FILTER, MediaType.APPLICATION_JSON));

        String response = filterService.getFilter(FILTER_UUID);

        assertEquals(FILTER, response);
        server.verify();
    }
}
