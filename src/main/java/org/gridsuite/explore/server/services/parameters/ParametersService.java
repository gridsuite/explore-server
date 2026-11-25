/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.parameters;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.AbstractElementsService;
import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.*;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
// must stay in sync with ParametersType
@DirectoryElementTypes({
    VOLTAGE_INIT_PARAMETERS,
    SECURITY_ANALYSIS_PARAMETERS,
    LOADFLOW_PARAMETERS,
    SENSITIVITY_PARAMETERS,
    SHORT_CIRCUIT_PARAMETERS,
    PCC_MIN_PARAMETERS,
    NETWORK_VISUALIZATIONS_PARAMETERS
})
@Service
public class ParametersService extends AbstractElementsService {

    private final ParametersRestService parametersRestService;

    public ParametersService(ParametersRestService parametersRestService, DirectoryService directoryService, DirectoryRestService directoryRestService) {
        super(directoryRestService);
        this.parametersRestService = parametersRestService;
    }

    @Override
    public void delete(UUID parametersUuid) {
        ElementAttributes elementAttributes = directoryRestService.getElementInfos(parametersUuid);
        ParametersType parametersType = ParametersType.valueOf(elementAttributes.getType());
        parametersRestService.delete(parametersUuid, parametersType);
    }
}
