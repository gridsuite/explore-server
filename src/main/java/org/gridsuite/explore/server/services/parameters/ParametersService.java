/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.parameters;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@Service
public class ParametersService implements IDirectoryElementsService {

    private final ParametersRestService parametersRestService;
    private final DirectoryService directoryService;

    public ParametersService(ParametersRestService parametersRestService, DirectoryService directoryService) {
        this.parametersRestService = parametersRestService;
        this.directoryService = directoryService;
    }

    @Override
    public void delete(UUID parametersUuid) {
        ElementAttributes elementAttributes = directoryService.getElementInfos(parametersUuid);
        ParametersType parametersType = ParametersType.valueOf(elementAttributes.getType());
        parametersRestService.delete(parametersUuid, parametersType);
    }
}
