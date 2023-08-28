/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
//@NoArgsConstructor(force = true)
@Builder(toBuilder = true)
public record ElementAttributes(
    UUID elementUuid,
    String elementName,
    String type,
    AccessRightsAttributes accessRights,
    String owner,
    Long subdirectoriesCount,
    String description,
    Map<String, Object> specificMetadata
) {
    public ElementAttributes(UUID elementUuid, String elementName, String type, AccessRightsAttributes accessRights, String owner, Long subdirectoriesCount, String description) {
        this(elementUuid, elementName, type, accessRights, owner, subdirectoriesCount, description, null);
    }

    //TODO is it really pertinent?
    public ElementAttributes() {
        this(null, null, null, null, null, null, null, new HashMap<>());
    }
}
