/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Information about an element consuming a shared element (a usage of a shared element in the application)
 *
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
@Builder
public record ConsumerElementInfos(String elementName, String type,
                                   List<String> path,
                                   // only relevant when sharing a network modification within a study
                                   String node,
                                   String ownerLabel, Instant lastModificationDate, String lastModifiedByLabel) {
}
