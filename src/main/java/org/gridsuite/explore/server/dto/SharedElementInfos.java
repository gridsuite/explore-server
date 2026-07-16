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
 * One element using a shared element. There is one instance per reference of the shared element,
 * so a same element can appear several times, once per node referencing it.
 *
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
@Builder
public record SharedElementInfos(String node, String elementName, String type, String subtype,
                                 List<String> pathName,
                                 String ownerLabel, Instant lastModificationDate, String lastModifiedByLabel) {
}
