/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.gridsuite.explore.server.error.ExploreBusinessErrorCode;
import org.gridsuite.explore.server.error.ExploreException;
import org.gridsuite.explore.server.error.ExploreExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class ExploreExceptionHandlerTest {

    private TestExploreExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestExploreExceptionHandler();
    }

    @Test
    void mapsElementNotFoundToNotFoundStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/explore");
        ExploreException exception = new ExploreException(ExploreBusinessErrorCode.EXPLORE_PERMISSION_DENIED,
            "denied");

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertEquals("explore.permissionDenied", response.getBody().getBusinessErrorCode());
    }

    private static final class TestExploreExceptionHandler extends ExploreExceptionHandler {

        private TestExploreExceptionHandler() {
            super(() -> "explore-server");
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleDomainException(ExploreException exception, MockHttpServletRequest request) {
            return super.handleDomainException(exception, request);
        }
    }
}
