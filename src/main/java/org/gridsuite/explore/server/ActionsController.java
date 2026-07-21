/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.explore.server.services.ContingencyListService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore")
@Tag(name = "explore-server")
public class ActionsController {

    private final ContingencyListService contingencyListService;

    public ActionsController(ContingencyListService contingencyListService) {
        this.contingencyListService = contingencyListService;
    }

    @GetMapping(value = "/identifier-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get identifier contingency list by id from actions-server")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list"),
        @ApiResponse(responseCode = "404", description = "The identifier contingency list does not exists")})
    public ResponseEntity<String> getIdentifierContingencyList(@PathVariable("id") UUID id) {
        return contingencyListService.getIdentifierContingencyList(id);
    }

    @GetMapping(value = "/filters-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get filter based contingency list by id from actions-server")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter based contingency list"),
        @ApiResponse(responseCode = "404", description = "The filter based contingency list does not exists")})
    public ResponseEntity<String> getFilterBasedContingencyList(@PathVariable("id") UUID id) {
        return contingencyListService.getFilterBasedContingencyList(id);
    }
}
