/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.dto.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.gridsuite.explore.server.utils.filter.FilterType;
import java.util.*;


/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@Schema(description = "Identifier list Filters", allOf = AbstractFilter.class)
@SuperBuilder
@NoArgsConstructor
public class IdentifierListFilter extends AbstractFilter {

    private List<IdentifierListFilterEquipmentAttributes> filterEquipmentsAttributes = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public FilterType getType() {
        return FilterType.IDENTIFIER_LIST;
    }

}
