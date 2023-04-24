/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.dto.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.explore.server.utils.filter.EquipmentType;
import org.gridsuite.explore.server.utils.filter.FilterType;

import java.util.Date;
import java.util.UUID;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@Setter
@NoArgsConstructor
public class FilterAttributes implements IFilterAttributes {
    UUID id;
    Date modificationDate;
    FilterType type;
    EquipmentType equipmentType;

    public FilterAttributes(FilterMetadata filterMetadata, FilterType type, EquipmentType equipmentType) {
        id = filterMetadata.getId();
        modificationDate = filterMetadata.getModificationDate();
        this.type = type;
        this.equipmentType = equipmentType;
    }
}
