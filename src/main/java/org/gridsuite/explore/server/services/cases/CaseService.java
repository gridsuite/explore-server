/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.services.cases;

import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CaseService implements IDirectoryElementsService {
    CaseRestService caseRestService;

    public CaseService(CaseRestService caseRestService) {
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
