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

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class IdentifierListFilterEquipmentAttributes {

    @Schema(description = "Equipment ID")
    private String equipmentID;

    @Schema(description = "Distribution Key")
    private Double distributionKey;
}
