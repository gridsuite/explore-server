/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.ElementAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
interface IDirectoryElementsService {
    String HEADER_USER_ID = "userId";

    default List<Map<String, Object>> getMetadata(List<UUID> uuidList) {
        return uuidList.stream().map(e -> Map.of("id", (Object) e)).collect(Collectors.toList());
    }

    void delete(UUID id, String userId);

    default List<ElementAttributes> completeElementAttribute(List<ElementAttributes> lstElementAttribute) {
        /* generating id -> elementAttribute map */
        Map<String, ElementAttributes> mapElementAttribute = lstElementAttribute.stream()
                .collect(Collectors.toMap(e -> e.elementUuid().toString(), Function.identity()));
        /* getting metadata from services */
        List<Map<String, Object>> metadata = getMetadata(lstElementAttribute.stream().map(ElementAttributes::elementUuid).collect(Collectors.toList()));
        return metadata.stream().map(metadataItem -> {
            Object item = metadataItem.get("id");
            if (item == null) {
                item = metadataItem.getOrDefault("uuid", "");
            }
            ElementAttributes e = mapElementAttribute.get(item.toString());
            return e.toBuilder().specificMetadata(metadataItem).build();
        }).collect(Collectors.toList());
    }
}
