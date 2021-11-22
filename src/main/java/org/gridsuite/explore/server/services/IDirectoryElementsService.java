/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */

public interface IDirectoryElementsService {
    default Flux<Map<String, Object>> getMetadata(List<UUID> uuidList) {
        return Flux.empty();
    }

    Mono<Void> delete(UUID id, String userId);
}
