/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * @author Maissa Souissi <maissa.souissi at rte-france.com>
 */
@Getter
@NoArgsConstructor
@SuperBuilder
public class ReferenceAttributes {
    public enum ReferenceType {
        STUDY_NODE,
        STUDY_NODE_NETWORK_MODIFICATION,
        DIRECTORY_NETWORK_MODIFICATION,
    }

    @NonNull
    private UUID referenceId;
    @NonNull private ReferenceContainer referenceContainer;
    @NonNull private ReferenceType referenceType;
}
