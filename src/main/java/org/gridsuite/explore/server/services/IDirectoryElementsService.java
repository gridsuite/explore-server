/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.ElementAttributes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    default Flux<Map<String, Object>> getMetadata(List<UUID> uuidList) {
        return Flux.fromStream(() -> uuidList.stream().map(e -> Map.of("id", e)));
    }

    Mono<Void> delete(UUID id, String userId);

    default Flux<ElementAttributes> completeElementAttribute(List<ElementAttributes> lstElementAttribute) {
        /* generating id -> elementAttribute map */
        Map<String, ElementAttributes> mapElementAttribute = lstElementAttribute.stream()
            .collect(Collectors.toMap(e -> e.getElementUuid().toString(), Function.identity()));

        /* getting metadata from services */
        return getMetadata(lstElementAttribute.stream().map(ElementAttributes::getElementUuid).collect(Collectors.toList()))
            .flatMap(metadataItem -> Mono.justOrEmpty(mapElementAttribute.get(metadataItem.getOrDefault("id", "").toString()))
                .map(elementAttribute -> {
                    elementAttribute.setSpecificMetadata(metadataItem);
                    return elementAttribute;
                }));
    }
}
