/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Seddik Yengui <seddik.yengui at rte-france.com>
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ElementInfos {
    private ElementAttributes elementAttributes;
    private String elementContent;
}
