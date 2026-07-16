/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * attributes of the references to a shared element stored in directory server
 *
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceAttributes {
    public enum ReferenceType {
        STUDY_NODE,
        NETWORK_MODIFICATION,
        DIRECTORY_ELEMENT
    }

    private UUID referenceId;
    private ReferenceType referenceType;
}
