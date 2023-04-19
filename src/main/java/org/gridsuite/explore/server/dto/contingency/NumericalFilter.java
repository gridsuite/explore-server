/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.contingency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.explore.server.utils.NumericalFilterOperator;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NumericalFilter {

    NumericalFilterOperator type;
    Double value1;
    Double value2;
}
