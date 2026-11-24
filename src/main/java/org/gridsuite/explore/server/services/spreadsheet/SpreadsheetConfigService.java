/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.spreadsheet;

import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.SPREADSHEET_CONFIG;

/**
 * @author Achour BERRAHMA <achour.berrahma at rte-france.com>
 */
@DirectoryElementTypes(SPREADSHEET_CONFIG)
@Service
public class SpreadsheetConfigService implements IDirectoryElementsService {
    SpreadsheetConfigRestService spreadsheetConfigRestService;

    public SpreadsheetConfigService(SpreadsheetConfigRestService spreadsheetConfigRestService) {
        this.spreadsheetConfigRestService = spreadsheetConfigRestService;
    }

    @Override
    public void delete(UUID configUuid) {
        spreadsheetConfigRestService.delete(configUuid);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> configsUuids) {
        return spreadsheetConfigRestService.getMetadata(configsUuids);
    }
}
