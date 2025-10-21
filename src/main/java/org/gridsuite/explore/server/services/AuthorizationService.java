/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.error.ExploreException;
import org.gridsuite.explore.server.dto.PermissionResponse;
import org.gridsuite.explore.server.dto.PermissionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.EXPLORE_PERMISSION_DENIED;

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
    public boolean isAuthorized(String userId, List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType) {
        PermissionResponse permissionResponse = directoryService.checkPermission(elementUuids, targetDirectoryUuid, userId, permissionType);
        if (!permissionResponse.hasPermission()) {
            throw ExploreException.of(EXPLORE_PERMISSION_DENIED, permissionResponse.permissionCheckResult());
        }
        return true;
    }

    //This method should only be called inside of @PreAuthorize to centralize permission checks
    public boolean isAuthorizedForDuplication(String userId, UUID elementToDuplicate, UUID targetDirectoryUuid) {
        PermissionResponse readCheck = directoryService.checkPermission(List.of(elementToDuplicate), null, userId, PermissionType.READ);
        if (!readCheck.hasPermission()) {
            throw ExploreException.of(EXPLORE_PERMISSION_DENIED, readCheck.permissionCheckResult());
        }
        PermissionResponse writeCheck = directoryService.checkPermission(List.of(targetDirectoryUuid != null ? targetDirectoryUuid : elementToDuplicate), null, userId, PermissionType.WRITE);
        if (!writeCheck.hasPermission()) {
            throw ExploreException.of(EXPLORE_PERMISSION_DENIED, writeCheck.permissionCheckResult());
        }
        return true;
    }

    public boolean isRecursivelyAuthorized(String userId, List<UUID> elementUuids, UUID targetDirectoryUuid) {
        PermissionResponse permissionResponse = directoryService.checkPermission(elementUuids, targetDirectoryUuid, userId, PermissionType.WRITE, true);
        if (!permissionResponse.hasPermission()) {
            throw ExploreException.of(EXPLORE_PERMISSION_DENIED, permissionResponse.permissionCheckResult());
        }
        return true;
    }
}
