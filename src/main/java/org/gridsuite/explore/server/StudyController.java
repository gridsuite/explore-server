/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.StudyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION + "/explore")
public class StudyController {

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @PostMapping(value = "/studies/{studyUuid}/filters/elements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> evaluateFiltersOnFirstRootNetwork(@PathVariable("studyUuid") UUID studyUuid,
                                                                    @RequestBody String filters) {
        return studyService.evaluateFiltersOnFirstRootNetwork(studyUuid, filters);
    }

    @GetMapping(value = "/servers/about", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getServersInfos(@RequestParam(value = "view", required = false) String view) {
        return studyService.getServersInfos(view);
    }
}
