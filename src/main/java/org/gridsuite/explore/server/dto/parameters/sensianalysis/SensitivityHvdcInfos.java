/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.parameters.sensianalysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.explore.server.dto.parameters.EquipmentsContainer;

import java.util.List;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SensitivityHvdcInfos {
    List<EquipmentsContainer> monitoredBranches;
    SensitivityType sensitivityType;
    List<EquipmentsContainer> hvdcs;
    List<EquipmentsContainer> contingencies;
    boolean activated;
}
