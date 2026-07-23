/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.CaseService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping(value = "/cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> importCase(@RequestPart("file") MultipartFile file,
                                           @RequestParam(value = "withExpiration", required = false, defaultValue = "false") boolean withExpiration) {
        return caseService.importCaseWithoutDirectoryElementCreation(file, withExpiration);
    }

    @DeleteMapping(value = "/cases/{caseUuid}")
    public ResponseEntity<Void> deleteCase(@PathVariable("caseUuid") UUID caseUuid) {
        return caseService.deleteCase(caseUuid);
    }

    @GetMapping(value = "/cases/{caseUuid}")
    public ResponseEntity<Resource> downloadCase(@PathVariable("caseUuid") UUID caseUuid) {
        return caseService.downloadCase(caseUuid);
    }

    @GetMapping(value = "/cases/caseBaseName")
    public ResponseEntity<String> getBaseName(@RequestParam("caseName") String caseName) {
        return caseService.getBaseName(caseName);
    }
}
