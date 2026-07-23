/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.NetworkConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NetworkConversionServiceTest {

    private static final String BASE_URI = "http://network-conversion-server";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID EXPORT_UUID = UUID.randomUUID();
    private static final UUID CONVERSION_UUID = UUID.randomUUID();
    private static final String JSON = "{\"format\":\"CGMES\"}";

    private NetworkConversionService networkConversionService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        networkConversionService = new NetworkConversionService(BASE_URI, restTemplate);
    }

    @Test
    void getCaseImportParametersForwardsCaseUuid() {
        server.expect(once(), requestTo(BASE_URI + "/v1/cases/" + CASE_UUID + "/import-parameters"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = networkConversionService.getCaseImportParameters(CASE_UUID);

        assertEquals(JSON, response.getBody());
        server.verify();
    }

    @Test
    void convertCaseForwardsPathQueryHeaderAndBody() {
        server.expect(once(), requestTo(BASE_URI + "/v1/cases/" + CASE_UUID + "/convert/CGMES?fileName=network.zip"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("userId", "userId"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(JSON))
            .andRespond(withSuccess("\"" + CONVERSION_UUID + "\"", MediaType.APPLICATION_JSON));

        ResponseEntity<UUID> response = networkConversionService.convertCase(CASE_UUID, "CGMES", "network.zip", JSON, "userId");

        assertEquals(CONVERSION_UUID, response.getBody());
        server.verify();
    }

    @Test
    void downloadFileForwardsExportUuidAndReturnsHeadersAndBody() throws Exception {
        server.expect(once(), requestTo(BASE_URI + "/v1/download-file/" + EXPORT_UUID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("file", MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=network.zip"));

        ResponseEntity<Resource> response = networkConversionService.downloadFile(EXPORT_UUID);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals("attachment; filename=network.zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNotNull(response.getBody());
        assertEquals("file", new String(response.getBody().getContentAsByteArray()));
        server.verify();
    }

    @Test
    void getExportFormatsForwardsRequest() {
        server.expect(once(), requestTo(BASE_URI + "/v1/export/formats"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = networkConversionService.getExportFormats();

        assertEquals(JSON, response.getBody());
        server.verify();
    }
}
