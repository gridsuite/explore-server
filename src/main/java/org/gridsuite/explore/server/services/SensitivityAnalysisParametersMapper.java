/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.parameters.EquipmentsContainer;
import org.gridsuite.explore.server.dto.parameters.sensianalysis.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class SensitivityAnalysisParametersMapper {

    public SensitivityAnalysisParameters getParameters(SensitivityAnalysisParametersInfos paramsInfos) {

        List<SensitivityInjectionsSet> sensiInjectionsSets = paramsInfos.getSensitivityInjectionsSet().stream()
                .map(injectionsSetInfos -> new SensitivityInjectionsSet(
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionsSetInfos.getMonitoredBranches()),
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionsSetInfos.getInjections()),
                        injectionsSetInfos.getDistributionType(),
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionsSetInfos.getContingencies()),
                        injectionsSetInfos.isActivated()))
                .toList();

        List<SensitivityInjection> sensiInjections = paramsInfos.getSensitivityInjection().stream()
                .map(injectionInfos -> new SensitivityInjection(
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionInfos.getMonitoredBranches()),
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionInfos.getInjections()),
                        EquipmentsContainer.getEquipmentsContainerUuids(injectionInfos.getContingencies()),
                        injectionInfos.isActivated()))
                .toList();

        List<SensitivityHVDC> sensiHvdcs = paramsInfos.getSensitivityHVDC().stream()
                .map(hvdcInfos -> new SensitivityHVDC(
                        EquipmentsContainer.getEquipmentsContainerUuids(hvdcInfos.getMonitoredBranches()),
                        hvdcInfos.getSensitivityType(),
                        EquipmentsContainer.getEquipmentsContainerUuids(hvdcInfos.getHvdcs()),
                        EquipmentsContainer.getEquipmentsContainerUuids(hvdcInfos.getContingencies()),
                        hvdcInfos.isActivated()))
                .toList();

        List<SensitivityPST> sensiPsts = paramsInfos.getSensitivityPST().stream()
                .map(pstInfos -> new SensitivityPST(
                        EquipmentsContainer.getEquipmentsContainerUuids(pstInfos.getMonitoredBranches()),
                        pstInfos.getSensitivityType(),
                        EquipmentsContainer.getEquipmentsContainerUuids(pstInfos.getPsts()),
                        EquipmentsContainer.getEquipmentsContainerUuids(pstInfos.getContingencies()),
                        pstInfos.isActivated()))
                .toList();

        List<SensitivityNodes> sensiNodes = paramsInfos.getSensitivityNodes().stream()
                .map(nodeInfos -> new SensitivityNodes(
                        EquipmentsContainer.getEquipmentsContainerUuids(nodeInfos.getMonitoredVoltageLevels()),
                        EquipmentsContainer.getEquipmentsContainerUuids(nodeInfos.getEquipmentsInVoltageRegulation()),
                        EquipmentsContainer.getEquipmentsContainerUuids(nodeInfos.getContingencies()),
                        nodeInfos.isActivated()))
                .toList();

        return SensitivityAnalysisParameters.builder()
                .uuid(paramsInfos.getUuid())
                .provider(paramsInfos.getProvider())
                .flowFlowSensitivityValueThreshold(paramsInfos.getFlowFlowSensitivityValueThreshold())
                .angleFlowSensitivityValueThreshold(paramsInfos.getAngleFlowSensitivityValueThreshold())
                .flowVoltageSensitivityValueThreshold(paramsInfos.getFlowVoltageSensitivityValueThreshold())
                .sensitivityInjectionsSet(sensiInjectionsSets)
                .sensitivityInjection(sensiInjections)
                .sensitivityHVDC(sensiHvdcs)
                .sensitivityPST(sensiPsts)
                .sensitivityNodes(sensiNodes)
                .build();
    }
}
