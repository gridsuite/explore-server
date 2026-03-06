/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.error.ExploreBusinessErrorCode;
import org.gridsuite.explore.server.error.ExploreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class ExploreExceptionTest {

    @Test
    void staticFactoryFormatsMessage() {
        ExploreException exception = ExploreException.of(ExploreBusinessErrorCode.EXPLORE_MAX_ELEMENTS_EXCEEDED,
            "Case %s failed", "demo");

        assertThat(exception.getMessage()).isEqualTo("Case demo failed");
        assertThat(exception.getBusinessErrorCode()).isEqualTo(ExploreBusinessErrorCode.EXPLORE_MAX_ELEMENTS_EXCEEDED);
    }

}
