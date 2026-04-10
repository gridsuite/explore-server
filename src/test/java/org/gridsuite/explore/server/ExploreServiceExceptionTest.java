/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.ExploreService;
import org.gridsuite.explore.server.services.FilterService;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExploreServiceExceptionTest {

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private FilterService filterService;

    @Autowired
    private ExploreService exploreService;

    @MockitoBean
    private NetworkModificationService networkModificationService;

    @Test
    void testDirectoryServerCrashesWithFilter() {
        // creation
        String creatingErrorMessage = "error when creating element from directory server";
        when(directoryService.createElement(any(), any(), any())).thenThrow(new RuntimeException(creatingErrorMessage));
        doNothing().when(filterService).insertFilter(any(), any(), any());
        doNothing().when(filterService).delete(any(), any());
        UUID parentDirectoryUuid = UUID.randomUUID();
        String message = assertThrows(RuntimeException.class, () -> exploreService.createFilter("filterId",
                "filterName", "description", parentDirectoryUuid, "userId"))
                .getMessage();
        ArgumentCaptor<UUID> createdFilterId = ArgumentCaptor.forClass(UUID.class);
        verify(filterService, times(1)).insertFilter(any(), createdFilterId.capture(), eq("userId"));
        verify(filterService, times(1)).delete(createdFilterId.getValue(), "userId");
        assertEquals(creatingErrorMessage, message);
        reset(filterService);

        // duplication
        String duplicateErrorMessage = "error when duplicating element from directory server";
        when(directoryService.duplicateElement(any(), any(), any(), any())).thenThrow(new RuntimeException(duplicateErrorMessage));
        UUID duplicatedFilterId = UUID.randomUUID();
        when(filterService.duplicateFilter(any())).thenReturn(duplicatedFilterId);

        UUID sourceFilterId = UUID.randomUUID();
        UUID targetDirectoryId = UUID.randomUUID();
        message = assertThrows(RuntimeException.class, () -> exploreService.duplicateFilter(sourceFilterId, targetDirectoryId, "userId"))
                .getMessage();
        verify(filterService, times(1)).duplicateFilter(any());
        verify(filterService, times(1)).delete(eq(duplicatedFilterId), any());
        assertEquals(duplicateErrorMessage, message);
    }

    @Test
    void testDirectoryServerCrashesWithNetworkModification() {
        String creatingErrorMessage = "error when creating element from directory server";
        when(directoryService.createElementWithNewName(any(), any(), any(), anyBoolean())).thenThrow(new RuntimeException(creatingErrorMessage));
        UUID createdCompositeModificationId = UUID.randomUUID();
        when(networkModificationService.createCompositeModification(anyList())).thenReturn(createdCompositeModificationId);

        List<UUID> modificationUuids = List.of(UUID.randomUUID());
        UUID parentDirectoryUuid = UUID.randomUUID();
        String message = assertThrows(RuntimeException.class, () -> exploreService.createCompositeModification(modificationUuids,
                "userId", "name", "description", parentDirectoryUuid))
                .getMessage();
        verify(networkModificationService, times(1)).createCompositeModification(any());
        verify(networkModificationService, times(1)).delete(eq(createdCompositeModificationId), any());
        assertEquals(creatingErrorMessage, message);
    }

    @Test
    void testDirectoryServerCrashesAndDeleteElementToo() {
        // creation
        String creatingErrorMessage = "error when creating element from directory server";
        String deletingErrorMessage = "error when deleting filter element";
        when(directoryService.createElement(any(), any(), any())).thenThrow(new RuntimeException(creatingErrorMessage));
        doNothing().when(filterService).insertFilter(any(), any(), any());
        doThrow(new RuntimeException(deletingErrorMessage)).when(filterService).delete(any(), any());
        UUID parentDirectoryUuid = UUID.randomUUID();
        Throwable throwable = assertThrows(RuntimeException.class, () -> exploreService.createFilter("filterId",
                "filterName", "description", parentDirectoryUuid, "userId"));
        String message = throwable.getMessage();
        assertEquals(creatingErrorMessage, message);
        assertEquals(1, throwable.getSuppressed().length);
        assertEquals(deletingErrorMessage, throwable.getSuppressed()[0].getMessage());

        ArgumentCaptor<UUID> createdFilterId = ArgumentCaptor.forClass(UUID.class);
        verify(filterService, times(1)).insertFilter(any(), createdFilterId.capture(), eq("userId"));
        verify(filterService, times(1)).delete(createdFilterId.getValue(), "userId");
        reset(filterService);
    }
}
