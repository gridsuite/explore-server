/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.contingency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Form contingency list")
public class FormContingencyList {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Equipment type")
    private String equipmentType;

    @Schema(description = "Nominal voltage 1")
    private NumericalFilter nominalVoltage1;

    @Schema(description = "Nominal voltage 2")
    private NumericalFilter nominalVoltage2;

    @Schema(description = "Countries 1")
    private Set<String> countries1;

    @Schema(description = "Countries 2")
    private Set<String> countries2;
}
