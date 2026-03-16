/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.parameters.EquipmentsContainer;
import org.gridsuite.explore.server.dto.parameters.securityanalysis.ContingencyLists;
import org.gridsuite.explore.server.dto.parameters.securityanalysis.SecurityAnalysisParameters;
import org.gridsuite.explore.server.dto.parameters.securityanalysis.SecurityAnalysisParametersInfos;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class SecurityAnalysisParametersMapper {

    public SecurityAnalysisParameters getParameters(SecurityAnalysisParametersInfos paramsInfos) {

        List<ContingencyLists> contingencyLists = paramsInfos.getContingencyListsInfos().stream()
                .map(clInfos -> new ContingencyLists(
                        EquipmentsContainer.getEquipmentsContainerUuids(clInfos.getContingencyLists()),
                        clInfos.getDescription(),
                        clInfos.isActivated()))
                .toList();

        return SecurityAnalysisParameters.builder()
                .provider(paramsInfos.getProvider())
                .lowVoltageAbsoluteThreshold(paramsInfos.getLowVoltageAbsoluteThreshold())
                .lowVoltageProportionalThreshold(paramsInfos.getLowVoltageProportionalThreshold())
                .highVoltageAbsoluteThreshold(paramsInfos.getHighVoltageAbsoluteThreshold())
                .highVoltageProportionalThreshold(paramsInfos.getHighVoltageProportionalThreshold())
                .flowProportionalThreshold(paramsInfos.getFlowProportionalThreshold())
                .contingencyListsInfos(contingencyLists)
                .limitReductions(paramsInfos.getLimitReductions())
                .build();
    }
}
