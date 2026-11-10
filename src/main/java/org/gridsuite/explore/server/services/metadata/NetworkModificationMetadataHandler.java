/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.gridsuite.explore.server.services.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.services.ExploreService.MODIFICATION;
import static org.gridsuite.explore.server.services.NetworkModificationService.DESCRIPTION;
import static org.gridsuite.explore.server.services.NetworkModificationService.NAME;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
public class NetworkModificationMetadataHandler extends MetadataHandler {
    private final NotificationService notificationService;

    public NetworkModificationMetadataHandler(IDirectoryElementsService service, DirectoryService directoryService, NotificationService notificationService) {
        super(service, directoryService);
        this.notificationService = notificationService;
    }

    // the real name of a modification is stored in the network modification server, not in the directory
    @Override
    public List<ElementAttributes> enrichDirectoryElements(List<ElementAttributes> elements) {
        List<ElementAttributes> modifications = elements.stream()
                .filter(element -> MODIFICATION.equals(element.getType()))
                .toList();

        if (modifications.isEmpty()) {
            return elements;
        }

        List<ElementAttributes> directoryElements = elements.stream()
                .filter(element -> !MODIFICATION.equals(element.getType()))
                .collect(Collectors.toCollection(ArrayList::new));

        directoryElements.addAll(completeElementsAttributes(modifications));
        return directoryElements;
    }

    @Override
    public ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        if (metadataItem != null) {
            if (metadataItem.containsKey(NAME) && metadataItem.get(NAME) != null) {
                elementAttributes.setElementName(metadataItem.get(NAME).toString());
            }
            if (metadataItem.containsKey(DESCRIPTION) && metadataItem.get(DESCRIPTION) != null) {
                elementAttributes.setDescription(metadataItem.get(DESCRIPTION).toString());
            }
        }
        elementAttributes.setSpecificMetadata(metadataItem);
        return elementAttributes;
    }

    @Override
    public void updateElement(UUID modificationToNotifyId, ElementAttributes elementAttributes) {
        // modificationToNotifyId can be the id of the shared parent modification
        // updatedModificationAttributes.getElementUuid() is the real id of the network modification to update
        // TODO check : est-ce que les modifications autre que nom et description de composites ne vont pas rater ? temps de modification par exemple ??
        // appeler l'original en pmettant nom et description à null ??
        ((NetworkModificationService) specificService).updateModificationNameAndDescription(
                elementAttributes.getElementUuid(), elementAttributes.getElementName(), elementAttributes.getDescription());
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.emitElementMetadataUpdated(modificationToNotifyId, userId);
    }
}
