/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.diagram;

import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SingleLineDiagramService implements IDirectoryElementsService {
    SingleLineDiagramRestService singleLineDiagramRestService;

    @Override
    public void delete(UUID configUuid) {
        singleLineDiagramRestService.delete(configUuid);
    }
}
