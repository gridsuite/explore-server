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
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.ExploreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ExploreApi.API_VERSION)
@Tag(name = "explore-server")
public class ExploreController {

    // /!\ This query parameter is used by the gateway to control access
    private static final String QUERY_PARAM_PARENT_DIRECTORY_ID = "parentDirectoryUuid";

    private final ExploreService exploreService;
    private final DirectoryService directoryService;

    public ExploreController(ExploreService exploreService, DirectoryService directoryService) {
        this.exploreService = exploreService;
        this.directoryService = directoryService;
    }

    @PostMapping(value = "/explore/studies/{studyName}/cases/{caseUuid}")
    @Operation(summary = "create a study from an existing case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> createStudyFromExistingCase(@PathVariable("studyName") String studyName,
                                                            @PathVariable("caseUuid") UUID caseUuid,
                                                            @RequestParam("description") String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestHeader("userId") String userId,
                                                            @RequestBody(required = false) Map<String, Object> importParams) {
        exploreService.createStudy(studyName, caseUuid, description, userId, parentDirectoryUuid, importParams);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/studies/{studyName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "create a study and import the case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> createStudy(@PathVariable("studyName") String studyName,
                                            @RequestPart("caseFile") MultipartFile caseFile,
                                            @RequestParam("description") String description,
                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                            @RequestHeader("userId") String userId) {
        exploreService.createStudy(studyName, caseFile, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/studies")
    @Operation(summary = "Duplicate a study")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> createStudy(@RequestParam("duplicateFrom") UUID sourceStudyUuid,
                                            @RequestParam("studyName") String studyName,
                                            @RequestParam("description") String description,
                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                            @RequestHeader("userId") String userId) {
        exploreService.createStudy(sourceStudyUuid, studyName, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases/{caseName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "create a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case creation request delegated to case server")})
    public ResponseEntity<Void> createCase(@PathVariable("caseName") String caseName,
                                           @RequestPart("caseFile") MultipartFile caseFile,
                                           @RequestParam("description") String description,
                                           @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                           @RequestHeader("userId") String userId) {
        exploreService.createCase(caseName, caseFile, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases")
    @Operation(summary = "Duplicate a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case duplication request delegated to case server")})
    public ResponseEntity<Mono<Void>> createCase(@RequestParam("duplicateFrom") UUID parentCaseUuid,
                                                 @RequestParam("caseName") String caseName,
                                                 @RequestParam("description") String description,
                                                 @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                 @RequestHeader("userId") String userId) {
        exploreService.createCase(caseName, description, userId, parentCaseUuid, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/script-contingency-lists/{listName}")
    @Operation(summary = "create a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Script contingency list has been created")})
    public ResponseEntity<Void> createScriptContingencyList(@PathVariable("listName") String listName,
                                                            @RequestBody(required = false) String content,
                                                            @RequestParam("description") String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestHeader("userId") String userId) {
        exploreService.createScriptContingencyList(listName, content, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/script-contingency-lists")
    @Operation(summary = "Duplicate a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Script contingency list has been created")})
    public ResponseEntity<Void> createScriptContingencyList(@RequestParam("duplicateFrom") UUID parentListId,
                                                            @RequestParam("listName") String listName,
                                                            @RequestParam("description") String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestHeader("userId") String userId) {
        exploreService.createScriptContingencyList(parentListId, listName, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists/{listName}")
    @Operation(summary = "create a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Form contingency list has been created")})
    public ResponseEntity<Void> createFormContingencyList(@PathVariable("listName") String listName,
                                                          @RequestBody(required = false) String content,
                                                          @RequestParam("description") String description,
                                                          @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                          @RequestHeader("userId") String userId) {
        exploreService.createFormContingencyList(listName, content, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists")
    @Operation(summary = "Duplicate a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Form contingency list has been created")})
    public ResponseEntity<Void> createFormContingencyList(@RequestParam("duplicateFrom") UUID parentListId,
                                                          @RequestParam("listName") String listName,
                                                          @RequestParam("description") String description,
                                                          @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                          @RequestHeader("userId") String userId) {
        exploreService.createFormContingencyList(parentListId, listName, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script contingency list from a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<Void> newScriptFromFormContingencyList(@PathVariable("id") UUID id,
                                                                 @PathVariable("scriptName") String scriptName,
                                                                 @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                                 @RequestHeader("userId") String userId) {
        exploreService.newScriptFromFormContingencyList(id, scriptName, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists/{id}/replace-with-script")
    @Operation(summary = "Replace a form contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list has been replaced successfully")})
    public ResponseEntity<Void> replaceFilterContingencyListWithScript(@PathVariable("id") UUID id,
                                                                       @RequestHeader("userId") String userId) {
        exploreService.replaceFormContingencyListWithScript(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Filter creation request delegated to filter server")})
    public ResponseEntity<Void> createFilter(@RequestBody String filter,
                                             @RequestParam("name") String filterName,
                                             @RequestParam("description") String description,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                             @RequestHeader("userId") String userId) {
        exploreService.createFilter(filter, filterName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters")
    @Operation(summary = "Duplicate a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Void> createFilter(@RequestParam("duplicateFrom") UUID parentFilterId,
                                             @RequestParam("name") String filterName,
                                             @RequestParam("description") String description,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                             @RequestHeader("userId") String userId) {
        exploreService.createFilter(filterName, description, parentFilterId, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script from a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Void> newScriptFromFilter(@PathVariable("id") UUID filterId,
                                                    @PathVariable("scriptName") String scriptName,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                    @RequestHeader("userId") String userId) {
        exploreService.newScriptFromFilter(filterId, scriptName, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters/{id}/replace-with-script")
    @Operation(summary = "Replace a filter with a script")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter has been replaced successfully")})
    public ResponseEntity<Void> replaceFilterWithScript(@PathVariable("id") UUID id,
                                                        @RequestHeader("userId") String userId) {
        exploreService.replaceFilterWithScript(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/explore/elements/{elementUuid}")
    @Operation(summary = "Remove directory/element")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Directory/element was successfully removed"))
    public ResponseEntity<Void> deleteElement(@PathVariable("elementUuid") UUID elementUuid,
                                              @RequestHeader("userId") String userId) {
        exploreService.deleteElement(elementUuid, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/explore/elements/metadata")
    @Operation(summary = "get element infos from ids given as parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The elements information")})
    public ResponseEntity<List<ElementAttributes>> getElementsMetadata(@RequestParam("ids") List<UUID> ids) {
        directoryService.getElementsMetadata(ids);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .build();
    }
}
