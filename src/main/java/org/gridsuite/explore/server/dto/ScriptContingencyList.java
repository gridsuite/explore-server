package org.gridsuite.explore.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gridsuite.explore.server.utils.ContingencyListType;

import java.util.Date;
import java.util.UUID;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ScriptContingencyList {
    private String name;

    @Schema(description = "Script")
    private String script;
    private ContingencyListMetadataImpl metadata;

    @Schema(description = "List id")
    private UUID id;

    @Schema(description = "List type")
    private ContingencyListType type;

    @Schema(description = "Modification Date")
    Date modificationDate;

}
