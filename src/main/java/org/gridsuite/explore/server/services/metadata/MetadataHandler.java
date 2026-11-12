/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services.metadata;

import org.gridsuite.explore.server.dto.ElementAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Specific metadata behaviour of an element type.
 * Implementations are Spring beans automatically collected by {@link MetadataService}.
 * Element types without a dedicated handler get the default behaviour.
 *
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france_externe.com>
 */
public interface MetadataHandler {

    String getElementType();

    /**
     * @return true when the specific server holds data (name, description...) overriding the directory element ones,
     * meaning that directory elements of this type must be enriched before being returned
     */
    default boolean overridesDirectoryAttributes() {
        return false;
    }

    default ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        return setSpecificMetadata(elementAttributes, metadataItem);
    }

    void updateElement(UUID elementUuid, ElementAttributes elementAttributes);

    static ElementAttributes setSpecificMetadata(ElementAttributes elementAttributes, Map<String, Object> metadataItem) {
        elementAttributes.setSpecificMetadata(metadataItem);
        return elementAttributes;
    }
}
