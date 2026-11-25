/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.spreadsheet;

import org.gridsuite.explore.server.services.AbstractElementsService;
import org.gridsuite.explore.server.services.DirectoryElementTypes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.gridsuite.explore.server.dto.DirectoryElementType.SPREADSHEET_CONFIG_COLLECTION;

/**
 * @author Ayoub LABIDI <ayoub.labidi at rte-france.com>
 */
@DirectoryElementTypes(SPREADSHEET_CONFIG_COLLECTION)
@Service
public class SpreadsheetConfigCollectionService extends AbstractElementsService {
    SpreadsheetConfigCollectionRestService spreadsheetConfigCollectionRestService;

    @Autowired
    public SpreadsheetConfigCollectionService(SpreadsheetConfigCollectionRestService spreadsheetConfigCollectionRestService, DirectoryRestService directoryRestService) {
        super(directoryRestService);
        this.spreadsheetConfigCollectionRestService = spreadsheetConfigCollectionRestService;
    }

    @Override
    public void delete(UUID configUuid) {
        spreadsheetConfigCollectionRestService.delete(configUuid);
    }
}
