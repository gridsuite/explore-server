/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.workspace;

import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class WorkspaceService implements IDirectoryElementsService {

    WorkspaceRestService workspaceRestService;

    public WorkspaceService(WorkspaceRestService workspaceRestService) {
        this.workspaceRestService = workspaceRestService;
    }

    @Override
    public void delete(UUID workspaceUuid) {
        workspaceRestService.delete(workspaceUuid);
    }
}
