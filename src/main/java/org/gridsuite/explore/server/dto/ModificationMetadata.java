/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@Getter
@AllArgsConstructor
@Builder
@Schema(description = "Modification metadata")
public class ModificationMetadata {
    @Schema(description = "Modification id")
    private UUID id;

    @Schema(description = "Modification name")
    private String name;

    @Schema(description = "Modification description")
    private String description;
}
