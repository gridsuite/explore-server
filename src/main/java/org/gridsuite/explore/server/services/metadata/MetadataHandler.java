/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import lombok.extern.slf4j.Slf4j;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
@Slf4j
public class MetadataHandler {
    IDirectoryElementsService specificService;
    DirectoryService directoryService;

    public MetadataHandler(IDirectoryElementsService specificService, DirectoryService directoryService) {
        this.specificService = specificService;
        this.directoryService = directoryService;
    }

    // no modification by default. Should be overriden when the element type need to update specific elements data
    public List<ElementAttributes> completeDirectoryElements(List<ElementAttributes> elements) {
        return elements;
    }

    public List<ElementAttributes> completeElementsAttributes(List<ElementAttributes> lstElementAttribute) {
        /* generating id -> elementAttribute map */
        Map<String, ElementAttributes> mapElementAttribute = lstElementAttribute.stream()
                .collect(Collectors.toMap(e -> e.getElementUuid().toString(), Function.identity()));
        /* getting metadata from services */
        try {
            List<UUID> elementsUuids = lstElementAttribute.stream().map(ElementAttributes::getElementUuid).collect(Collectors.toList());
            List<Map<String, Object>> metadata = specificService.getMetadata(elementsUuids);
            return metadata.stream().map(metadataItem -> {
                Object item = metadataItem.get("id");
                if (item == null) {
                    item = metadataItem.getOrDefault("uuid", "");
                }
                ElementAttributes e = mapElementAttribute.get(item.toString());
                return populateMetadataItem(e, metadataItem);
            }).collect(Collectors.toList());
        } catch (ResourceAccessException e) {
            String elementType = lstElementAttribute.isEmpty() ? "UNKNOWN" : lstElementAttribute.getFirst().getType();
            log.warn("{} metadata service is unavailable, returning elements with empty specific metadata", elementType);
            return lstElementAttribute.stream()
                    .map(elementAttributes -> populateMetadataItem(elementAttributes, Map.of()))
                    .collect(Collectors.toList());
        }
    }

    ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        elementAttributes.setSpecificMetadata(metadataItem);
        return elementAttributes;
    }

    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes) {
        directoryService.updateElement(elementUuid, elementAttributes);
    }
}
