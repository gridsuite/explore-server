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
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Completes directory elements with the metadata held by their specific server,
 * delegating type specific behaviour to the {@link MetadataHandler} beans.
 *
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
@Slf4j
@Service
public class MetadataService {
    private final DirectoryService directoryService;
    private final Map<String, MetadataHandler> handlers;

    public MetadataService(DirectoryService directoryService, List<MetadataHandler> handlers) {
        this.directoryService = directoryService;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(MetadataHandler::getElementType, Function.identity()));
    }

    /**
     * Enriches only the elements whose type overrides some directory attributes (see {@link MetadataHandler#overridesDirectoryAttributes()}).
     */
    public List<ElementAttributes> enrichDirectoryElements(List<ElementAttributes> elements) {
        Map<Boolean, List<ElementAttributes>> elementsByOverride = elements.stream()
                .collect(Collectors.partitioningBy(element -> overridesDirectoryAttributes(element.getType())));
        if (elementsByOverride.get(true).isEmpty()) {
            return elements;
        }
        List<ElementAttributes> enrichedElements = new ArrayList<>(elementsByOverride.get(false));
        enrichedElements.addAll(completeElementsAttributes(elementsByOverride.get(true)));
        return enrichedElements;
    }

    public List<ElementAttributes> completeElementsAttributes(List<ElementAttributes> elements) {
        return elements.stream()
                .collect(Collectors.groupingBy(ElementAttributes::getType))
                .entrySet().stream()
                .flatMap(entry -> completeElementsAttributes(entry.getKey(), entry.getValue()).stream())
                .collect(Collectors.toList());
    }

    public void updateElement(UUID elementUuid, String elementType, ElementAttributes elementAttributes) {
        MetadataHandler handler = handlers.get(elementType);
        if (handler != null) {
            handler.updateElement(elementUuid, elementAttributes);
        } else {
            directoryService.updateElement(elementUuid, elementAttributes);
        }
    }

    private boolean overridesDirectoryAttributes(String elementType) {
        MetadataHandler handler = handlers.get(elementType);
        return handler != null && handler.overridesDirectoryAttributes();
    }

    private List<ElementAttributes> completeElementsAttributes(String elementType, List<ElementAttributes> elements) {
        IDirectoryElementsService specificService = directoryService.getGenericService(elementType);
        MetadataHandler handler = handlers.get(elementType);
        BiFunction<ElementAttributes, Map<String, Object>, ElementAttributes> populate = handler != null
                ? handler::populateMetadataItem
                : MetadataHandler::setSpecificMetadata;

        Map<String, ElementAttributes> elementsById = elements.stream()
                .collect(Collectors.toMap(e -> e.getElementUuid().toString(), Function.identity()));
        try {
            List<UUID> elementsUuids = elements.stream().map(ElementAttributes::getElementUuid).toList();
            return specificService.getMetadata(elementsUuids).stream().map(metadataItem -> {
                Object id = metadataItem.get("id");
                if (id == null) {
                    id = metadataItem.getOrDefault("uuid", "");
                }
                return populate.apply(elementsById.get(id.toString()), metadataItem);
            }).collect(Collectors.toList());
        } catch (ResourceAccessException _) {
            log.warn("{} metadata service is unavailable, returning elements with empty specific metadata", elementType);
            return elements.stream()
                    .map(element -> populate.apply(element, Map.of()))
                    .collect(Collectors.toList());
        }
    }
}
