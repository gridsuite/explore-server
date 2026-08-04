/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.gridsuite.explore.server.services.MonitorService;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitorServiceTest {

    private static final UUID PROCESS_CONFIG_UUID = UUID.randomUUID();
    private static final String PROCESS_CONFIG = "{\"name\":\"process config\"}";

    private WireMockServer wireMockServer;
    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        monitorService = new MonitorService(RestClient.builder(), new RemoteServicesProperties());
        monitorService.setMonitorServerBaseUri(wireMockServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void getProcessConfigForwardsProcessConfigUuid() {
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/process-configs/" + PROCESS_CONFIG_UUID))
            .willReturn(ok()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(PROCESS_CONFIG)));

        ResponseEntity<String> response = monitorService.getProcessConfig(PROCESS_CONFIG_UUID);

        assertEquals(PROCESS_CONFIG, response.getBody());
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/v1/process-configs/" + PROCESS_CONFIG_UUID)));
    }
}
