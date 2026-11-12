/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.NetworkModificationService;
import org.gridsuite.explore.server.services.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static org.gridsuite.explore.server.services.ExploreService.MODIFICATION;
import static org.gridsuite.explore.server.services.NetworkModificationService.DESCRIPTION;
import static org.gridsuite.explore.server.services.NetworkModificationService.NAME;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
@Component
public class NetworkModificationMetadataHandler implements MetadataHandler {
    private final NetworkModificationService networkModificationService;
    private final NotificationService notificationService;

    public NetworkModificationMetadataHandler(NetworkModificationService networkModificationService, NotificationService notificationService) {
        this.networkModificationService = networkModificationService;
        this.notificationService = notificationService;
    }

    @Override
    public String getElementType() {
        return MODIFICATION;
    }

    // the real name of a modification is stored in the network modification server, not in the directory
    @Override
    public boolean overridesDirectoryAttributes() {
        return true;
    }

    @Override
    public ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        if (metadataItem != null) {
            if (metadataItem.get(NAME) != null) {
                elementAttributes.setElementName(metadataItem.get(NAME).toString());
            }
            if (metadataItem.get(DESCRIPTION) != null) {
                elementAttributes.setDescription(metadataItem.get(DESCRIPTION).toString());
            }
        }
        return MetadataHandler.setSpecificMetadata(elementAttributes, metadataItem);
    }

    @Override
    public void updateElement(UUID modificationToNotifyId, ElementAttributes elementAttributes) {
        // modificationToNotifyId can be the id of the shared parent modification
        // elementAttributes.getElementUuid() is the real id of the network modification to update
        networkModificationService.updateModificationNameAndDescription(
                elementAttributes.getElementUuid(), elementAttributes.getElementName(), elementAttributes.getDescription());
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.emitElementMetadataUpdated(modificationToNotifyId, userId);
    }
}
