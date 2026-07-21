/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.NetworkConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore")
public class NetworkConversionController {

    private static final String HEADER_USER_ID = "userId";

    private final NetworkConversionService networkConversionService;

    public NetworkConversionController(NetworkConversionService networkConversionService) {
        this.networkConversionService = networkConversionService;
    }

    @GetMapping(value = "/cases/{caseUuid}/import-parameters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCaseImportParameters(@PathVariable("caseUuid") UUID caseUuid) {
        return networkConversionService.getCaseImportParameters(caseUuid);
    }

    @PostMapping(value = "/cases/{caseUuid}/convert/{format}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> convertCase(@PathVariable("caseUuid") UUID caseUuid,
                                            @PathVariable("format") String format,
                                            @RequestParam(value = "fileName", required = false) String fileName,
                                            @RequestBody(required = false) String formatParameters,
                                            @RequestHeader(HEADER_USER_ID) String userId) {
        return networkConversionService.convertCase(caseUuid, format, fileName, formatParameters, userId);
    }

    @GetMapping(value = "/download-file/{exportUuid}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("exportUuid") UUID exportUuid) {
        return networkConversionService.downloadFile(exportUuid);
    }

    @GetMapping(value = "/export/formats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getExportFormats() {
        return networkConversionService.getExportFormats();
    }
}
