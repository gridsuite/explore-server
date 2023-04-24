package org.gridsuite.explore.server.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterEquipments {
    @Schema(description = "filter id")
    private UUID filterId;

    @Schema(description = "filter name")
    private String filterName;

    @Schema(description = "equipments of filter")
    private List<IdentifiableAttributes> identifiableAttributes;

    @Schema(description = "equipments not found in network")
    private List<String> notFoundEquipments;
}
