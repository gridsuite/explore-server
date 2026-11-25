/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services.cases;

import org.gridsuite.explore.server.services.AbstractElementsService;
import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.CASE;

@DirectoryElementTypes(CASE)
@Service
public class CaseService extends AbstractElementsService {
    CaseRestService caseRestService;

    public CaseService(CaseRestService caseRestService, DirectoryRestService directoryRestService) {
        super(directoryRestService);
        this.caseRestService = caseRestService;
    }

    @Override
    public void delete(UUID id) {
        caseRestService.delete(id);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> casesUuids) {
        return caseRestService.getMetadata(casesUuids);
    }
}
