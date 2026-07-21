/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
public enum QuotaType {
    CASES,
    BUILD,
    LOAD_FLOW,
    SECURITY_ANALYSIS,
    SENSITIVITY_ANALYSIS,
    SHORT_CIRCUIT,
    VOLTAGE_INITIALIZATION,
    PCC_MIN,
    STATE_ESTIMATION,
    BALANCE_ADJUSTMENT,
    DYNAMIC_SIMULATION,
    DYNAMIC_SECURITY_ANALYSIS,
    DYNAMIC_MARGIN_CALCULATION,
}
