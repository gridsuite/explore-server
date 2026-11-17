/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.gridsuite.explore.server.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.explore.server.ExploreConstants.HEADER_USER_ID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@Import(AllowAllAuthorizationTestConfiguration.class)
class CompositeModificationApplicabilityTest {

    private static final String APPLICABILITY_PATH = "/v1/network-modifications/root-network-applicability";
    private static final String METADATA_PATH = "/v1/network-modifications";
    private static final String ACTIVATION = "{\"activated\":false,\"type\":\"LOAD_CREATION\"}";
    private static final UUID COMPOSITE_UUID = UUID.randomUUID();
    private static final UUID FIRST_UUID = UUID.randomUUID();
    private static final UUID SECOND_UUID = UUID.randomUUID();
    private static final String USER_ID = "userId";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NetworkModificationService networkModificationService;

    @MockitoBean
    private DirectoryService directoryService;

    @Autowired
    private OutputDestination output;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        networkModificationService.setNetworkModificationServerBaseUri(wireMockServer.baseUrl());
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathEqualTo(APPLICABILITY_PATH)).willReturn(WireMock.ok()));
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathEqualTo(METADATA_PATH)).willReturn(WireMock.ok()));
        output.clear();
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }

    @Test
    void testUpdateRootNetworkApplicability() throws Exception {
        mockMvc.perform(put("/v1/explore/composite-modifications/{id}/root-network-applicability", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("uuids", FIRST_UUID.toString(), SECOND_UUID.toString())
                        .param("rootNetworkTag", "R1")
                        .param("applicable", "false"))
                .andExpect(status().isOk());

        wireMockServer.verify(1, WireMock.putRequestedFor(WireMock.urlPathEqualTo(APPLICABILITY_PATH))
                .withQueryParam("uuids", WireMock.havingExactly(FIRST_UUID.toString(), SECOND_UUID.toString()))
                .withQueryParam("rootNetworkTag", WireMock.equalTo("R1"))
                .withQueryParam("applicable", WireMock.equalTo("false"))
                .withHeader(HEADER_USER_ID, WireMock.equalTo(USER_ID)));
        TestUtils.assertElementUpdatedSent(output, COMPOSITE_UUID, USER_ID);
    }

    @Test
    void testUpdateRootNetworkApplicabilityFailureIsNotNotified() throws Exception {
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathEqualTo(APPLICABILITY_PATH)).willReturn(WireMock.forbidden()));

        mockMvc.perform(put("/v1/explore/composite-modifications/{id}/root-network-applicability", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("uuids", FIRST_UUID.toString())
                        .param("rootNetworkTag", "R1")
                        .param("applicable", "true"))
                .andExpect(status().isForbidden());

        assertNull(output.receive(100, "element.update"));
    }

    @Test
    void testUpdateModificationsMetadata() throws Exception {
        mockMvc.perform(put("/v1/explore/composite-modifications/{id}/network-modifications/metadata", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("uuids", FIRST_UUID.toString(), SECOND_UUID.toString())
                        .contentType(APPLICATION_JSON)
                        .content(ACTIVATION))
                .andExpect(status().isOk());

        wireMockServer.verify(1, WireMock.putRequestedFor(WireMock.urlPathEqualTo(METADATA_PATH))
                .withQueryParam("uuids", WireMock.havingExactly(FIRST_UUID.toString(), SECOND_UUID.toString()))
                .withHeader(HEADER_USER_ID, WireMock.equalTo(USER_ID))
                .withRequestBody(WireMock.equalToJson(ACTIVATION)));
        TestUtils.assertElementUpdatedSent(output, COMPOSITE_UUID, USER_ID);
    }

    @Test
    void testUpdateModificationsMetadataFailureIsNotNotified() throws Exception {
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathEqualTo(METADATA_PATH)).willReturn(WireMock.forbidden()));

        mockMvc.perform(put("/v1/explore/composite-modifications/{id}/network-modifications/metadata", COMPOSITE_UUID)
                        .header(HEADER_USER_ID, USER_ID)
                        .param("uuids", FIRST_UUID.toString())
                        .contentType(APPLICATION_JSON)
                        .content(ACTIVATION))
                .andExpect(status().isForbidden());

        assertNull(output.receive(100, "element.update"));
    }
}
