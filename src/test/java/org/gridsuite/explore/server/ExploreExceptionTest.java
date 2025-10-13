/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class ExploreExceptionTest {

    @Test
    void staticFactoryFormatsMessage() {
        ExploreException exception = ExploreException.of(ExploreBusinessErrorCode.EXPLORE_IMPORT_CASE_FAILED,
            "Case %s failed", "demo");

        assertThat(exception.getMessage()).isEqualTo("Case demo failed");
        assertThat(exception.getBusinessErrorCode()).contains(ExploreBusinessErrorCode.EXPLORE_IMPORT_CASE_FAILED);
    }

    @Test
    void exposesRemoteErrorWhenPresent() {
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.BAD_GATEWAY)
            .server("downstream")
            .detail("failure")
            .timestamp(Instant.parse("2025-10-01T00:00:00Z"))
            .path("/remote")
            .build();

        ExploreException exception = new ExploreException(ExploreBusinessErrorCode.EXPLORE_REMOTE_ERROR,
            "wrapped", remote);

        assertThat(exception.getRemoteError()).contains(remote);
        assertThat(exception.getErrorCode()).contains(ExploreBusinessErrorCode.EXPLORE_REMOTE_ERROR);
    }
}
