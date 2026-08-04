/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.CaseInfo;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionDTO;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.ExploreService;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExploreControllerTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();
    private static final UUID DIRECTORY_ID = UUID.randomUUID();
    private static final String USER_ID = "userId";
    private static final String BODY = "{\"name\":\"value\"}";

    @Mock
    private ExploreService exploreService;

    @Mock
    private DirectoryService directoryService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ExploreController controller;

    @Test
    void createStudyAssertsCaseCreationAndDelegates() {
        Map<String, Object> importParams = Map.of("parameter", "value");

        assertEquals(HttpStatus.OK, controller.createStudy("study", ID, "XIIDM", true, "description", DIRECTORY_ID, USER_ID, importParams).getStatusCode());

        verify(exploreService).assertCanCreateCase(USER_ID);
        verify(exploreService).createStudy("study", new CaseInfo(ID, "XIIDM"), "description", USER_ID, DIRECTORY_ID, importParams, true);
    }

    @Test
    void createCaseAssertsCaseCreationAndDelegates() {
        assertEquals(HttpStatus.OK, controller.createCase("case", multipartFile, "description", DIRECTORY_ID, USER_ID).getStatusCode());

        verify(exploreService).assertCanCreateCase(USER_ID);
        verify(exploreService).createCase("case", multipartFile, "description", USER_ID, DIRECTORY_ID);
    }

    @Test
    void duplicateAndUpdateEndpointsDelegateToExploreService() {
        List<UUID> ids = List.of(ID, SECOND_ID);
        ElementAttributes elementAttributes = new ElementAttributes();

        assertEquals(HttpStatus.OK, controller.duplicateContingencyList(ID, ContingencyListType.IDENTIFIERS, DIRECTORY_ID, USER_ID).getStatusCode());
        assertEquals(HttpStatus.OK, controller.updateParameters(ID, BODY, ParametersType.LOADFLOW_PARAMETERS, USER_ID, "name", "description").getStatusCode());
        assertEquals(HttpStatus.OK, controller.updateElement(ID, elementAttributes, USER_ID).getStatusCode());
        assertEquals(HttpStatus.OK, controller.moveElementsDirectory(DIRECTORY_ID, ids, USER_ID).getStatusCode());

        verify(exploreService).duplicateContingencyList(ID, DIRECTORY_ID, USER_ID, ContingencyListType.IDENTIFIERS);
        verify(exploreService).updateParameters(ID, BODY, ParametersType.LOADFLOW_PARAMETERS, USER_ID, "name", "description");
        verify(exploreService).updateElement(ID, elementAttributes, USER_ID);
        verify(exploreService).moveElementsDirectory(ids, DIRECTORY_ID, USER_ID);
    }

    @Test
    void directoryReadEndpointsDelegateToDirectoryService() {
        List<UUID> ids = List.of(ID);
        List<ElementAttributes> metadata = List.of(new ElementAttributes(ID, "element", "CASE", USER_ID, 0L, "description"));
        Map<UUID, String> names = Map.of(ID, "element");

        when(directoryService.getElementsMetadata(ids, List.of("CASE"), List.of("LOAD"), USER_ID)).thenReturn(metadata);
        when(directoryService.getElementsName(ids)).thenReturn(names);
        when(directoryService.getRootDirectories(List.of("CASE"), USER_ID)).thenReturn("[{\"type\":\"CASE\"}]");

        assertSame(metadata, controller.getElementsMetadata(ids, List.of("LOAD"), List.of("CASE"), USER_ID).getBody());
        assertSame(names, controller.getElementsName(ids).getBody());
        assertEquals("[{\"type\":\"CASE\"}]", controller.getRootDirectories(List.of("CASE"), USER_ID).getBody());

        verify(directoryService).getElementsMetadata(ids, List.of("CASE"), List.of("LOAD"), USER_ID);
        verify(directoryService).getElementsName(ids);
        verify(directoryService).getRootDirectories(List.of("CASE"), USER_ID);
    }

    @Test
    void directoryWriteEndpointsDelegateToDirectoryService() {
        ElementAttributes elementAttributes = new ElementAttributes(ID, "directory", "DIRECTORY", USER_ID, 0L, "description");
        List<PermissionDTO> permissions = List.of(new PermissionDTO(true, List.of(), PermissionType.READ));

        when(directoryService.createRootDirectory(BODY, USER_ID)).thenReturn(BODY);
        when(directoryService.createElement(elementAttributes, DIRECTORY_ID, USER_ID)).thenReturn(elementAttributes);

        assertEquals(BODY, controller.createRootDirectory(BODY, USER_ID).getBody());
        assertSame(elementAttributes, controller.createDirectory(DIRECTORY_ID, elementAttributes, USER_ID).getBody());
        assertEquals(HttpStatus.OK, controller.setDirectoryPermissions(DIRECTORY_ID, permissions, USER_ID).getStatusCode());

        verify(directoryService).createRootDirectory(BODY, USER_ID);
        verify(directoryService).createElement(elementAttributes, DIRECTORY_ID, USER_ID);
        verify(directoryService).setDirectoryPermissions(DIRECTORY_ID, permissions, USER_ID);
    }

    @Test
    void uuidCreationEndpointsReturnServiceResult() {
        when(exploreService.createProcessConfig("process", BODY, "description", USER_ID, DIRECTORY_ID)).thenReturn(ID);
        when(exploreService.duplicateProcessConfig(ID, DIRECTORY_ID, USER_ID)).thenReturn(SECOND_ID);
        when(exploreService.createDynamicMapping("dynamic", BODY, "description", USER_ID, DIRECTORY_ID)).thenReturn(ID);

        assertEquals(ID, controller.createProcessConfig("process", "description", DIRECTORY_ID, USER_ID, BODY).getBody());
        assertEquals(SECOND_ID, controller.duplicateProcessConfig(ID, DIRECTORY_ID, USER_ID).getBody());
        assertEquals(ID, controller.createDynamicMapping("dynamic", "description", DIRECTORY_ID, USER_ID, BODY).getBody());

        verify(exploreService).createProcessConfig("process", BODY, "description", USER_ID, DIRECTORY_ID);
        verify(exploreService).duplicateProcessConfig(ID, DIRECTORY_ID, USER_ID);
        verify(exploreService).createDynamicMapping("dynamic", BODY, "description", USER_ID, DIRECTORY_ID);
    }
}
