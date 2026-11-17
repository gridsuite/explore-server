/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.study;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class StudyService implements IDirectoryElementsService {

    StudyRestService studyRestService;

    public StudyService(StudyRestService studyRestService) {
        this.studyRestService = studyRestService;
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> studiesUuids) {
        return studyRestService.getMetadata(studiesUuids);
    }

    @Override
    public void delete(UUID studyUuid) {
        studyRestService.delete(studyUuid);
    }

    @Override
    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes, DirectoryService directoryService) {
        directoryService.updateElement(elementUuid, elementAttributes);
        studyRestService.notifyStudyUpdate(elementUuid);
    }
}
