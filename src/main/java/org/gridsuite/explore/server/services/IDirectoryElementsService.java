/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.directory.DirectoryService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */

public interface IDirectoryElementsService {

    default List<Map<String, Object>> getMetadata(List<UUID> uuidList) {
        return uuidList.stream().map(e -> Map.of("id", (Object) e)).collect(Collectors.toList());
    }

    void delete(UUID id);

    default void updateElement(UUID elementUuid, ElementAttributes elementAttributes, DirectoryService directoryService) {
        directoryService.updateElement(elementUuid, elementAttributes);
    }

    default Map<UUID, String> getNames(List<UUID> ids, DirectoryService directoryService) {
        return directoryService.getElementsName(ids);
    }

    default ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        elementAttributes.setSpecificMetadata(metadataItem);
        return elementAttributes;
    }

    // by default there are no overriden elements : we just return the basic directory elements
    default List<ElementAttributes> overrideBasicDirectoryElements(List<ElementAttributes> elementAttributes) {
        return elementAttributes;
    }
}
