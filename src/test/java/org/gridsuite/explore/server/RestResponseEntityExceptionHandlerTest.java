/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RestResponseEntityExceptionHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private TestRestResponseEntityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestRestResponseEntityExceptionHandler();
    }

    @Test
    void mapsElementNotFoundToNotFoundStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/explore");
        ExploreException exception = new ExploreException(ExploreBusinessErrorCode.EXPLORE_ELEMENT_NOT_FOUND,
            "missing");

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertEquals("explore.elementNotFound", response.getBody().getBusinessErrorCode());
    }

    @Test
    void usesRemoteStatusAndChainWhenAvailable() throws JsonProcessingException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/explore/call");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.INTERNAL_SERVER_ERROR)
            .server("directory")
            .businessErrorCode("directory.remoteError")
            .detail("Directory failure")
            .path("/directory")
            .build();

        HttpClientErrorException exception = HttpClientErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "coucou",
            HttpHeaders.EMPTY,
            OBJECT_MAPPER.writeValueAsBytes(remote),
            null
        );

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertEquals("directory.remoteError", response.getBody().getBusinessErrorCode());
        assertThat(response.getBody().getChain()).hasSize(1);
    }

    @Test
    void wrapsInvalidRemotePayload() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/explore/remote");
        HttpClientErrorException exception = HttpClientErrorException.create(
            HttpStatus.BAD_GATEWAY,
            "coucou",
            null,
            "oops".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8
        );

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertNull(response.getBody().getBusinessErrorCode());
    }

    @Test
    void keepsRemoteStatusFromPayload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/explore/remote");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.BAD_GATEWAY)
            .server("directory")
            .businessErrorCode("directory.remoteError")
            .detail("bad gateway")
            .path("/directory")
            .build();

        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(remote);
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.BAD_GATEWAY, "bad gateway",
            null, payload, StandardCharsets.UTF_8);

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getChain()).hasSize(1);
    }

    private static final class TestRestResponseEntityExceptionHandler extends RestResponseEntityExceptionHandler {

        private TestRestResponseEntityExceptionHandler() {
            super(() -> "explore-server");
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleDomainException(ExploreException exception, MockHttpServletRequest request) {
            return super.handleDomainException(exception, request);
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleRemoteException(HttpClientErrorException exception, MockHttpServletRequest request) {
            return super.handleRemoteException(exception, request);
        }
    }
}
