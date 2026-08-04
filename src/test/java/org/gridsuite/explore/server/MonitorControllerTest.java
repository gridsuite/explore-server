/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.MonitorService;
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
class MonitorControllerTest {

    private static final UUID PROCESS_CONFIG_UUID = UUID.randomUUID();

    @Mock
    private MonitorService monitorService;

    @InjectMocks
    private MonitorController controller;

    @Test
    void getProcessConfigForwardsId() {
        String response = "{\"name\":\"process config\"}";
        when(monitorService.getProcessConfig(PROCESS_CONFIG_UUID)).thenReturn(response);

        assertSame(response, controller.getProcessConfig(PROCESS_CONFIG_UUID).getBody());

        verify(monitorService).getProcessConfig(PROCESS_CONFIG_UUID);
    }
}
