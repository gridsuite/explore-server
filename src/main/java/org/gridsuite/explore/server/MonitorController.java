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
import org.gridsuite.explore.server.services.ExploreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore/monitor")
@Tag(name = "Explore server - Monitor")
public class MonitorController {

    // /!\ This query parameter is used by the gateway to control access
    private static final String QUERY_PARAM_NAME = "name";
    private static final String QUERY_PARAM_DESCRIPTION = "description";
    private static final String QUERY_PARAM_PARENT_DIRECTORY_ID = "parentDirectoryUuid";

    private static final String QUERY_PARAM_USER_ID = "userId";
    private static final String QUERY_PARAM_DUPLICATE_FROM = "duplicateFrom";

    private final ExploreService exploreService;

    public MonitorController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @PostMapping(value = "/process-config", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully created")})
    @PreAuthorize("@authorizationService.isAuthorized(#userId, #parentDirectoryId, null, T(org.gridsuite.explore.server.dto.PermissionType).WRITE)")
    public ResponseEntity<Void> createProcessConfig(@RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryId,
                                                    @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                                    @RequestBody(required = false) String processConfig) {
        exploreService.createProcessConfig(name, processConfig, description, userId, parentDirectoryId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/process-config/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully modified")})
    @PreAuthorize("@authorizationService.isAuthorized(#userId, #id, null, T(org.gridsuite.explore.server.dto.PermissionType).WRITE)")
    public ResponseEntity<Void> updateProcessConfig(@PathVariable UUID id,
                                                    @RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                                    @RequestBody(required = false) String processConfig) {
        exploreService.updateProcessConfig(id, name, processConfig, description, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/process-config/duplication")
    @Operation(summary = "Duplicate a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully created")})
    @PreAuthorize("@authorizationService.isAuthorizedForDuplication(#userId, #id, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateProcessConfig(@RequestParam(QUERY_PARAM_DUPLICATE_FROM) UUID id,
                                                       @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                       @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateProcessConfig(id, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }
}
