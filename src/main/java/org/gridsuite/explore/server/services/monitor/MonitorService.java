/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.monitor;

import org.gridsuite.explore.server.services.AbstractElementsService;
import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.PROCESS_CONFIG;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@DirectoryElementTypes(PROCESS_CONFIG)
@Service
public class MonitorService extends AbstractElementsService {
    MonitorRestService monitorRestService;

    public MonitorService(MonitorRestService monitorRestService, DirectoryRestService directoryRestService) {
        super(directoryRestService);
        this.monitorRestService = monitorRestService;
    }

    @Override
    public void delete(UUID id) {
        monitorRestService.delete(id);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> processConfigUuids) {
        return monitorRestService.getMetadata(processConfigUuids);
    }
}
