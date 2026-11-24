/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.contingency;

import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.CONTINGENCY_LIST;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@DirectoryElementTypes(CONTINGENCY_LIST)
@Service
public class ContingencyListService implements IDirectoryElementsService {
    ContingencyListRestService contingencyListRestService;

    public ContingencyListService(ContingencyListRestService contingencyListRestService) {
        this.contingencyListRestService = contingencyListRestService;
    }

    @Override
    public void delete(UUID id) {
        contingencyListRestService.delete(id);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> contingencyListsUuids) {
        return contingencyListRestService.getMetadata(contingencyListsUuids);
    }
}
