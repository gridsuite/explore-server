/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.gridsuite.explore.server.utils.SheetType;

import java.util.List;
import java.util.UUID;

/**
 * @author Achour BERRAHMA <achour.berrahma at rte-france.com>
 */
@Data
@Builder
@Jacksonized
@Schema(name = "SpreadsheetConfigDto", description = "Spreadsheet configuration")
public class SpreadsheetConfigDto {

    @Schema(description = "Spreadsheet configuration ID")
    UUID id;

    @NotNull(message = "Sheet type is mandatory")
    @Schema(description = "Spreadsheet type")
    SheetType sheetType;

    @Schema(description = "Custom columns")
    List<CustomColumnDto> customColumns;

    @Data
    @AllArgsConstructor
    @Builder
    @Jacksonized
    @Schema(name = "CustomColumnDto", description = "Custom column configuration")
    public static class CustomColumnDto {

        @Schema(description = "Custom column ID")
        UUID id;

        @NotNull(message = "Column name is mandatory")
        @Schema(description = "Column name")
        String name;

        @Schema(description = "Column formula")
        String formula;
    }
}