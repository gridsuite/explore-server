/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.controllers;

import org.gridsuite.explore.server.controller.CaseController;
import org.gridsuite.explore.server.services.CaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

    private static final UUID CASE_UUID = UUID.randomUUID();

    @Mock
    private CaseService caseService;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private CaseController controller;

    @Test
    void importCaseForwardsFileAndExpirationFlag() {
        UUID response = CASE_UUID;
        when(caseService.importCaseWithoutDirectoryElementCreation(file, true)).thenReturn(response);

        assertSame(response, controller.importCase(file, true).getBody());

        verify(caseService).importCaseWithoutDirectoryElementCreation(file, true);
    }

    @Test
    void deleteCaseForwardsCaseUuid() {
        controller.deleteCase(CASE_UUID);

        verify(caseService).deleteCase(CASE_UUID);
    }

    // See comment in CaseController.java for the reason this test is commented out
    // @Test
    // void downloadCaseForwardsCaseUuid() {
    //     ResponseEntity<ByteArrayResource> response = ResponseEntity.ok(new ByteArrayResource("case".getBytes()));
    //     when(caseService.downloadCase(CASE_UUID)).thenReturn(ResponseEntity.ok(response.getBody()));

    //     assertSame(response.getBody(), controller.downloadCase(CASE_UUID).getBody());

    //     verify(caseService).downloadCase(CASE_UUID);
    // }

    @Test
    void getBaseNameForwardsCaseName() {
        String response = "case";
        when(caseService.getBaseName("case.xiidm")).thenReturn(response);

        assertSame(response, controller.getBaseName("case.xiidm").getBody());

        verify(caseService).getBaseName("case.xiidm");
    }
}
