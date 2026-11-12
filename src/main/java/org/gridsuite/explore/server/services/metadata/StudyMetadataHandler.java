/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.StudyService;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.gridsuite.explore.server.services.ExploreService.STUDY;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
@Component
public class StudyMetadataHandler implements MetadataHandler {
    private final StudyService studyService;
    private final DirectoryService directoryService;

    public StudyMetadataHandler(StudyService studyService, DirectoryService directoryService) {
        this.studyService = studyService;
        this.directoryService = directoryService;
    }

    @Override
    public String getElementType() {
        return STUDY;
    }

    @Override
    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes) {
        directoryService.updateElement(elementUuid, elementAttributes);
        studyService.notifyStudyUpdate(elementUuid);
    }
}
