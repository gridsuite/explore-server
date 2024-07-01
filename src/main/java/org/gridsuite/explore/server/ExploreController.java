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
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.ExploreService;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    private static final String QUERY_PARAM_TYPE = "type";

    private final ExploreService exploreService;
    private final DirectoryService directoryService;

    public ExploreController(ExploreService exploreService, DirectoryService directoryService) {
        this.exploreService = exploreService;
        this.directoryService = directoryService;
    }

    @PostMapping(value = "/explore/studies/{studyName}/cases/{caseUuid}")
    @Operation(summary = "create a study from an existing case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> createStudy(@PathVariable("studyName") String studyName,
                                                            @PathVariable("caseUuid") UUID caseUuid,
                                                            @RequestParam(name = "caseFormat") String caseFormat,
                                                            @RequestParam(name = "duplicateCase", required = false, defaultValue = "false") Boolean duplicateCase,
                                                            @RequestParam("description") String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestHeader("userId") String userId,
                                                            @RequestBody(required = false) Map<String, Object> importParams) {
        CaseInfo caseInfo = new CaseInfo(caseUuid, caseFormat);
        exploreService.createStudy(studyName, caseInfo, description, userId, parentDirectoryUuid, importParams, duplicateCase);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/studies", params = "duplicateFrom")
    @Operation(summary = "Duplicate a study")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> duplicateStudy(@RequestParam("duplicateFrom") UUID studyId,
                                               @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                               @RequestHeader("userId") String userId) {
        exploreService.duplicateStudy(studyId, targetDirectoryId, userId);
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

    @PostMapping(value = "/explore/cases", params = "duplicateFrom")
    @Operation(summary = "Duplicate a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case duplication request delegated to case server")})
    public ResponseEntity<Void> duplicateCase(
            @RequestParam("duplicateFrom") UUID caseId,
            @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
            @RequestHeader("userId") String userId) {
        exploreService.duplicateCase(caseId, targetDirectoryId, userId);
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

    @PostMapping(value = "/explore/contingency-lists", params = "duplicateFrom")
    @Operation(summary = "Duplicate a contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Contingency list has been created")})
    public ResponseEntity<Void> duplicateContingencyList(
            @RequestParam("duplicateFrom") UUID contingencyListUuid,
            @RequestParam(name = QUERY_PARAM_TYPE) ContingencyListType contingencyListType,
            @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
            @RequestHeader("userId") String userId) {
        exploreService.duplicateContingencyList(contingencyListUuid, targetDirectoryId, userId, contingencyListType);
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

    @PostMapping(value = "/explore/identifier-contingency-lists/{listName}")
    @Operation(summary = "create an identifier contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Identifier contingency list has been created")})
    public ResponseEntity<Void> createIdentifierContingencyList(@PathVariable("listName") String listName,
                                                          @RequestBody(required = false) String content,
                                                          @RequestParam("description") String description,
                                                          @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                          @RequestHeader("userId") String userId) {
        exploreService.createIdentifierContingencyList(listName, content, description, userId, parentDirectoryUuid);
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

    @PostMapping(value = "/explore/filters", params = "duplicateFrom")
    @Operation(summary = "Duplicate a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Void> duplicateFilter(
                                             @RequestParam("duplicateFrom") UUID filterId,
                                             @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                             @RequestHeader("userId") String userId) {
        exploreService.duplicateFilter(filterId, targetDirectoryId, userId);
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory/element was successfully removed"),
        @ApiResponse(responseCode = "404", description = "Directory/element was not found"),
        @ApiResponse(responseCode = "403", description = "Access forbidden for the directory/element")
    })
    public ResponseEntity<Void> deleteElement(@PathVariable("elementUuid") UUID elementUuid,
                                              @RequestHeader("userId") String userId) {
        exploreService.deleteElement(elementUuid, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/explore/elements/{directoryUuid}", params = "ids")
    @Operation(summary = "Remove directories/elements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "directories/elements was successfully removed"),
        @ApiResponse(responseCode = "404", description = "At least one directory/element was not found"),
        @ApiResponse(responseCode = "403", description = "Access forbidden for at least one directory/element")
    })
    public ResponseEntity<Void> deleteElements(@RequestParam("ids") List<UUID> elementsUuid,
                                               @RequestHeader("userId") String userId,
                                               @PathVariable UUID directoryUuid) {
        exploreService.deleteElementsFromDirectory(elementsUuid, directoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/explore/elements/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get element infos from ids given as parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The elements information")})
    public ResponseEntity<List<ElementAttributes>> getElementsMetadata(@RequestParam("ids") List<UUID> ids,
                                                                       @RequestParam(value = "equipmentTypes", required = false) List<String> equipmentTypes,
                                                                       @RequestParam(value = "elementTypes", required = false) List<String> elementTypes) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getElementsMetadata(ids, elementTypes, equipmentTypes));
    }

    @PutMapping(value = "/explore/filters/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter has been successfully modified")})
    public ResponseEntity<Void> changeFilter(@PathVariable UUID id, @RequestBody String filter, @RequestHeader("userId") String userId, @RequestParam("name") String name) {
        exploreService.updateFilter(id, filter, userId, name);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list have been modified successfully")})
    public ResponseEntity<Void> updateContingencyList(
            @PathVariable UUID id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "contingencyListType") ContingencyListType contingencyListType,
            @RequestBody String content,
            @RequestHeader("userId") String userId) {

        exploreService.updateContingencyList(id, content, userId, name, contingencyListType);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters creation request delegated to corresponding server")})
    public ResponseEntity<Void> createParameters(@RequestBody String parameters,
                                             @RequestParam("name") String parametersName,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                             @RequestHeader("userId") String userId) {
        exploreService.createParameters(parameters, parametersType, parametersName, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/parameters/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters have been successfully modified")})
    public ResponseEntity<Void> updateParameters(@PathVariable UUID id,
                                             @RequestBody String parameters,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestHeader("userId") String userId,
                                             @RequestParam("name") String name) {
        exploreService.updateParameters(id, parameters, parametersType, userId, name);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/parameters", params = "duplicateFrom")
    @Operation(summary = "Duplicate parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters have been successfully duplicated")})
    public ResponseEntity<Void> duplicateParameters(@RequestParam("duplicateFrom") UUID parametersId,
                                                    @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                    @RequestParam(name = QUERY_PARAM_TYPE) ParametersType parametersType,
                                                    @RequestHeader("userId") String userId) {
        exploreService.duplicateParameters(parametersId, targetDirectoryId, parametersType, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/modifications")
    @Operation(summary = "create some modification elements from existing network modifications")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Modifications have been duplicated and corresponding elements created in the directory")})
    public ResponseEntity<Void> createNetworkModifications(@RequestBody List<ElementAttributes> bodyContent,
                                                                @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                                @RequestHeader("userId") String userId) {
        exploreService.createNetworkModifications(bodyContent, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/modifications", params = "duplicateFrom")
    @Operation(summary = "duplicate modification element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Modifications have been duplicated and corresponding elements created in the directory")})
    public ResponseEntity<Void> duplicateNetworkModifications(@RequestParam("duplicateFrom") UUID networkModificationId,
                                                              @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                              @RequestHeader("userId") String userId) {
        exploreService.duplicateNetworkModifications(networkModificationId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/studies/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a study")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The study has been modified successfully")})
    public ResponseEntity<Void> updateStudy(
            @PathVariable UUID id,
            @RequestBody ElementAttributes elementAttributes,
            @RequestHeader("userId") String userId) {

        exploreService.updateStudy(id, elementAttributes, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/elements", params = "targetDirectoryUuid")
    @Operation(summary = "Move elements within directory tree")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Elements was successfully updated"),
        @ApiResponse(responseCode = "404", description = "The elements or the targeted directory was not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized execute this update")
    })
    public ResponseEntity<Void> moveElementsDirectory(
            @RequestParam UUID targetDirectoryUuid,
            @RequestBody List<UUID> elementsUuids,
            @RequestHeader("userId") String userId) {
        exploreService.moveElementsDirectory(elementsUuids, targetDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

}
