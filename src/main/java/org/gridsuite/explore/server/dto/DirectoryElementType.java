/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

/**
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
public enum DirectoryElementType {
    DIRECTORY,
    STUDY,
    CASE,
    CONTINGENCY_LIST,
    FILTER,
    MODIFICATION, // only for composite modification
    SPREADSHEET_CONFIG,
    SPREADSHEET_CONFIG_COLLECTION,
    DIAGRAM_CONFIG,
    WORKSPACE,
    PROCESS_CONFIG,
    DYNAMIC_MAPPING,
    VOLTAGE_INIT_PARAMETERS,
    SECURITY_ANALYSIS_PARAMETERS,
    LOADFLOW_PARAMETERS,
    SENSITIVITY_PARAMETERS,
    SHORT_CIRCUIT_PARAMETERS,
    PCC_MIN_PARAMETERS,
    NETWORK_VISUALIZATIONS_PARAMETERS
}
