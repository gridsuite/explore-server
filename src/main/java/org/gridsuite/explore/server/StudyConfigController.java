/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.SpreadsheetConfigCollectionService;
import org.gridsuite.explore.server.services.SpreadsheetConfigService;
import org.gridsuite.explore.server.services.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore")
public class StudyConfigController {

    private final SpreadsheetConfigService spreadsheetConfigService;
    private final SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;
    private final WorkspaceService workspaceService;

    public StudyConfigController(SpreadsheetConfigService spreadsheetConfigService,
                                 SpreadsheetConfigCollectionService spreadsheetConfigCollectionService,
                                 WorkspaceService workspaceService) {
        this.spreadsheetConfigService = spreadsheetConfigService;
        this.spreadsheetConfigCollectionService = spreadsheetConfigCollectionService;
        this.workspaceService = workspaceService;
    }

    @GetMapping(value = "/spreadsheet-configs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSpreadsheetConfig(@PathVariable("id") UUID id) {
        return spreadsheetConfigService.getSpreadsheetConfig(id);
    }

    @GetMapping(value = "/spreadsheet-config-collections/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSpreadsheetConfigCollection(@PathVariable("id") UUID id) {
        return spreadsheetConfigCollectionService.getSpreadsheetConfigCollection(id);
    }

    @GetMapping(value = "/workspaces/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getWorkspace(@PathVariable("id") UUID id) {
        return workspaceService.getWorkspace(id);
    }
}
