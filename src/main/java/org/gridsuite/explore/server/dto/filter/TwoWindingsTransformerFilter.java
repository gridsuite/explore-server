/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
@Schema(description = "Two windings transformer Filters", allOf = CriteriaFilter.class)
public class TwoWindingsTransformerFilter extends AbstractEquipmentFilterForm {
    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.TWO_WINDINGS_TRANSFORMER;
    }

    @Schema(description = "SubstationName")
    String substationName;

    @Schema(description = "Countries")
    private SortedSet<String> countries;

    @Schema(description = "Free properties")
    // LinkedHashMap to keep order too
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<String, List<String>> freeProperties;

    @Schema(description = "Nominal voltage 1")
    private NumericalFilter nominalVoltage1;

    @Schema(description = "Nominal voltage 2")
    private NumericalFilter nominalVoltage2;

    public TwoWindingsTransformerFilter(String equipmentID, String equipmentName, String substationName,
        SortedSet<String> countries, Map<String, List<String>> freeProperties,
        NumericalFilter nominalVoltage1, NumericalFilter nominalVoltage2) {
        super(equipmentID, equipmentName);
        this.substationName =  substationName;
        this.countries = countries;
        this.freeProperties = freeProperties;
        this.nominalVoltage1 =  nominalVoltage1;
        this.nominalVoltage2 =  nominalVoltage2;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
            && substationName == null
            && CollectionUtils.isEmpty(countries)
            && nominalVoltage1 == null
            && nominalVoltage2 == null
            && CollectionUtils.isEmpty(freeProperties);
    }
}