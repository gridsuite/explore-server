/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.PermissionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Caroline JEANDAT <caroline.jeandat at rte-france.com>
 */
@Service
public class PermissionService {
    private final DirectoryService directoryService;

    public PermissionService(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    public boolean canRead(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.READ);
        return true;
    }

    public boolean canWrite(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.WRITE);
        return true;
    }

    public boolean canDuplicateTo(UUID elementUuid, UUID targetDirectoryUuid) {
        canRead(elementUuid);
        canWrite(targetDirectoryUuid != null ? targetDirectoryUuid : elementUuid);
        return true;
    }

    public boolean canDelete(UUID elementUuid) {
        canRecursivelyWrite(elementUuid);
        return true;
    }

    public boolean canRecursivelyWrite(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.WRITE, true);
        return true;
    }

    public boolean canManage(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.MANAGE);
        return true;
    }
}
