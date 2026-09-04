/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.explore.server.ExploreApi;
import org.gridsuite.explore.server.UserAuthentication;
import org.gridsuite.explore.server.dto.CaseInfo;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionDTO;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.dto.ReferencingElementInfos;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.ExploreService;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Tag(name = "Explore server")
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

    // TODO
    @PostMapping(value = "/explore/studies/{studyName}/cases/{caseUuid}")
    @Operation(summary = "create a study from an existing case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createStudy(@PathVariable("studyName") String studyName,
                                                            @PathVariable("caseUuid") UUID caseUuid,
                                                            @RequestParam(name = "caseFormat") String caseFormat,
                                                            @RequestParam(name = "duplicateCase", required = false, defaultValue = "false") Boolean duplicateCase,
                                                            @RequestParam("description") String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid,
                                                            @RequestBody(required = false) Map<String, Object> importParams) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.assertCanCreateCase(userId);
        CaseInfo caseInfo = new CaseInfo(caseUuid, caseFormat);
        exploreService.createStudy(studyName, caseInfo, description, userId, parentDirectoryUuid, importParams, duplicateCase);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/studies/{id}/duplicate")
    @Operation(summary = "Duplicate a study")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Study creation request delegated to study server")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#studyId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateStudy(@PathVariable("id") UUID studyId,
                                               @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.assertCanCreateCase(userId);
        exploreService.duplicateStudy(studyId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases/{caseName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "create a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case creation request delegated to case server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createCase(@PathVariable("caseName") String caseName,
                                           @RequestPart("caseFile") MultipartFile caseFile,
                                           @RequestParam("description") String description,
                                           @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.assertCanCreateCase(userId);
        exploreService.createCase(caseName, caseFile, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases/{caseName}/persist", params = {"caseUuid", "description", QUERY_PARAM_PARENT_DIRECTORY_ID})
    @Operation(summary = "persist an existing case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case persist request delegated to case server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> persistCase(@PathVariable("caseName") String caseName,
                                           @RequestParam("caseUuid") UUID caseUuid,
                                           @RequestParam("description") String description,
                                           @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.assertCanCreateCase(userId);
        exploreService.persistCase(caseName, caseUuid, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/cases/{id}/duplicate")
    @Operation(summary = "Duplicate a case")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Case duplication request delegated to case server")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#caseId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateCase(
            @PathVariable("id") UUID caseId,
            @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.assertCanCreateCase(userId);
        exploreService.duplicateCase(caseId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/contingency-lists/{id}/duplicate")
    @Operation(summary = "Duplicate a contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Contingency list has been created")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#contingencyListUuid, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateContingencyList(
            @PathVariable("id") UUID contingencyListUuid,
            @RequestParam(name = QUERY_PARAM_TYPE) ContingencyListType contingencyListType,
            @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateContingencyList(contingencyListUuid, targetDirectoryId, userId, contingencyListType);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/identifier-contingency-lists/{listName}")
    @Operation(summary = "create an identifier contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Identifier contingency list has been created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createIdentifierContingencyList(@PathVariable("listName") String listName,
                                                          @RequestBody(required = false) String content,
                                                          @RequestParam("description") String description,
                                                          @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createIdentifierContingencyList(listName, content, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters-contingency-lists/{listName}")
    @Operation(summary = "create a filter based contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Filter based contingency list has been created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createFilterBasedContingencyList(@PathVariable("listName") String listName,
                                                                @RequestBody(required = false) String content,
                                                                @RequestParam("description") String description,
                                                                @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createFilterBasedContingencyList(listName, content, description, userId, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Filter creation request delegated to filter server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createFilter(@RequestBody String filter,
                                             @RequestParam("name") String filterName,
                                             @RequestParam("description") String description,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createFilter(filter, filterName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/filters/{id}/duplicate")
    @Operation(summary = "Duplicate a filter")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script has been created successfully")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#filterId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateFilter(
                                             @PathVariable("id") UUID filterId,
                                             @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateFilter(filterId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    // TODO: les deux endpoints suivants sont redondants ??? p-e moyen de refacto ?

    @DeleteMapping(value = "/explore/elements/{elementUuid}")
    @Operation(summary = "Remove directory/element")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory/element was successfully removed"),
        @ApiResponse(responseCode = "404", description = "Directory/element was not found"),
        @ApiResponse(responseCode = "403", description = "Access forbidden for the directory/element")
    })
    @PreAuthorize("@authorizationService.canDelete(#elementUuid)")
    public ResponseEntity<Void> deleteElement(@PathVariable("elementUuid") UUID elementUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
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
    @PreAuthorize("@authorizationService.canDelete(#elementsUuids)") // ça ne peut pas contenir de subDirectories, car ils n'apparaissent que dans l'arbre
    public ResponseEntity<Void> deleteElements(@RequestParam("elementsUuids") List<UUID> elementsUuids,
                                               @PathVariable UUID directoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.deleteElementsFromDirectory(elementsUuids, directoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    // TODO : l'endpoint getElements dans directory-server filtre les éléments sur lesquels on n'a pas les droits si strictMode = false, et renvoie une erreur si strictMode = true
    //  ici on a strictMode = true (peut être que c'est à revoir ?) -> @PreFilter à tester ?
    @GetMapping(value = "/explore/elements/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get element infos from ids given as parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The elements information")})
    @PreAuthorize("@authorizationService.canRead(#ids)") // TODO: strictMode => @PreAuthorize, non strictMode => @PreFilter
    public ResponseEntity<List<ElementAttributes>> getElementsMetadata(@RequestParam("ids") List<UUID> ids,
                                                                       @RequestParam(value = "equipmentTypes", required = false) List<String> equipmentTypes,
                                                                       @RequestParam(value = "elementTypes", required = false) List<String> elementTypes) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getElementsMetadata(ids, elementTypes, equipmentTypes, userId));
    }

    // TODO: ici je pense qu'on n'a pas besoin de permission ? ou alors READ ?
    //  actuellement on ne regarde pas si on a les droits (même dans l'endpoint de directory-server), on renvoie tout. On peut faire un @PreFilter ?
    @GetMapping(value = "/explore/elements/name", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get element names from ids given as parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The elements names")})
    public ResponseEntity<Map<UUID, String>> getElementsName(@RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getElementsName(ids));
    }

    // TODO: est ce que cet élément existe dans directory-server ??? car on l'appelle depuis network-modification-server
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
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> changeFilter(@PathVariable UUID id,
                                             @RequestBody String filter,
                                             @RequestParam("name") String name,
                                             @RequestParam("description") String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateFilter(id, filter, userId, name, description);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list have been modified successfully")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateContingencyList(
            @PathVariable UUID id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = QUERY_PARAM_DESCRIPTION) String description,
            @RequestParam(name = "contingencyListType") ContingencyListType contingencyListType,
            @RequestBody String content) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateContingencyList(id, content, userId, name, description, contingencyListType);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/parameters", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters creation request delegated to corresponding server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createParameters(@RequestBody String parameters,
                                             @RequestParam("name") String parametersName,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                             @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createParameters(parameters, parametersType, parametersName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/diagram-config", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "create diagram config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "diagram config creation request delegated to corresponding server")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createDiagramConfig(@RequestBody String diagramConfig,
                                                    @RequestParam("name") String diagramConfigName,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createDiagramConfig(diagramConfig, diagramConfigName, description, parentDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/diagram-config/{id}/duplicate")
    @Operation(summary = "Duplicate a diagram config")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "diagram config has been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#sourceId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateDiagramConfig(@PathVariable("id") UUID sourceId,
                                                           @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateDiagramConfig(sourceId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/diagram-config/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a diagram config")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Diagram config has been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateDiagramConfig(@PathVariable UUID id,
                                                    @RequestBody String diagramConfig,
                                                    @RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateDiagramConfig(id, diagramConfig, userId, name, description);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/explore/parameters/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters have been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateParameters(@PathVariable UUID id,
                                             @RequestBody String parameters,
                                             @RequestParam(name = QUERY_PARAM_TYPE, defaultValue = "") ParametersType parametersType,
                                             @RequestParam(QUERY_PARAM_NAME) String name,
                                             @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateParameters(id, parameters, parametersType, userId, name, description);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/parameters/{id}/duplicate")
    @Operation(summary = "Duplicate parameters")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "parameters have been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#parametersId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateParameters(@PathVariable("id") UUID parametersId,
                                                    @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId,
                                                    @RequestParam(name = QUERY_PARAM_TYPE) ParametersType parametersType) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateParameters(parametersId, targetDirectoryId, parametersType, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/spreadsheet-configs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a spreadsheet configuration")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createSpreadsheetConfig(@RequestBody String spreadsheetConfigDto,
                                                        @RequestParam("name") String configName,
                                                        @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                        @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createSpreadsheetConfig(spreadsheetConfigDto, configName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/spreadsheet-config-collections", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createSpreadsheetConfigCollection(@RequestBody String spreadsheetConfigCollectionDto,
                                                                  @RequestParam("name") String collectionName,
                                                                  @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                                  @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createSpreadsheetConfigCollection(spreadsheetConfigCollectionDto, collectionName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // TODO: je passe de canCreate à canDuplicateTo. Pertinent ?
    @PostMapping(value = "/explore/spreadsheet-config-collections/merge", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new spreadsheet configuration collection duplicating and merging a list of existing configurations")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection created")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#configUuids, #parentDirectoryUuid)")
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
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateSpreadsheetConfig(@PathVariable UUID id,
                                                        @RequestBody String spreadsheetConfigDto,
                                                        @RequestParam(QUERY_PARAM_NAME) String name,
                                                        @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateSpreadsheetConfig(id, spreadsheetConfigDto, userId, name, description);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/explore/spreadsheet-config-collections/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Spreadsheet config collection has been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateSpreadsheetConfigCollection(@PathVariable UUID id,
                                                        @RequestBody String spreadsheetConfigCollectionDto,
                                                        @RequestParam(QUERY_PARAM_NAME) String name,
                                                        @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateSpreadsheetConfigCollection(id, spreadsheetConfigCollectionDto, userId, name, description);
        return ResponseEntity.noContent().build();
    }

    // TODO: on rajoute un canRead ?
    @PutMapping(value = "/explore/spreadsheet-config-collections/{id}/spreadsheet-configs/replace-all", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace all spreadsheet configurations in a collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Spreadsheet config collection has been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id) && @authorizationService.canRead(configUuids)")
    public ResponseEntity<Void> replaceAllSpreadsheetConfigsInCollection(@PathVariable UUID id,
                                                                        @RequestBody List<UUID> configUuids,
                                                                        @RequestParam(QUERY_PARAM_NAME) String name,
                                                                        @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.replaceAllSpreadsheetConfigsInCollection(id, configUuids, userId, name, description);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/explore/spreadsheet-configs/{id}/duplicate")
    @Operation(summary = "Duplicate a spreadsheet configuration")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config has been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#sourceId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateSpreadsheetConfig(@PathVariable("id") UUID sourceId,
                                                           @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateSpreadsheetConfig(sourceId, targetDirectoryId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // TODO: je passe de canCreate à canDuplicateTo. Pertinent ?
    @PostMapping(value = "/explore/workspaces", params = "workspaceId")
    @Operation(summary = "Create a workspace by duplicating an existing workspace")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Workspace created")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#workspaceId, #parentDirectoryUuid)")
    public ResponseEntity<Void> createWorkspace(@RequestParam("workspaceId") UUID workspaceId,
                                                @RequestParam("name") String workspaceName,
                                                @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createWorkspace(workspaceId, workspaceName, description, parentDirectoryUuid, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // TODO: je rajoute un canRead ?
    @PutMapping(value = "/explore/workspaces/{id}", params = "workspaceId")
    @Operation(summary = "Replace a workspace with another workspace")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Workspace has been successfully replaced")})
    @PreAuthorize("@authorizationService.canWrite(#id) && @authorizationService.canRead(#workspaceId)")
    public ResponseEntity<Void> replaceWorkspace(@PathVariable UUID id,
                                                 @RequestParam("workspaceId") UUID workspaceId,
                                                 @RequestParam(QUERY_PARAM_NAME) String name,
                                                 @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.replaceWorkspace(id, workspaceId, userId, name, description);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/explore/workspaces/{id}/duplicate")
    @Operation(summary = "Duplicate a workspace")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Workspace has been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#sourceId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateWorkspace(@PathVariable("id") UUID sourceId,
                                                   @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateWorkspace(sourceId, targetDirectoryId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/spreadsheet-config-collections/{id}/duplicate")
    @Operation(summary = "Duplicate a spreadsheet configuration collection")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Spreadsheet config collection has been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#sourceId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateSpreadsheetConfigCollection(@PathVariable("id") UUID sourceId,
                                                           @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateSpreadsheetConfigCollection(sourceId, targetDirectoryId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/explore/composite-modifications")
    @Operation(summary = "Create composite modification element from existing network modifications")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Modifications have been created and composite modification element created in the directory")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryUuid)")
    public ResponseEntity<Void> createCompositeModification(@RequestBody List<UUID> modificationAttributes,
                                                            @RequestParam(QUERY_PARAM_NAME) String name,
                                                            @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                            @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.createCompositeModification(modificationAttributes, userId, name, description, parentDirectoryUuid);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/explore/composite-modifications/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a composite modification")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The composite modification has been modified successfully")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateCompositeNetworkModification(@PathVariable UUID id,
                                                                   @RequestBody List<UUID> modificationUuids,
                                                                   @RequestParam(QUERY_PARAM_NAME) String name,
                                                                   @RequestParam(QUERY_PARAM_DESCRIPTION) String description) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateCompositeModification(id, modificationUuids, userId, name, description);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/composite-modifications/{id}/duplicate")
    @Operation(summary = "duplicate modification element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Composite modification has been duplicated and corresponding element created in the directory")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#networkModificationId, #targetDirectoryId)")
    public ResponseEntity<Void> duplicateCompositeNetworkModification(@PathVariable("id") UUID networkModificationId,
                                                                      @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.duplicateCompositeModification(networkModificationId, targetDirectoryId, userId);
        return ResponseEntity.ok().build();
    }

    // TODO
    @PutMapping(value = "/explore/elements/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify an element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The element has been modified successfully")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateElement(
            @PathVariable UUID id,
            @RequestBody ElementAttributes elementAttributes) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
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
    @PreAuthorize("@authorizationService.canMoveTo(#elementsUuids, #targetDirectoryUuid)")
    public ResponseEntity<Void> moveElementsDirectory(
            @RequestParam UUID targetDirectoryUuid,
            @RequestBody List<UUID> elementsUuids) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.moveElementsDirectory(elementsUuids, targetDirectoryUuid, userId);
        return ResponseEntity.ok().build();
    }

    // TODO: ici dans getUsersIdentities, ça appelle directory-server getElements qui vérifie si on a le droit, avec strictMode = true.
    //  Je met un PreAuthorize, voir si on met un PreFilter à la place
    @GetMapping(value = "/explore/elements/users-identities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "get users identities from the elements ids given as parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The users identities"),
    })
    @PreAuthorize("@authorizationService.canRead(#ids)")
    public ResponseEntity<String> getUsersIdentities(@RequestParam("ids") List<UUID> ids) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        String usersIdentities = exploreService.getUsersIdentities(ids, userId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(usersIdentities);
    }

    // TODO: PostFilter ? et virer la vérification dans l'endpoint directory-server
    //  Mais pas très efficace, on ferait 2 requêtes eu lieu d'une
    //  Par contre c'est plus dans la logique de mon refacto
    @GetMapping(value = "/explore/directories/root-directories", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get root directories")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The root directories"))
    public ResponseEntity<String> getRootDirectories(@RequestParam(value = "elementTypes", required = false, defaultValue = "") List<String> types) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().body(directoryService.getRootDirectories(types, userId));
    }

    // TODO: idem que au dessus: PostFilter ?
    @RequestMapping(value = "explore/directories/root-directories", method = RequestMethod.HEAD)
    @Operation(summary = "Get if a root directory of this name exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The root directory exists"),
        @ApiResponse(responseCode = "204", description = "The root directory doesn't exist"),
    })
    public ResponseEntity<Void> rootDirectoryExists(@RequestParam("directoryName") String directoryName) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.status(directoryService.rootDirectoryExists(directoryName, userId)).contentType(MediaType.APPLICATION_JSON).build();
    }

    // TODO: ici tout le monde a les droits -> pas de @PreAuthorize
    @PostMapping(value = "/explore/directories/root-directories", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create root directory")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The created root directory"))
    public ResponseEntity<String> createRootDirectory(@RequestBody String rootDirectoryAttributes) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.createRootDirectory(rootDirectoryAttributes, userId));
    }

    // TODO: je rajoute un canRead ? il est déjà côté directory-server : hasReadPermission(directoryUuid) et renvoie List.of()
    @GetMapping(value = "/explore/directories/{directoryUuid}/elements", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get directory elements")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "List directory's elements"))
    @PreAuthorize("@authorizationService.canRead(#directoryUuid)") // renvoie une erreur, alors qu'actuellement on renvoie juste une liste vide
    public ResponseEntity<String> getDirectoryElements(@PathVariable("directoryUuid") UUID directoryUuid,
                                                       @RequestParam(value = "elementTypes", required = false, defaultValue = "") List<String> types,
                                                       @RequestParam(value = "recursive", required = false, defaultValue = "false") Boolean recursive) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getDirectoryElements(directoryUuid, types, recursive, userId));
    }

    @PostMapping(value = "/explore/directories/{directoryUuid}/directories", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a subdirectory")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The created directory"),
        @ApiResponse(responseCode = "409", description = "A directory with the same name already exists in the directory")})
    @PreAuthorize("@authorizationService.canWrite(#directoryUuid)")
    public ResponseEntity<ElementAttributes> createDirectory(@PathVariable("directoryUuid") UUID directoryUuid,
                                                             @RequestBody ElementAttributes elementAttributes) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.createElement(elementAttributes, directoryUuid, userId));
    }

    // TODO: est ce que je rajoute un canRead ici ? ça ne sert pas à grand chose, de toute manière on ne peut pas ouvrir l'élément par la suite si on n'a pas les droits
    //  par contre vérifier s'il n'y a pas des infos dans le path. Il n'y a que les parentDirectories et le depth de l'élément
    //  donc j'aurais tendance à dire qu'on veut justement que ce soit accessible à tous
    @GetMapping(value = "/explore/directories/elements/{elementUuid}/path", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get path of element")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List info of an element and its parents in order to get its path"),
        @ApiResponse(responseCode = "403", description = "Access forbidden for the element"),
        @ApiResponse(responseCode = "404", description = "The searched element was not found")})
    public ResponseEntity<String> getPath(@PathVariable("elementUuid") UUID elementUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getPath(elementUuid, userId));
    }

    // TODO: accessible à tout le monde ; pas de @PreAuthorize
    @RequestMapping(method = RequestMethod.HEAD, value = "/explore/directories/{directoryUuid}/elements/{elementName}/types/{type}")
    @Operation(summary = "Check if an element with this name and this type already exists in the given directory")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The element exists"),
        @ApiResponse(responseCode = "204", description = "The element doesn't exist")})
    @PreAuthorize("true")
    public ResponseEntity<Void> elementExists(@PathVariable("directoryUuid") UUID directoryUuid,
                                              @PathVariable("elementName") String elementName,
                                              @PathVariable("type") String type) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.status(directoryService.elementExists(directoryUuid, elementName, type, userId)).contentType(MediaType.APPLICATION_JSON).build();
    }

    // TODO: vérif dans endpoint de directory-server canRead(directoryUuid) -> je le rajoute ici en @PreAuthorize, à suppr dans directory-server ??
    @GetMapping(value = "/explore/directories/{directoryUuid}/{elementName}/newNameCandidate")
    @Operation(summary = "Get a free name in directory based on the one given and it's type")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "If the element exists or not")})
    @PreAuthorize("@authorizationService.canRead(#directoryUuid)")
    public ResponseEntity<String> elementNameCandidate(@PathVariable("directoryUuid") UUID directoryUuid,
                                                       @PathVariable("elementName") String elementName,
                                                       @RequestParam("type") String type) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(directoryService.getNameCandidate(directoryUuid, elementName, type, userId));
    }

    // TODO: accessible à tout le monde
    @GetMapping(value = "/explore/directories/elements/indexation-infos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search elements in elasticsearch")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of elements found")})
    @PreAuthorize("true")
    public ResponseEntity<String> searchElements(
            @Parameter(description = "User input") @RequestParam(value = "userInput") String userInput,
            @Parameter(description = "Current directory UUID") @RequestParam(value = "directoryUuid", required = false, defaultValue = "") String directoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(directoryService.searchElements(userInput, directoryUuid, userId));
    }

    // TODO: accessible à tous ??? ou remplacer entièrement par @PreAuthorize... (bof)
    @GetMapping(value = "/explore/elements/{elementUuid}")
    @Operation(summary = "Check if user has a given right on a directory, or a single element by checking its parent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The user has the right on the element"),
        @ApiResponse(responseCode = "204", description = "The user has not the right on the element"),
    })
    @PreAuthorize("true")
    public ResponseEntity<Void> hasRight(@PathVariable("elementUuid") UUID elementUuid,
                                         @RequestParam(name = "permission") PermissionType permission) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        directoryService.checkPermission(List.of(elementUuid), null, userId, permission);
        return ResponseEntity.ok().build();
    }

    // TODO: verif READ dans endpoint directory-server sur les studyUuids retournées car on leur fait getElementsInfos en strictMode = false ;
    //  voir ce qu'on en fait, je ne sais pas
    //  je rajoute le canRead sur l'elementUuid'
    @GetMapping(value = "/explore/elements/{elementUuid}/referencing-element-infos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the elements using the given shared element")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The infos of the elements using the shared element"),
        @ApiResponse(responseCode = "404", description = "The shared element was not found"),
    })
    @PreAuthorize("@authorizationService.canRead(#elementUuid)")
    public ResponseEntity<List<ReferencingElementInfos>> getReferencingElementInfos(@PathVariable("elementUuid") UUID elementUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(exploreService.getReferencingElementInfos(elementUuid, userId));
    }

    @GetMapping(value = "/explore/directories/{directoryUuid}/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get permissions for a directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The permissions for the directory"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view permissions for this directory"),
        @ApiResponse(responseCode = "404", description = "The directory was not found")
    })
    @PreAuthorize("@authorizationService.canRead(#directoryUuid)")
    public ResponseEntity<List<PermissionDTO>> getDirectoryPermissions(@PathVariable("directoryUuid") UUID directoryUuid) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(directoryService.getDirectoryPermissions(directoryUuid, userId));
    }

    // TODO
    @PutMapping(value = "/explore/directories/{directoryUuid}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set permissions for a directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permissions were successfully updated"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update permissions for this directory"),
        @ApiResponse(responseCode = "404", description = "The directory was not found")
    })
    @PreAuthorize("@authorizationService.canManage(#directoryUuid)")
    public ResponseEntity<Void> setDirectoryPermissions(@PathVariable("directoryUuid") UUID directoryUuid,
                                                        @RequestBody List<PermissionDTO> permissions) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        directoryService.setDirectoryPermissions(directoryUuid, permissions, userId);
        return ResponseEntity.ok().build();
    }

    // TODO
    @PostMapping(value = "/explore/process-configs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryId)")
    public ResponseEntity<UUID> createProcessConfig(@RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryId,
                                                    @RequestBody(required = false) String processConfig) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().body(exploreService.createProcessConfig(name, processConfig, description, userId, parentDirectoryId));
    }

    // TODO
    @PutMapping(value = "/explore/process-configs/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateProcessConfig(@PathVariable UUID id,
                                                    @RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestBody(required = false) String processConfig) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateProcessConfig(id, name, processConfig, description, userId);
        return ResponseEntity.ok().build();
    }

    // TODO
    @PostMapping(value = "/explore/process-configs/{id}/duplicate")
    @Operation(summary = "Duplicate a process config")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Process config has been successfully created")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#id, #targetDirectoryId)")
    public ResponseEntity<UUID> duplicateProcessConfig(@PathVariable("id") UUID id,
                                                       @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        return ResponseEntity.ok().body(exploreService.duplicateProcessConfig(id, targetDirectoryId, userId));
    }

    @PostMapping(value = "/explore/dynamic-mappings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a dynamic mapping")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic mapping has been successfully created")})
    @PreAuthorize("@authorizationService.canWrite(#parentDirectoryId)")
    public ResponseEntity<UUID> createDynamicMapping(@RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestParam(QUERY_PARAM_PARENT_DIRECTORY_ID) UUID parentDirectoryId,
                                                    @RequestBody(required = false) String dynamicMapping) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        UUID newDynamicMappingUuid = exploreService.createDynamicMapping(name, dynamicMapping, description, userId, parentDirectoryId);
        return ResponseEntity.ofNullable(newDynamicMappingUuid);
    }

    @PutMapping(value = "/explore/dynamic-mappings/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a dynamic mapping")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic mapping has been successfully modified")})
    @PreAuthorize("@authorizationService.canWrite(#id)")
    public ResponseEntity<Void> updateDynamicMapping(@PathVariable UUID id,
                                                    @RequestParam(QUERY_PARAM_NAME) String name,
                                                    @RequestParam(QUERY_PARAM_DESCRIPTION) String description,
                                                    @RequestBody(required = false) String dynamicMapping) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        exploreService.updateDynamicMapping(id, name, dynamicMapping, description, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/explore/dynamic-mappings/{id}/duplicate")
    @Operation(summary = "Duplicate a dynamic mapping")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic mapping has been successfully duplicated")})
    @PreAuthorize("@authorizationService.canDuplicateTo(#id, #targetDirectoryId)")
    public ResponseEntity<UUID> duplicateDynamicMapping(@PathVariable("id") UUID id,
                                                       @RequestParam(name = QUERY_PARAM_PARENT_DIRECTORY_ID, required = false) UUID targetDirectoryId) {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        UUID newDynamicMappingUuid = exploreService.duplicateDynamicMapping(id, targetDirectoryId, userId);
        return ResponseEntity.ofNullable(newDynamicMappingUuid);
    }
}
