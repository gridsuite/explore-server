/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.controllers;

import org.gridsuite.explore.server.FilterController;
import org.gridsuite.explore.server.services.FilterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterControllerTest {

    private static final UUID FILTER_UUID = UUID.randomUUID();

    @Mock
    private FilterService filterService;

    @InjectMocks
    private FilterController controller;

    @Test
    void getFilterForwardsId() {
        String response = "{\"name\":\"filter\"}";
        when(filterService.getFilter(FILTER_UUID)).thenReturn(response);

        assertSame(response, controller.getFilter(FILTER_UUID).getBody());

        verify(filterService).getFilter(FILTER_UUID);
    }
}
