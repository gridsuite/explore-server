/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.modification;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.IDirectoryElementsService;
import org.gridsuite.explore.server.services.NotificationService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class NetworkModificationService implements IDirectoryElementsService {
    private final NotificationService notificationService;
    private final NetworkModificationRestService networkModificationRestService;

    public NetworkModificationService(NotificationService notificationService, NetworkModificationRestService networkModificationRestService) {
        this.notificationService = notificationService;
        this.networkModificationRestService = networkModificationRestService;
    }

    @Override
    public void delete(UUID id) {
        networkModificationRestService.delete(id);
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> modificationUuids) {
        return networkModificationRestService.getMetadata(modificationUuids);
    }

    @Override
    public void updateElement(UUID modificationToNotifyId, ElementAttributes elementAttributes, DirectoryService directoryService) {
        // modificationToNotifyId can be the id of the shared parent modification
        // elementAttributes.getElementUuid() is the real id of the network modification to update
        networkModificationRestService.updateModificationNameAndDescription(
                elementAttributes.getElementUuid(), elementAttributes.getElementName(), elementAttributes.getDescription());
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.emitElementMetadataUpdated(modificationToNotifyId, userId);
    }

    @Override
    public ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> specificServiceMetadata) {
        if (specificServiceMetadata != null) {
            if (specificServiceMetadata.get(NetworkModificationRestService.NAME) != null) {
                elementAttributes.setElementName(specificServiceMetadata.get(NetworkModificationRestService.NAME).toString());
            }
            if (specificServiceMetadata.get(NetworkModificationRestService.DESCRIPTION) != null) {
                elementAttributes.setDescription(specificServiceMetadata.get(NetworkModificationRestService.DESCRIPTION).toString());
            }
        }
        return IDirectoryElementsService.setSpecificMetadata(elementAttributes, specificServiceMetadata);
    }

    @Override
    public List<ElementAttributes> overrideBasicDirectoryElements(List<ElementAttributes> elementsAttributes) {
        List<UUID> modificationUuids = elementsAttributes.stream().map(ElementAttributes::getElementUuid).toList();
        List<Map<String, Object>> specificServiceMetadataList = getMetadata(modificationUuids);

        modificationUuids.forEach(modificationUuid -> {
            Map<String, Object> specificServiceMetadata = specificServiceMetadataList.stream()
                    .filter(metadata ->
                            metadata.get("id").equals(modificationUuid.toString()))
                    .findFirst().orElse(null);
            ElementAttributes elementAttributes = elementsAttributes.stream()
                    .filter(attributes -> attributes.getElementUuid().equals(modificationUuid))
                    .findFirst().orElse(null);
            if (specificServiceMetadata != null && elementAttributes != null) {
                if (specificServiceMetadata.get(NetworkModificationRestService.NAME) != null) {
                    elementAttributes.setElementName(specificServiceMetadata.get(NetworkModificationRestService.NAME).toString());
                }
                if (specificServiceMetadata.get(NetworkModificationRestService.DESCRIPTION) != null) {
                    elementAttributes.setDescription(specificServiceMetadata.get(NetworkModificationRestService.DESCRIPTION).toString());
                }
            }
        });

        return elementsAttributes;
    }
}
