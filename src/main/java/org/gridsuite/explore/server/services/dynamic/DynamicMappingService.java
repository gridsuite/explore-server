/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.dynamic;

import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.DYNAMIC_MAPPING;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@DirectoryElementTypes(DYNAMIC_MAPPING)
@Service
public class DynamicMappingService implements IDirectoryElementsService {
    DynamicMappingRestService dynamicMappingRestService;

    public DynamicMappingService(DynamicMappingRestService dynamicMappingRestService) {
        this.dynamicMappingRestService = dynamicMappingRestService;
    }

    @Override
    public void delete(UUID id) {
        dynamicMappingRestService.delete(id);
    }
}
