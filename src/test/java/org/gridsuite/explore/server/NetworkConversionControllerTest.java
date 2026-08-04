/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.NetworkConversionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkConversionControllerTest {

    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID EXPORT_UUID = UUID.randomUUID();

    @Mock
    private NetworkConversionService networkConversionService;

    @InjectMocks
    private NetworkConversionController controller;

    @Test
    void getCaseImportParametersForwardsCaseUuid() {
        ResponseEntity<String> response = ResponseEntity.ok("{\"parameters\":[]}");
        when(networkConversionService.getCaseImportParameters(CASE_UUID)).thenReturn(response);

        assertSame(response, controller.getCaseImportParameters(CASE_UUID));

        verify(networkConversionService).getCaseImportParameters(CASE_UUID);
    }

    @Test
    void convertCaseForwardsArguments() {
        ResponseEntity<UUID> response = ResponseEntity.ok(EXPORT_UUID);
        when(networkConversionService.convertCase(CASE_UUID, "CGMES", "network.zip", "{}", "userId")).thenReturn(response);

        assertSame(response, controller.convertCase(CASE_UUID, "CGMES", "network.zip", "{}", "userId"));

        verify(networkConversionService).convertCase(CASE_UUID, "CGMES", "network.zip", "{}", "userId");
    }

    @Test
    void downloadFileForwardsExportUuid() {
        ByteArrayResource resource = new ByteArrayResource("file".getBytes());
        when(networkConversionService.downloadFile(EXPORT_UUID)).thenReturn(ResponseEntity.ok(resource));

        assertSame(resource, controller.downloadFile(EXPORT_UUID).getBody());

        verify(networkConversionService).downloadFile(EXPORT_UUID);
    }

    @Test
    void getExportFormatsDelegatesToService() {
        ResponseEntity<String> response = ResponseEntity.ok("[\"XIIDM\"]");
        when(networkConversionService.getExportFormats()).thenReturn(response);

        assertSame(response, controller.getExportFormats());

        verify(networkConversionService).getExportFormats();
    }
}
