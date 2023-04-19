/**
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import com.powsybl.contingency.contingency.list.criterion.SingleNominalVoltageCriterion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.explore.server.utils.NumericalFilterOperator;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NumericalFilter {
    NumericalFilterOperator type;
    Double value1;
    Double value2;

    public String operator() {
        switch (type) {
            case EQUALITY:
                return "==";
            case LESS_THAN:
                return "<";
            case LESS_OR_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_OR_EQUAL:
                return ">=";
            default:
                return "";
        }
    }

    public static SingleNominalVoltageCriterion toNominalVoltageCriterion(NumericalFilter numericalFilter) {
        if (numericalFilter == null) {
            return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval());
        }
        switch (numericalFilter.getType()) {
            case EQUALITY:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(numericalFilter.getValue1(), numericalFilter.getValue1(), true, true));
            case LESS_THAN:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(Double.MIN_VALUE, numericalFilter.getValue1(), true, false));
            case LESS_OR_EQUAL:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(Double.MIN_VALUE, numericalFilter.getValue1(), true, true));
            case GREATER_THAN:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(numericalFilter.getValue1(), Double.MAX_VALUE, false, true));
            case GREATER_OR_EQUAL:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(numericalFilter.getValue1(), Double.MAX_VALUE, true, true));
            case RANGE:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval(numericalFilter.getValue1(), numericalFilter.getValue2(), true, true));
            default:
                return new SingleNominalVoltageCriterion(new SingleNominalVoltageCriterion.VoltageInterval());
        }
    }
}

