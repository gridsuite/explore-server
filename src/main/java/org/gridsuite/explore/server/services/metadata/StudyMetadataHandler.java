/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;

import java.util.UUID;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
public class StudyMetadataHandler extends MetadataHandler {

    public StudyMetadataHandler(IDirectoryElementsService service, DirectoryService directoryService) {
        super(service, directoryService);
    }

    @Override
    public void updateElement(UUID id, ElementAttributes elementAttributes) {
        super.updateElement(id, elementAttributes);
        ((StudyService) specificService).notifyStudyUpdate(id);
    }
}
