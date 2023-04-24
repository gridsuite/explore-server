/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.filter;

import com.powsybl.iidm.network.EnergySource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.gridsuite.explore.server.utils.filter.EquipmentType;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Schema(description = "Generator Filters", allOf = AbstractInjectionFilter.class)
public class GeneratorFilter extends AbstractInjectionFilter {

    @Schema(description = "Energy source")
    EnergySource energySource;

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && energySource == null;
    }

    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.GENERATOR;
    }
}
