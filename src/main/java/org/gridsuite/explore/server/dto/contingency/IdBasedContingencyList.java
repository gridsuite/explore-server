/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto.contingency;

import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.gridsuite.explore.server.utils.ContingencyListType;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Id based contingency list")
public class IdBasedContingencyList {

    private String name;
    private ContingencyListMetadataImpl metadata;

    private UUID id;

    private ContingencyListType type;

    private Date modificationDate;
    private IdentifierContingencyList identifierContingencyList;

}
