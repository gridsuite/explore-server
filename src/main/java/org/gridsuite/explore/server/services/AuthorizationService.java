/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.PermissionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */

@Service
public class AuthorizationService {

    private final UserAdminService userAdminService;
    private final DirectoryService directoryService;

    public AuthorizationService(UserAdminService userAdminService, DirectoryService directoryService) {
        this.userAdminService = userAdminService;
        this.directoryService = directoryService;
    }

    public void isAuthorized(String userId, List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType) {
        if (!userAdminService.isUserAdmin(userId) && !directoryService.hasPermission(elementUuids, targetDirectoryUuid, userId, permissionType)) {
            throw new ExploreException(ExploreException.Type.NOT_ALLOWED);
        }
    }
}
