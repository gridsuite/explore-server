/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.gridsuite.explore.server.utils.filter.EquipmentType;
import org.gridsuite.explore.server.utils.filter.FilterType;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Schema(description = "Substation Filters", allOf = CriteriaFilter.class)
public class SubstationFilter extends AbstractEquipmentFilterForm {
    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.SUBSTATION;
    }

    @Schema(description = "Countries")
    private SortedSet<String> countries;

    public SubstationFilter(String equipmentID, String equipmentName,
        SortedSet<String> countries, Map<String, List<String>> freeProperties) {
        super(equipmentID, equipmentName);
        this.countries = countries;
        this.freeProperties = freeProperties;
    }

    @Schema(description = "Free properties")
    // LinkedHashMap to keep order too
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<String, List<String>> freeProperties;

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
            && CollectionUtils.isEmpty(countries) && CollectionUtils.isEmpty(freeProperties);
    }
}