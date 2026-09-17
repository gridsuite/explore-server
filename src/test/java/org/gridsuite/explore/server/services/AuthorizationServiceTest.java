/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.PermissionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UUID_2 = UUID.randomUUID();
    private static final UUID TARGET_DIRECTORY_UUID = UUID.randomUUID();

    @Mock
    private DirectoryService directoryService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void shouldAllowReadElement() {
        boolean result = authorizationService.canRead(ELEMENT_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.READ);
    }

    @Test
    void shouldPropagateExceptionWhenReadElementPermissionDenied() {
        doThrow(new AccessDeniedException("Read access denied"))
                .when(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.READ);

        assertThatThrownBy(() -> authorizationService.canRead(ELEMENT_UUID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowReadElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        boolean result = authorizationService.canRead(elementUuids);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission( elementUuids, null, PermissionType.READ);
    }

    @Test
    void shouldPropagateExceptionWhenReadElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doThrow(new AccessDeniedException("Read access denied"))
                .when(directoryService).checkPermission(elementUuids, null, PermissionType.READ);

        assertThatThrownBy(() -> authorizationService.canRead(elementUuids))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowWriteElement() {
        boolean result = authorizationService.canWrite(ELEMENT_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.WRITE);
    }

    @Test
    void shouldPropagateExceptionWhenWriteElementPermissionDenied() {
        doThrow(new AccessDeniedException("Write access denied"))
                .when(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE);

        assertThatThrownBy(() -> authorizationService.canWrite(ELEMENT_UUID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowWriteElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        boolean result = authorizationService.canWrite(elementUuids);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(elementUuids,null, PermissionType.WRITE);
    }

    @Test
    void shouldPropagateExceptionWhenWriteElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doThrow(new AccessDeniedException("Write access denied"))
                .when(directoryService).checkPermission(elementUuids, null, PermissionType.WRITE);

        assertThatThrownBy(() -> authorizationService.canWrite(elementUuids))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowDuplicateElementTo() {
        boolean result = authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.READ);
        verify(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID),null,PermissionType.WRITE);
    }

    @Test
    void shouldAllowDuplicateElementToWhenTargetDirectoryUuidIsNull() {
        boolean result = authorizationService.canDuplicateTo(ELEMENT_UUID, null);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.READ);
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null,PermissionType.WRITE);
    }

    @Test void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementWithoutReadPermission() {
        doThrow(new AccessDeniedException("Access denied"))
                .when(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.READ);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID))
                .isInstanceOf(AccessDeniedException.class);
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null,PermissionType.READ);
        verify(directoryService, never()).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);
    }

    @Test void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementWithoutWritePermissionOnTargetDirectory() {
        doNothing().when(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.READ);
        doThrow(new AccessDeniedException("Access denied"))
                .when(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID))
                .isInstanceOf(AccessDeniedException.class);
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID),null, PermissionType.READ);
        verify(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);
    }

    @Test
    void shouldAllowDuplicateElementsTo() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        boolean result = authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(elementUuids,null, PermissionType.READ);
        verify(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);
    }

    @Test void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementsWithoutReadPermission() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doThrow(new AccessDeniedException("Access denied"))
                .when(directoryService).checkPermission(elementUuids,null, PermissionType.READ);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(AccessDeniedException.class);
        verify(directoryService).checkPermission(elementUuids,null, PermissionType.READ);
        verify(directoryService, never()).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);
    }

    @Test void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementsWithoutWritePermissionOnTargetDirectory() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doNothing().when(directoryService).checkPermission(elementUuids,null, PermissionType.READ);
        doThrow(new AccessDeniedException("Access denied"))
                .when(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(AccessDeniedException.class);
        verify(directoryService).checkPermission(elementUuids,null, PermissionType.READ);
        verify(directoryService).checkPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE);
    }

    @Test
    void shouldAllowDeleteElement() {
        boolean result = authorizationService.canDelete(ELEMENT_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, true);
    }

    @Test
    void shouldPropagateExceptionWhenDeleteElementPermissionDenied() {
        doThrow(new AccessDeniedException("Write access denied"))
                .when(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, true);

        assertThatThrownBy(() -> authorizationService.canDelete(ELEMENT_UUID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowDeleteElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        boolean result = authorizationService.canDelete(elementUuids);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(elementUuids, null, PermissionType.WRITE, true);
    }

    @Test
    void shouldPropagateExceptionWhenDeleteElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doThrow(new AccessDeniedException("Write access denied"))
                .when(directoryService).checkPermission(elementUuids, null, PermissionType.WRITE);

        assertThatThrownBy(() -> authorizationService.canWrite(elementUuids))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowMoveTo() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        boolean result = authorizationService.canMoveTo(elementUuids, TARGET_DIRECTORY_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(elementUuids, TARGET_DIRECTORY_UUID, PermissionType.WRITE, true);
    }

    @Test
    void shouldPropagateExceptionWhenMoveElementPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);

        doThrow(new AccessDeniedException("Move access denied"))
                .when(directoryService).checkPermission(elementUuids, TARGET_DIRECTORY_UUID, PermissionType.WRITE, true);

        assertThatThrownBy(() -> authorizationService.canMoveTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowManage() {
        boolean result = authorizationService.canManage(ELEMENT_UUID);

        assertThat(result).isTrue();
        verify(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.MANAGE);
    }

    @Test
    void shouldPropagateExceptionWhenManageElementPermissionDenied() {
        doThrow(new AccessDeniedException("Manage access denied"))
                .when(directoryService).checkPermission(List.of(ELEMENT_UUID), null, PermissionType.MANAGE);

        assertThatThrownBy(() -> authorizationService.canManage(ELEMENT_UUID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
