/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import jakarta.validation.constraints.NotNull;
import org.gridsuite.explore.server.dto.PermissionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class AuthorizationService {

    private final DirectoryService directoryService;

    public AuthorizationService(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    //This method should only be called inside of @PreAuthorize to centralize permission checks
    public void isAuthorized(String userId, List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType) {
        directoryService.checkPermission(elementUuids, targetDirectoryUuid, userId, permissionType);
    }

    //This method should only be called inside of @PreAuthorize to centralize permission checks
    public void isAuthorizedForDuplication(String userId, UUID elementToDuplicate, UUID targetDirectoryUuid) {
        directoryService.checkPermission(List.of(elementToDuplicate), null, userId, PermissionType.READ);
        directoryService.checkPermission(List.of(targetDirectoryUuid != null ? targetDirectoryUuid : elementToDuplicate), null, userId, PermissionType.WRITE);
    }

    public void isRecursivelyAuthorized(String userId, List<UUID> elementUuids, UUID targetDirectoryUuid) {
        directoryService.checkPermission(elementUuids, targetDirectoryUuid, userId, PermissionType.WRITE, true);
    }

    public boolean canRead(UUID elementUuid) {
        return canRead(List.of(elementUuid));
    }

    public boolean canRead(List<UUID> elementUuids) {
        directoryService.checkPermission(elementUuids, null, PermissionType.READ);
        return true;
    }

    public boolean canWrite(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.WRITE);
        return true;
    }

    public boolean canDuplicateTo(UUID elementUuid, UUID targetDirectoryUuid) {
        return canDuplicateTo(List.of(elementUuid), targetDirectoryUuid != null ? targetDirectoryUuid : elementUuid);
    }

    public boolean canDuplicateTo(List<UUID> elementUuids, @NotNull UUID targetDirectoryUuid) {
        return canRead(elementUuids) && canWrite(targetDirectoryUuid);
    }

    public boolean canDelete(UUID elementUuid) {
        return canDelete(List.of(elementUuid));
    }

    public boolean canDelete(List<UUID> elementUuids) {
        return canRecursivelyWrite(elementUuids, null);
    }

    public boolean canMoveTo(List<UUID> elementUuids, UUID targetDirectoryUuid) {
        return canRecursivelyWrite(elementUuids, targetDirectoryUuid);
    } // pas sûre de ça, p-e on veut plus la main au niveau des endpoints pour savoir exactement ce qu'on checke ?

    public boolean canRecursivelyWrite(List<UUID> elementUuids, UUID targetDirectoryUuid) {
        directoryService.checkPermission(elementUuids, targetDirectoryUuid, PermissionType.WRITE, true);
        return true;
    }

    public boolean canManage(UUID elementUuid) {
        directoryService.checkPermission(List.of(elementUuid), null, PermissionType.MANAGE);
        return true;
    }
}
