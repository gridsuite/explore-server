/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.AuthorizationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@TestConfiguration
public class AllowAllAuthorizationTestConfiguration {

    @Bean
    @Primary
    public AuthorizationService authorizationService() {
        return mock(
                AuthorizationService.class,
                invocation -> true
        );
    }
}
