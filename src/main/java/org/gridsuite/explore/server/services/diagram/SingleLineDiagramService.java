/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.diagram;

import org.gridsuite.explore.server.services.AbstractElementsService;
import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.DIAGRAM_CONFIG;

@DirectoryElementTypes(DIAGRAM_CONFIG)
@Service
public class SingleLineDiagramService extends AbstractElementsService {
    SingleLineDiagramRestService singleLineDiagramRestService;

    public SingleLineDiagramService(SingleLineDiagramRestService singleLineDiagramRestService, DirectoryRestService directoryRestService) {
        super(directoryRestService);
        this.singleLineDiagramRestService = singleLineDiagramRestService;
    }

    @Override
    public void delete(UUID configUuid) {
        singleLineDiagramRestService.delete(configUuid);
    }
}
