/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.dto.filter;

import org.gridsuite.explore.server.utils.filter.EquipmentType;
import org.gridsuite.explore.server.utils.filter.FilterType;


/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
public interface IFilterAttributes {
    java.util.UUID getId();

    java.util.Date getModificationDate();

    FilterType getType();

    EquipmentType getEquipmentType();
}
