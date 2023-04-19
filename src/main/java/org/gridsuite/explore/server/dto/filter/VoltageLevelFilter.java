/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.gridsuite.explore.server.utils.filter.EquipmentType;
import org.gridsuite.explore.server.utils.filter.FilterType;
import org.springframework.util.CollectionUtils;

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
@Schema(description = "Voltage level Filters", allOf = CriteriaFilter.class)
public class VoltageLevelFilter extends AbstractEquipmentFilterForm {
    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.VOLTAGE_LEVEL;
    }

    @Schema(description = "Countries")
    private SortedSet<String> countries;

    @Schema(description = "Free properties")
    private Map<String, List<String>> freeProperties;

    @Schema(description = "Nominal voltage")
    private NumericalFilter nominalVoltage;

    public VoltageLevelFilter(String equipmentID, String equipmentName,
        SortedSet<String> countries, Map<String, List<String>> freeProperties,
        NumericalFilter nominalVoltage) {
        super(equipmentID, equipmentName);
        this.countries = countries;
        this.freeProperties = freeProperties;
        this.nominalVoltage =  nominalVoltage;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
            && CollectionUtils.isEmpty(countries)
            && CollectionUtils.isEmpty(freeProperties)
            && nominalVoltage == null;
    }
}
