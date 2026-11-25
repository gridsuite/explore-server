/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * handles the default behavior of directory elements. Service specific behaviors should override those functions
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
public abstract class AbstractElementsService {
    public DirectoryRestService directoryRestService;

    protected AbstractElementsService(DirectoryRestService directoryRestService) {
        this.directoryRestService = directoryRestService;
    }

    public List<Map<String, Object>> getMetadata(List<UUID> uuidList) {
        return uuidList.stream().map(e -> Map.of("id", (Object) e)).collect(Collectors.toList());
    }

    public abstract void delete(UUID id);

    /**
     * updates the directory element basic data. Should be overriden when those data are stored in a specific way
     */
    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes) {
        directoryRestService.updateElement(elementUuid, elementAttributes);
    }

    public Map<UUID, String> getNames(List<UUID> ids) {
        return directoryRestService.getElementsName(ids);
    }

    /**
     * by default adds specific metadata to an element. May be overriden if the specific metadata have extra effects on the basic metadata
     */
    public ElementAttributes addSpecificMetadata(ElementAttributes basicMetadata, Map<String, Object> specificMetadataItem) {
        basicMetadata.setSpecificMetadata(specificMetadataItem);
        return basicMetadata;
    }

    // by default there are no overriden elements : we just return the basic directory elements
    public List<ElementAttributes> overrideBasicDirectoryElements(List<ElementAttributes> elementAttributes) {
        return elementAttributes;
    }
}
