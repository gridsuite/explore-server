/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.ExploreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION)
@Tag(name = "explore-server")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @PostMapping(value = "/explore/studies/{studyName}/cases/{caseUuid}")
    @Operation(summary = "create a study from an existing case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Mono<Void>> createStudyFromExistingCase(@PathVariable("studyName") String studyName,
                                                                  @PathVariable("caseUuid") UUID caseUuid,
                                                                  @RequestParam("description") String description,
                                                                  @RequestParam("isPrivate") Boolean isPrivate,
                                                                  @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                                  @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.createStudy(studyName, caseUuid, description, userId, isPrivate, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/studies/{studyName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "create a study and import the case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Mono<Void>> createStudy(@PathVariable("studyName") String studyName,
                                                  @RequestPart("caseFile") FilePart caseFile,
                                                  @RequestParam("description") String description,
                                                  @RequestParam("isPrivate") Boolean isPrivate,
                                                  @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                  @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.createStudy(studyName, Mono.just(caseFile), description, userId, isPrivate, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/script-contingency-lists/{listName}")
    @Operation(summary = "create a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Script contingency list has been created")})
    public ResponseEntity<Mono<Void>> createScriptContingencyList(@PathVariable("listName") String listName,
                                                                  @RequestBody(required = false) String content,
                                                                  @RequestParam("description") String description,
                                                                  @RequestParam("isPrivate") Boolean isPrivate,
                                                                  @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                                  @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.createScriptContingencyList(listName, content, description, userId, isPrivate, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/filters-contingency-lists/{listName}")
    @Operation(summary = "create a filters contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Filters contingency list has been created")})
    public ResponseEntity<Mono<Void>> createFiltersContingencyList(@PathVariable("listName") String listName,
                                                                   @RequestBody(required = false) String content,
                                                                   @RequestParam("description") String description,
                                                                   @RequestParam("isPrivate") Boolean isPrivate,
                                                                   @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                                   @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.createFiltersContingencyList(listName, content, description, userId, isPrivate, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/filters-contingency-lists/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script contingency list from a filters contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<Mono<Void>> newScriptFromFiltersContingencyList(@PathVariable("id") UUID id,
                                                                          @PathVariable("scriptName") String scriptName,
                                                                          @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                                          @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.newScriptFromFiltersContingencyList(id, scriptName, userId, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/filters-contingency-lists/{id}/replace-with-script")
    @Operation(summary = "Replace a filters contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filters contingency list has been replaced successfully")})
    public ResponseEntity<Mono<Void>> replaceFilterContingencyListWithScript(@PathVariable("id") UUID id,
                                                                             @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.replaceFilterContingencyListWithScript(id, userId));
    }

    @PostMapping(value = "/explore/filters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Filter creation request delegated to filter server")})
    public ResponseEntity<Mono<Void>> createFilter(@RequestBody String filter,
                                                   @RequestParam("name") String filterName,
                                                   @RequestParam("description") String description,
                                                   @RequestParam("isPrivate") Boolean isPrivate,
                                                   @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                   @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.createFilter(filter, filterName, description, isPrivate, parentDirectoryUuid, userId));
    }

    @PostMapping(value = "/explore/filters/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script from a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Mono<Void>> newScriptFromFilter(@PathVariable("id") UUID filterId,
                                                          @PathVariable("scriptName") String scriptName,
                                                          @RequestParam("parentDirectoryUuid") UUID parentDirectoryUuid,
                                                          @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.newScriptFromFilter(filterId, scriptName, userId, parentDirectoryUuid));
    }

    @PostMapping(value = "/explore/filters/{id}/replace-with-script")
    @Operation(summary = "Replace a filter with a script")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter has been replaced successfully")})
    public ResponseEntity<Mono<Void>> replaceFilterWithScript(@PathVariable("id") UUID id,
                                                              @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.replaceFilterWithScript(id, userId));
    }

    @DeleteMapping(value = "/explore/elements/{elementUuid}")
    @Operation(summary = "Remove directory/element")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Directory/element was successfully removed"))
    public ResponseEntity<Mono<Void>> deleteElement(@PathVariable("elementUuid") UUID elementUuid,
                                                    @RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(exploreService.deleteElement(elementUuid, userId));
    }

    @GetMapping(value = "/explore/elements/metadata")
    @Operation(summary = "get element infos from ids given as parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The elements information")})
    public ResponseEntity<Flux<ElementAttributes>> getElementsMetadata(@RequestParam("id") List<UUID> ids) {
        return ResponseEntity.ok().body(exploreService.getElementsMetadata(ids));
    }
}
