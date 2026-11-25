/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.directory;

import org.gridsuite.explore.server.services.*;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.gridsuite.explore.server.dto.DirectoryElementType.DIRECTORY;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@DirectoryElementTypes(DIRECTORY)
@Service
public class DirectoryService extends AbstractElementsService {

    public DirectoryService(DirectoryRestService directoryRestService) {
        super(directoryRestService);
    }

    @Override
    public void delete(UUID id) {
        // elements are already deleted before in explore service,
        // this function only delete data which is in other services, and there are none for simple directory elements
    }
}
