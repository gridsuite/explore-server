/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaseServiceTest {

    private static final String BASE_URI = "http://case-server";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final String CASE_NAME = "network.xiidm";

    private CaseService caseService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        caseService = new CaseService(BASE_URI, restTemplate);
    }

    @Test
    void importCaseWithoutDirectoryElementCreationForwardsMultipartFileAndExpirationFlag() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(CASE_NAME);
        when(file.getResource()).thenReturn(new ByteArrayResource("case".getBytes()) {
            @Override
            public String getFilename() {
                return CASE_NAME;
            }
        });

        server.expect(once(), requestTo(BASE_URI + "/v1/cases"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("withExpiration")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("true")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("case")))
            .andRespond(withSuccess("\"" + CASE_UUID + "\"", MediaType.APPLICATION_JSON));

        UUID response = caseService.importCaseWithoutDirectoryElementCreation(file, true);

        assertEquals(CASE_UUID, response);
        server.verify();
    }

    @Test
    void downloadCaseForwardsCaseUuidAndReturnsHeadersAndBody() throws Exception {
        server.expect(once(), requestTo(BASE_URI + "/v1/cases/" + CASE_UUID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("case", MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=network.xiidm"));

        ResponseEntity<Resource> response = caseService.downloadCase(CASE_UUID);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals("attachment; filename=network.xiidm", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNotNull(response.getBody());
        assertEquals("case", new String(response.getBody().getContentAsByteArray()));
        server.verify();
    }

    @Test
    void deleteCaseForwardsCaseUuid() {
        server.expect(once(), requestTo(BASE_URI + "/v1/cases/" + CASE_UUID))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withSuccess());

        caseService.deleteCase(CASE_UUID);

        server.verify();
    }

    @Test
    void getBaseNameForwardsCaseName() {
        server.expect(once(), requestTo(BASE_URI + "/v1/cases/caseBaseName?caseName=" + CASE_NAME))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("network", MediaType.TEXT_PLAIN));

        String response = caseService.getBaseName(CASE_NAME);

        assertEquals("network", response);
        server.verify();
    }
}
