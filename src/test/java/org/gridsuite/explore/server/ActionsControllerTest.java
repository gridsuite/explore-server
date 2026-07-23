/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.services.ContingencyListService;
import org.gridsuite.explore.server.utils.WireMockUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@AutoConfigureMockMvc
class ActionsControllerTest {

    private static final UUID CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final String EXPLORE_BASE_URL = "/v1/explore";
    private static final String ACTIONS_IDENTIFIER_CONTINGENCY_LIST_URL = "/v1/identifier-contingency-lists/" + CONTINGENCY_LIST_UUID;
    private static final String ACTIONS_FILTERS_CONTINGENCY_LIST_URL = "/v1/filters-contingency-lists/" + CONTINGENCY_LIST_UUID;
    private static final String CONTINGENCY_LIST_JSON = "{\"type\":\"IDENTIFIERS\",\"name\":\"contingency list\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContingencyListService contingencyListService;

    private WireMockServer wireMockServer;

    private WireMockUtils wireMockUtils;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockUtils = new WireMockUtils(wireMockServer);
        wireMockServer.start();
        contingencyListService.setActionsServerBaseUri(wireMockServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testGetIdentifierContingencyList() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(ACTIONS_IDENTIFIER_CONTINGENCY_LIST_URL))
                .willReturn(WireMock.ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(CONTINGENCY_LIST_JSON)))
                .getId();

        mockMvc.perform(get(EXPLORE_BASE_URL + "/identifier-contingency-lists/{id}", CONTINGENCY_LIST_UUID))
                .andExpect(status().isOk())
                .andExpect(content().json(CONTINGENCY_LIST_JSON));

        wireMockUtils.verifyGetRequest(stubId, ACTIONS_IDENTIFIER_CONTINGENCY_LIST_URL, Map.of(), false);
    }

    @Test
    void testGetFilterBasedContingencyList() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(ACTIONS_FILTERS_CONTINGENCY_LIST_URL))
                .willReturn(WireMock.ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(CONTINGENCY_LIST_JSON)))
                .getId();

        mockMvc.perform(get(EXPLORE_BASE_URL + "/filters-contingency-lists/{id}", CONTINGENCY_LIST_UUID))
                .andExpect(status().isOk())
                .andExpect(content().json(CONTINGENCY_LIST_JSON));

        wireMockUtils.verifyGetRequest(stubId, ACTIONS_FILTERS_CONTINGENCY_LIST_URL, Map.of(), false);
    }

    @Test
    void testGetContingencyListNotFound() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(ACTIONS_FILTERS_CONTINGENCY_LIST_URL))
                .willReturn(WireMock.notFound()))
                .getId();

        mockMvc.perform(get(EXPLORE_BASE_URL + "/filters-contingency-lists/{id}", CONTINGENCY_LIST_UUID))
                .andExpect(status().isNotFound());

        wireMockUtils.verifyGetRequest(stubId, ACTIONS_FILTERS_CONTINGENCY_LIST_URL, Map.of(), false);
    }
}
