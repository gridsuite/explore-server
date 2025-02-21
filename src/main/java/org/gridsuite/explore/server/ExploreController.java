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
import org.springframework.http.HttpStatus;
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
    private static final String QUERY_PARAM_NAME = "name";
    private static final String QUERY_PARAM_DESCRIPTION = "description";
    private static final String QUERY_PARAM_PARENT_DIRECTORY_ID = "parentDirectoryUuid";

    private static final String QUERY_PARAM_TYPE = "type";
    private static final String QUERY_PARAM_USER_ID = "userId";

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
                                                            @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                                            @RequestBody(required = false) Map<String, Object> importParams) {
        exploreService.assertCanCreateCase(userId);
        CaseInfo caseInfo = new CaseInfo(caseUuid, caseFormat);
        exploreService.createStudy(studyName, caseInfo, description, userId, parentDirectoryUuid, importParams, duplicateCase);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/studies", params = "duplicateFrom")
    @Operation(summary = "Duplicate a study")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    public ResponseEntity<Void> duplicateStudy(@RequestParam("duplicateFrom") UUID studyId,
                                               @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                               @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.assertCanCreateCase(userId);
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
                                           @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.assertCanCreateCase(userId);
        exploreService.createCase(caseName, caseFile, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases", params = "duplicateFrom")
    @Operation(summary = "Duplicate a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case duplication request delegated to case server")})
    public ResponseEntity<Void> duplicateCase(
            @RequestParam("duplicateFrom") UUID caseId,
            @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.assertCanCreateCase(userId);
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
                                                            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
                                                          @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createFormContingencyList(listName, content, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script contingency list from a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<Void> newScriptFromFormContingencyList(@PathVariable("id") UUID id,
                                                                 @PathVariable("scriptName") String scriptName,
                                                                 @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                                 @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.newScriptFromFormContingencyList(id, scriptName, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/form-contingency-lists/{id}/replace-with-script")
    @Operation(summary = "Replace a form contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list has been replaced successfully")})
    public ResponseEntity<Void> replaceFilterContingencyListWithScript(@PathVariable("id") UUID id,
                                                                       @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
                                                          @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
                                             @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createFilter(filter, filterName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters", params = "duplicateFrom")
    @Operation(summary = "Duplicate a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Void> duplicateFilter(
                                             @RequestParam("duplicateFrom") UUID filterId,
                                             @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                             @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateFilter(filterId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script from a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    public ResponseEntity<Void> newScriptFromFilter(@PathVariable("id") UUID filterId,
                                                    @PathVariable("scriptName") String scriptName,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                    @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.newScriptFromFilter(filterId, scriptName, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters/{id}/replace-with-script")
    @Operation(summary = "Replace a filter with a script")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter has been replaced successfully")})
    public ResponseEntity<Void> replaceFilterWithScript(@PathVariable("id") UUID id,
                                                        @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
                                              @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
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
                                               @RequestHeader(QUERY_PARAM_USER_ID) String userId,
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

    @GetMapping(value = "/explore/composite-modification/{id}/network-modifications", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get the basic information of the network modifications contained in a composite modification")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Basic infos from all the contained network modifications")})
    public ResponseEntity<List<Object>> getCompositeModificationContent(@PathVariable("id") UUID compositeModificationId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(exploreService.getCompositeModificationContent(compositeModificationId));
    }

    @PutMapping(value = "/explore/filters/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filter has been successfully modified")})
    public ResponseEntity<Void> changeFilter(@PathVariable UUID id, @RequestBody String filter, @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                             @RequestParam("name") String name, @RequestParam("description") String description) {
        exploreService.updateFilter(id, filter, userId, name, description);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list have been modified successfully")})
    public ResponseEntity<Void> updateContingencyList(
            @PathVariable UUID id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = QUERY_PARAM_DESCRIPTION) String description,
            @RequestParam(name = "contingencyListType") ContingencyListType contingencyListType,
            @RequestBody String content,
            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {

        exploreService.updateContingencyList(id, content, userId, name, description, contingencyListType);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/composite-modification/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a composite modification")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The composite modification has been modified successfully")})
    public ResponseEntity<Void> updateCompositeModification(
            @PathVariable UUID id,
            @RequestParam(name = "name") String name,
            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {

        exploreService.updateCompositeModification(id, userId, name);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters creation request delegated to corresponding server")})
    public ResponseEntity<Void> createParameters(@RequestBody String parameters,
                                             @RequestParam("name") String parametersName,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                             @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createParameters(parameters, parametersType, parametersName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/parameters/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters have been successfully modified")})
    public ResponseEntity<Void> updateParameters(@PathVariable UUID id,
                                             @RequestBody String parameters,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestHeader(QUERY_PARAM_USER_ID) String userId,
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
                                                    @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateParameters(parametersId, targetDirectoryId, parametersType, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/spreadsheet-configs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a spreadsheet configuration")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config created")})
    public ResponseEntity<Void> createSpreadsheetConfig(@RequestBody String spreadsheetConfigDto,
                                                        @RequestParam("name") String configName,
                                                        @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                        @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                        @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createSpreadsheetConfig(spreadsheetConfigDto, configName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/spreadsheet-config-collections", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection created")})
    public ResponseEntity<Void> createSpreadsheetConfigCollection(@RequestBody String spreadsheetConfigCollectionDto,
                                                                  @RequestParam("name") String collectionName,
                                                                  @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                                  @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                                  @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createSpreadsheetConfigCollection(spreadsheetConfigCollectionDto, collectionName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/spreadsheet-config-collections/merge", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new spreadsheet configuration collection duplicating and merging a list of existing configurations")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection created")})
    public ResponseEntity<Void> createSpreadsheetConfigCollectionFromConfigIds(@RequestBody List<UUID> configUuids,
                                                                               @RequestParam("name") String collectionName,
                                                                               @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                                               @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                                               @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createSpreadsheetConfigCollectionFromConfigIds(configUuids, collectionName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping(value = "/explore/spreadsheet-configs/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a spreadsheet configuration")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Spreadsheet config has been successfully modified")})
    public ResponseEntity<Void> updateSpreadsheetConfig(@PathVariable UUID id,
                                                        @RequestBody String spreadsheetConfigDto,
                                                        @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                                        @RequestParam("name") String name) {
        exploreService.updateSpreadsheetConfig(id, spreadsheetConfigDto, userId, name);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/explore/spreadsheet-config-collections/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Spreadsheet config collection has been successfully modified")})
    public ResponseEntity<Void> updateSpreadsheetConfigCollection(@PathVariable UUID id,
                                                        @RequestBody String spreadsheetConfigCollectionDto,
                                                        @RequestHeader(QUERY_PARAM_USER_ID) String userId,
                                                        @RequestParam("name") String name) {
        exploreService.updateSpreadsheetConfigCollection(id, spreadsheetConfigCollectionDto, userId, name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/explore/spreadsheet-configs/duplicate", params = "duplicateFrom")
    @Operation(summary = "Duplicate a spreadsheet configuration")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config has been successfully duplicated")})
    public ResponseEntity<Void> duplicateSpreadsheetConfig(@RequestParam("duplicateFrom") UUID sourceId,
                                                           @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                           @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateSpreadsheetConfig(sourceId, targetDirectoryId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/spreadsheet-config-collections/duplicate", params = "duplicateFrom")
    @Operation(summary = "Duplicate a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection has been successfully duplicated")})
    public ResponseEntity<Void> duplicateSpreadsheetConfigCollection(@RequestParam("duplicateFrom") UUID sourceId,
                                                           @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                           @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateSpreadsheetConfigCollection(sourceId, targetDirectoryId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/composite-modifications")
    @Operation(summary = "Create composite modification element from existing network modifications")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Modifications have been created and composite modification element created in the directory")})
    public ResponseEntity<Void> createCompositeModification(@RequestBody List<UUID> modificationAttributes,
                                                            @RequestParam(QUERY_PARAM_NAME) String name,
                                                            @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.createCompositeModification(modificationAttributes, userId, name, description, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/composite-modifications", params = "duplicateFrom")
    @Operation(summary = "duplicate modification element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Composite modification has been duplicated and corresponding element created in the directory")})
    public ResponseEntity<Void> duplicateCompositeNetworkModification(@RequestParam("duplicateFrom") UUID networkModificationId,
                                                                      @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                                      @RequestHeader(QUERY_PARAM_USER_ID) String userId) {
        exploreService.duplicateCompositeModification(networkModificationId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/elements/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify an element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The element has been modified successfully")})
    public ResponseEntity<Void> updateElement(
            @PathVariable UUID id,
            @RequestBody ElementAttributes elementAttributes,
            @RequestHeader("userId") String userId) {

        exploreService.updateElement(id, elementAttributes, userId);
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

    @GetMapping(value = "/explore/elements/users-identities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get users identities from the elements ids given as parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The users identities"),
    })
    public ResponseEntity<String> getUsersIdentities(@RequestParam("ids") List<UUID> ids) {
        String usersIdentities = exploreService.getUsersIdentities(ids);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(usersIdentities);
    }
}
