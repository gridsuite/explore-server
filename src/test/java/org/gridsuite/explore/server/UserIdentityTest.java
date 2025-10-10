/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;
import org.gridsuite.explore.server.utils.WireMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreBusinessErrorCode.EXPLORE_ELEMENT_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * @author Sylvain BOUZOLS <sylvain.bouzols at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserIdentityTest {

    @Autowired
    protected MockMvc mockMvc;

    private WireMockServer wireMockServer;

    protected WireMockUtils wireMockUtils;

    @Autowired
    private UserIdentityService userIdentityService;

    @MockBean
    private DirectoryService directoryService;

    private static final String BASE_URL = "/v1/explore/elements/users-identities";
    private static final String USER_IDENTITY_SERVER_BASE_URL = "/v1/users";
    private static final String SUB = "user01";
    private static final String UNKNOWN_SUB = "user02";
    private static final String EXCEPTION_SUB = "user03";

    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final String ELEMENT_NAME = "Test Element";

    private static final UUID ELEMENT_NOT_FOUND_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UNKNOWN_SUB_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_EXCEPTION_SUB_UUID = UUID.randomUUID();
    private static final String ELEMENT_UNKNOWN_SUB_NAME = "Test Element Unknown sub";

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockUtils = new WireMockUtils(wireMockServer);
        wireMockServer.start();
        userIdentityService.setUserIdentityServerBaseUri(wireMockServer.baseUrl());

        when(directoryService.getElementsInfos(List.of(ELEMENT_UUID), null, SUB)).thenReturn(List.of(new ElementAttributes(
            ELEMENT_UUID,
            ELEMENT_NAME,
            "SOME TYPE",
            SUB,
            0L,
            null
        )));
        when(directoryService.getElementsInfos(List.of(ELEMENT_UNKNOWN_SUB_UUID), null, UNKNOWN_SUB)).thenReturn(List.of(new ElementAttributes(
            ELEMENT_UNKNOWN_SUB_UUID,
            ELEMENT_UNKNOWN_SUB_NAME,
            "SOME TYPE",
            UNKNOWN_SUB,
            0L,
            null
        )));
        when(directoryService.getElementsInfos(List.of(ELEMENT_EXCEPTION_SUB_UUID), null, EXCEPTION_SUB)).thenReturn(List.of(new ElementAttributes(
            ELEMENT_EXCEPTION_SUB_UUID,
            "exception",
            "SOME TYPE",
            EXCEPTION_SUB,
            0L,
            null
        )));
        when(directoryService.getElementsInfos(List.of(ELEMENT_NOT_FOUND_UUID), null, UNKNOWN_SUB))
                .thenThrow(ExploreException.of(EXPLORE_ELEMENT_NOT_FOUND, "Element '%s' not found", ELEMENT_NOT_FOUND_UUID));
    }

    protected Map<String, StringValuePattern> handleQueryParams(List<String> subs) {
        return Map.of("subs", WireMock.matching(subs.stream().map(sub -> ".+").collect(Collectors.joining(","))));
    }

    @Test
    void testGetSubIdentity() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(USER_IDENTITY_SERVER_BASE_URL + "/identities\\?subs=" + SUB))
                .willReturn(WireMock.ok()
                    .withBody("{sub: " + SUB + ", firstName: \"userFirstName\", lastName: \"userLastName\"}")
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))).getId();

        MvcResult mvcResult;
        String usersInfos;
        mvcResult = mockMvc.perform(get(BASE_URL)
                    .param("ids", ELEMENT_UUID.toString())
                    .header("userId", SUB))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
        usersInfos = mvcResult.getResponse().getContentAsString();
        assertTrue(usersInfos.contains("userFirstName"));
        assertTrue(usersInfos.contains("userLastName"));

        verify(directoryService, times(1)).getElementsInfos(List.of(ELEMENT_UUID), null, SUB);
        wireMockUtils.verifyGetRequest(stubId, USER_IDENTITY_SERVER_BASE_URL + "/identities", handleQueryParams(List.of(SUB)), false);
    }

    @Test
    void testGetSubIdentityNotFoundElement() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("ids", ELEMENT_NOT_FOUND_UUID.toString())
                        .header("userId", UNKNOWN_SUB))
                        .andExpect(status().isNotFound());

        verify(directoryService, times(1)).getElementsInfos(List.of(ELEMENT_NOT_FOUND_UUID), null, UNKNOWN_SUB);
    }

    @Test
    void testGetSubIdentityException() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(USER_IDENTITY_SERVER_BASE_URL + "/identities\\?subs=" + EXCEPTION_SUB))
                .willReturn(WireMock.serverError())).getId();

        mockMvc.perform(get(BASE_URL)
                .param("ids", ELEMENT_EXCEPTION_SUB_UUID.toString())
                .header("userId", EXCEPTION_SUB))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(ExploreException.class, result.getResolvedException()));

        verify(directoryService, times(1)).getElementsInfos(List.of(ELEMENT_EXCEPTION_SUB_UUID), null, EXCEPTION_SUB);
        wireMockUtils.verifyGetRequest(stubId, USER_IDENTITY_SERVER_BASE_URL + "/identities", handleQueryParams(List.of(EXCEPTION_SUB)), false);
    }
}
