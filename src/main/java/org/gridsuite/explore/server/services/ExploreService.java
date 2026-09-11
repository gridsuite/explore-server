/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.explore.server.UserAuthentication;
import org.gridsuite.explore.server.dto.CaseAlertThresholdMessage;
import org.gridsuite.explore.server.dto.CaseInfo;
import org.gridsuite.explore.server.dto.DirectoryElementStatus;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.NodeInfos;
import org.gridsuite.explore.server.dto.ReferenceAttributes;
import org.gridsuite.explore.server.dto.ReferencingElementInfos;
import org.gridsuite.explore.server.dto.UsersIdentities;
import org.gridsuite.explore.server.error.ExploreException;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gridsuite.explore.server.dto.DirectoryElementStatus.CREATING;
import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.EXPLORE_MAX_ELEMENTS_EXCEEDED;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Etienne Homer <jacques.borsenberger at rte-france.com>
 */

@Service
public class ExploreService {
    static final String STUDY = "STUDY";
    static final String CASE = "CASE";
    static final String CONTINGENCY_LIST = "CONTINGENCY_LIST";
    static final String FILTER = "FILTER";
    public static final String MODIFICATION = "MODIFICATION";
    static final String DIRECTORY = "DIRECTORY";
    static final String SPREADSHEET_CONFIG = "SPREADSHEET_CONFIG";
    static final String SPREADSHEET_CONFIG_COLLECTION = "SPREADSHEET_CONFIG_COLLECTION";
    static final String DIAGRAM_CONFIG = "DIAGRAM_CONFIG";
    static final String WORKSPACE = "WORKSPACE";
    static final String PROCESS_CONFIG = "PROCESS_CONFIG";
    static final String DYNAMIC_MAPPING = "DYNAMIC_MAPPING";

    private final DirectoryService directoryService;
    private final StudyService studyService;
    private final ContingencyListService contingencyListService;
    private final NetworkModificationService networkModificationService;
    private final FilterService filterService;
    private final CaseService caseService;
    private final ParametersService parametersService;
    private final SpreadsheetConfigService spreadsheetConfigService;
    private final SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;
    private final WorkspaceService workspaceService;
    private final UserIdentityService userIdentityService;
    private final NotificationService notificationService;
    private final MonitorService monitorService;
    private final DynamicMappingService dynamicMappingService;
    private final ExploreServerExecutionService exploreServerExecutionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);
    private final UserAdminService userAdminService;
    private final SingleLineDiagramService singleLineDiagramService;

    public ExploreService(
        DirectoryService directoryService,
        StudyService studyService,
        ContingencyListService contingencyListService,
        FilterService filterService,
        NetworkModificationService networkModificationService,
        CaseService caseService,
        ParametersService parametersService,
        UserAdminService userAdminService,
        SpreadsheetConfigService spreadsheetConfigService,
        SpreadsheetConfigCollectionService spreadsheetConfigCollectionService,
        WorkspaceService workspaceService,
        UserIdentityService userIdentityService,
        NotificationService notificationService,
        SingleLineDiagramService singleLineDiagramService,
        MonitorService monitorService,
        DynamicMappingService dynamicMappingService,
        ExploreServerExecutionService exploreServerExecutionService) {

        this.directoryService = directoryService;
        this.studyService = studyService;
        this.contingencyListService = contingencyListService;
        this.filterService = filterService;
        this.networkModificationService = networkModificationService;
        this.caseService = caseService;
        this.parametersService = parametersService;
        this.userAdminService = userAdminService;
        this.spreadsheetConfigService = spreadsheetConfigService;
        this.spreadsheetConfigCollectionService = spreadsheetConfigCollectionService;
        this.workspaceService = workspaceService;
        this.userIdentityService = userIdentityService;
        this.notificationService = notificationService;
        this.singleLineDiagramService = singleLineDiagramService;
        this.monitorService = monitorService;
        this.dynamicMappingService = dynamicMappingService;
        this.exploreServerExecutionService = exploreServerExecutionService;
    }

    public void createStudy(String studyName, CaseInfo caseInfo, String description, UUID parentDirectoryUuid, Map<String, Object> importParams, Boolean duplicateCase) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY, 0L, description, CREATING);

        String elementName = getElementName(caseInfo.caseUuid());

        studyService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), caseInfo.caseUuid(), caseInfo.caseFormat(), importParams, duplicateCase, elementName);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, studyService::delete);
    }

    private @Nullable String getElementName(UUID elementUuid) {
        String elementName = null;
        // Two scenarios to handle.
        try {
            // Scenario 1: the study is created from an existing case, so the case is available in the directory server.
            ElementAttributes caseAttributes = directoryService.getElementInfos(elementUuid);
            elementName = caseAttributes.getElementName();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
            // Scenario 2: the study is not created from an existing case, in which case the directory throws exception
            // because no element with the given uuid. Then, do nothing and keep elementName as null
        }
        return elementName;
    }

    public void duplicateStudy(UUID sourceStudyUuid, UUID targetDirectoryId) {
        UUID newStudyId = studyService.duplicateStudy(sourceStudyUuid);
        duplicateDirectoryElementOrDeleteElement(sourceStudyUuid, newStudyId, targetDirectoryId, CREATING, studyService::delete);
    }

    public void createCase(String caseName, MultipartFile caseFile, String description, UUID parentDirectoryUuid) {
        UUID uuid = caseService.importCase(caseFile);
        ElementAttributes elementAttributes = new ElementAttributes(uuid, caseName, CASE, 0L, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, caseService::delete);
    }

    public void persistCase(String caseName, UUID caseUuid, String description, UUID parentDirectoryUuid) {
        caseService.persistCase(caseUuid);
        ElementAttributes elementAttributes = new ElementAttributes(caseUuid, caseName, CASE, 0L, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, caseService::delete);
    }

    public void duplicateCase(UUID sourceCaseUuid, UUID targetDirectoryId) {
        UUID newCaseId = caseService.duplicateCase(sourceCaseUuid);
        duplicateDirectoryElementOrDeleteElement(sourceCaseUuid, newCaseId, targetDirectoryId, caseService::delete);
    }

    public void duplicateContingencyList(UUID contingencyListsId, UUID targetDirectoryId, ContingencyListType contingencyListType) {
        UUID newId = switch (contingencyListType) {
            case IDENTIFIERS -> contingencyListService.duplicateIdentifierContingencyList(contingencyListsId);
            case FILTERS -> contingencyListService.duplicateFilterBasedContingencyList(contingencyListsId);
        };
        duplicateDirectoryElementOrDeleteElement(contingencyListsId, newId, targetDirectoryId, contingencyListService::delete);
    }

    public void createIdentifierContingencyList(String listName, String content, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST, 0L, description);
        contingencyListService.insertIdentifierContingencyList(elementAttributes.getElementUuid(), content);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, contingencyListService::delete);
    }

    public void createFilterBasedContingencyList(String listName, String content, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST, 0L, description);
        contingencyListService.insertFilterBasedContingencyList(elementAttributes.getElementUuid(), content);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, contingencyListService::delete);
    }

    public void createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER, 0, description);
        filterService.insertFilter(filter, elementAttributes.getElementUuid());
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, filterService::delete);
    }

    public void duplicateFilter(UUID sourceFilterId, UUID targetDirectoryId) {
        UUID newFilterId = filterService.duplicateFilter(sourceFilterId);
        duplicateDirectoryElementOrDeleteElement(sourceFilterId, newFilterId, targetDirectoryId, filterService::delete);
    }

    public CompletableFuture<Void> deleteElement(UUID id) {
        return exploreServerExecutionService.runAsync(() -> doDeleteElement(id));
    }

    private void doDeleteElement(UUID id) {
        try {
            directoryService.updateElementsStatus(List.of(id), DirectoryElementStatus.DELETING);
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
            directoryService.deleteElement(id);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        } finally {
            directoryService.deleteDirectoryElement(id);
        }
    }

    public CompletableFuture<Void> deleteElementsFromDirectory(List<UUID> uuids, UUID parentDirectoryUuid) {
        return exploreServerExecutionService.runAsync(() -> doDeleteElementsFromDirectory(uuids, parentDirectoryUuid));
    }

    private void doDeleteElementsFromDirectory(List<UUID> uuids, UUID parentDirectoryUuid) {
        directoryService.updateElementsStatus(uuids, DirectoryElementStatus.DELETING);
        List<UUID> deletedIds = new ArrayList<>();
        List<UUID> failedIds = new ArrayList<>();
        for (UUID id : uuids) {
            try {
                directoryService.deleteElement(id);
                deletedIds.add(id);
            } catch (Exception e) {
                LOGGER.error("Failed to delete element {}", id, e);
                failedIds.add(id);
            }
        }
        if (!deletedIds.isEmpty()) {
            try {
                directoryService.deleteElementsFromDirectory(deletedIds, parentDirectoryUuid);
            } catch (Exception e) {
                LOGGER.error("Failed to remove deleted elements {} from directory", deletedIds, e);
                failedIds.addAll(deletedIds);
            }
        }
        if (!failedIds.isEmpty()) {
            directoryService.updateElementsStatus(failedIds, DirectoryElementStatus.CREATED);
        }
    }

    public void updateFilter(UUID id, String filter, String name, String description) {
        // check if the  user have the right to update the filter
        filterService.updateFilter(id, filter);

        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes);
    }

    public void updateContingencyList(UUID id, String content, String name, String description, ContingencyListType contingencyListType) {
        // check if the  user have the right to update the contingency
        contingencyListService.updateContingencyList(id, content, getProperPath(contingencyListType));
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes);
    }

    public void updateCompositeModification(UUID id, List<UUID> modificationUuids, String name, String description) {
        networkModificationService.replaceCompositeModification(id, name, modificationUuids);
        updateElementNameAndDescription(id, name, description);
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        Map<UUID, List<Object>> compositeContent = networkModificationService.getCompositeModificationContent(compositeModificationId);
        List<Object> requestedContent = compositeContent.get(compositeModificationId);
        return requestedContent != null ? requestedContent : List.of();
    }

    private void updateElementNameAndDescription(UUID id, String name, String description) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(name);
        elementAttributes.setDescription(description);
        directoryService.updateElement(id, elementAttributes);
    }

    private String getProperPath(ContingencyListType contingencyListType) {
        return switch (contingencyListType) {
            case IDENTIFIERS -> "/identifier-contingency-lists/{id}";
            case FILTERS -> "/filters-contingency-lists/{id}";
        };
    }

    public void createParameters(String parameters, ParametersType parametersType, String parametersName, String description, UUID parentDirectoryUuid) {
        UUID parametersUuid = parametersService.createParameters(parameters, parametersType);
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, parametersService::delete);
    }

    public void updateParameters(UUID id, String parameters, ParametersType parametersType, String name, String description) {
        parametersService.updateParameters(id, parameters, parametersType);
        updateElementNameAndDescription(id, name, description);
    }

    public void duplicateParameters(UUID sourceId, UUID targetDirectoryId, ParametersType parametersType) {
        UUID newParametersUuid = parametersService.duplicateParameters(sourceId, parametersType);
        duplicateDirectoryElementOrDeleteElement(sourceId, newParametersUuid, targetDirectoryId, parametersService::delete);
    }

    public void createDiagramConfig(String diagramConfig, String diagramConfigName, String description, UUID parentDirectoryUuid) {
        UUID diagramConfigUuid = singleLineDiagramService.createDiagramConfig(diagramConfig);
        ElementAttributes elementAttributes = new ElementAttributes(diagramConfigUuid, diagramConfigName, DIAGRAM_CONFIG, 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, singleLineDiagramService::delete);
    }

    public void duplicateDiagramConfig(UUID sourceId, UUID targetDirectoryId) {
        UUID newConfigUuid = singleLineDiagramService.duplicateDiagramConfig(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newConfigUuid, targetDirectoryId, singleLineDiagramService::delete);
    }

    public void updateDiagramConfig(UUID id, String diagramConfig, String name, String description) {
        singleLineDiagramService.updateDiagramConfig(id, diagramConfig);
        updateElementNameAndDescription(id, name, description);
    }

    public void createSpreadsheetConfig(String spreadsheetConfigDto, String configName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigService.createSpreadsheetConfig(spreadsheetConfigDto);
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, configName, SPREADSHEET_CONFIG, 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, spreadsheetConfigService::delete);
    }

    public void createSpreadsheetConfigCollection(String spreadsheetConfigCollectionDto, String collectionName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionService.createSpreadsheetConfigCollection(spreadsheetConfigCollectionDto);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid);
    }

    public void createSpreadsheetConfigCollectionFromConfigIds(List<UUID> configIds, String collectionName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionService.createSpreadsheetConfigCollectionFromConfigIds(configIds);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid);
    }

    private void createSpreadsheetConfigCollectionElement(UUID spreadsheetConfigUuid, String collectionName, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, collectionName, SPREADSHEET_CONFIG_COLLECTION, 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, spreadsheetConfigCollectionService::delete);
    }

    public void updateSpreadsheetConfig(UUID id, String spreadsheetConfigDto, String name, String description) {
        spreadsheetConfigService.updateSpreadsheetConfig(id, spreadsheetConfigDto);
        updateElementNameAndDescription(id, name, description);
    }

    public void updateSpreadsheetConfigCollection(UUID id, String spreadsheetConfigCollectionDto, String name, String description) {
        spreadsheetConfigCollectionService.updateSpreadsheetConfigCollection(id, spreadsheetConfigCollectionDto);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public void replaceAllSpreadsheetConfigsInCollection(UUID id, List<UUID> configIds, String name, String description) {
        spreadsheetConfigCollectionService.replaceAllSpreadsheetConfigsInCollection(id, configIds);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public void duplicateSpreadsheetConfig(UUID sourceId, UUID targetDirectoryId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigService.duplicateSpreadsheetConfig(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, spreadsheetConfigService::delete);
    }

    public void duplicateSpreadsheetConfigCollection(UUID sourceId, UUID targetDirectoryId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigCollectionService.duplicateSpreadsheetConfigCollection(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, spreadsheetConfigCollectionService::delete);
    }

    public void createWorkspace(UUID workspaceId, String workspaceName, String description, UUID parentDirectoryUuid) {
        UUID newWorkspaceId = workspaceService.duplicateWorkspace(workspaceId);
        ElementAttributes elementAttributes = new ElementAttributes(newWorkspaceId, workspaceName, WORKSPACE, 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, workspaceService::delete);
    }

    public void replaceWorkspace(UUID id, UUID workspaceId, String name, String description) {
        workspaceService.replaceWorkspace(id, workspaceId);
        updateElementNameAndDescription(id, name, description);
    }

    public void duplicateWorkspace(UUID sourceId, UUID targetDirectoryId) {
        UUID newWorkspaceId = workspaceService.duplicateWorkspace(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newWorkspaceId, targetDirectoryId, workspaceService::delete);
    }

    public void createCompositeModification(List<UUID> modificationUuids, String name,
                                            String description, UUID parentDirectoryUuid) {

        // create composite modifications
        UUID modificationsUuid = networkModificationService.createCompositeModification(modificationUuids, name);
        ElementAttributes elementAttributes = new ElementAttributes(modificationsUuid, name, MODIFICATION, 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, networkModificationService::delete);
    }

    public void duplicateCompositeModification(UUID sourceId, UUID parentDirectoryUuid) {
        // create duplicated modification
        Map<UUID, UUID> newModificationsUuids = networkModificationService.duplicateCompositeModifications(List.of(sourceId));
        UUID newNetworkModification = newModificationsUuids.get(sourceId);
        // create corresponding directory element
        duplicateDirectoryElementOrDeleteElement(sourceId, newNetworkModification, parentDirectoryUuid, networkModificationService::delete);
    }

    public void assertCanCreateCase() {
        String userId = ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication()).getUserId();
        Integer userMaxAllowedStudiesAndCases = userAdminService.getUserMaxAllowedCases(userId);
        if (userMaxAllowedStudiesAndCases != null) {
            int userCasesCount = directoryService.getUserCasesCount(userId);
            if (userCasesCount >= userMaxAllowedStudiesAndCases) {
                throw new ExploreException(EXPLORE_MAX_ELEMENTS_EXCEEDED, "max allowed cases reached", Map.of("limit", userMaxAllowedStudiesAndCases));
            }
            notifyCasesThresholdReached(userCasesCount, userMaxAllowedStudiesAndCases);
        }
    }

    public void notifyCasesThresholdReached(int userCasesCount, int userMaxAllowedStudiesAndCases) {
        Integer casesAlertThreshold = userAdminService.getCasesAlertThreshold();
        if (casesAlertThreshold != null) {
            int userCasesUsagePercentage = (100 * userCasesCount) / userMaxAllowedStudiesAndCases;
            if (userCasesUsagePercentage >= casesAlertThreshold) {
                CaseAlertThresholdMessage caseAlertThresholdMessage = new CaseAlertThresholdMessage(userCasesUsagePercentage, userCasesCount);
                notificationService.emitUserMessage("casesAlertThreshold", caseAlertThresholdMessage);
            }
        }
    }

    public void updateElement(UUID id, ElementAttributes elementAttributes) {
        // The check to know if the  user have the right to update the element is done in the directory-server
        directoryService.updateElement(id, elementAttributes);
        ElementAttributes elementsInfos = directoryService.getElementInfos(id);
        notifyElementUpdated(elementsInfos);
    }

    private void notifyElementUpdated(ElementAttributes element) {
        // send notification if the study name was updated
        if (STUDY.equals(element.getType())) {
            studyService.notifyStudyUpdate(element.getElementUuid());
        }

        // the composite modification name has to be updated in order to match the new element name
        if (MODIFICATION.equals(element.getType())) {
            networkModificationService.updateCompositeModification(element.getElementUuid(), element.getElementName());
        }
    }

    private void notifyElementMoved(ElementAttributes element) {
        // send notification if the study name was updated
        if (STUDY.equals(element.getType())) {
            studyService.notifyStudyUpdate(element.getElementUuid());
        }
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid) {
        directoryService.moveElementsDirectory(elementsUuids, targetDirectoryUuid);
        List<ElementAttributes> elementsAttributes = directoryService.getElementsInfos(elementsUuids, null);
        elementsAttributes.forEach(this::notifyElementMoved);

    }

    public String getUsersIdentities(List<UUID> elementsUuids) {
        // this returns names for owner and lastmodifiedby,
        // if we need it in the future, we can do separate requests.
        List<String> subs = directoryService.getElementsInfos(elementsUuids, null).stream()
                .flatMap(x -> Stream.of(x.getOwner(), x.getLastModifiedBy())).distinct().filter(Objects::nonNull).toList();
        return userIdentityService.getUsersIdentities(subs);
    }

    /**
     * Lists the elements using a shared element. There is one result per reference of the shared element.
     * Elements the user cannot read are omitted.
     */
    public List<ReferencingElementInfos> getReferencingElementInfos(UUID elementUuid) {
        // for now only STUDY_NODE references
        List<UUID> referencedNodeUuids = directoryService.getElementInfos(elementUuid).getReferences().stream()
                .filter(reference -> reference.getReferenceType() == ReferenceAttributes.ReferenceType.STUDY_NODE)
                // STUDY_NODE: the referenced node is the container's containerId
                .map(reference -> reference.getReferenceContainer().getContainerId())
                .toList();
        if (referencedNodeUuids.isEmpty()) {
            return List.of();
        }

        // a node can be referenced several times, and several nodes often belong to the same study: query each only once
        Map<UUID, NodeInfos> nodeInfosByUuid = studyService.getNodesInfos(referencedNodeUuids.stream().distinct().toList())
                .stream().collect(Collectors.toMap(NodeInfos::nodeUuid, Function.identity()));
        List<UUID> studyUuids = nodeInfosByUuid.values().stream().map(NodeInfos::studyUuid).distinct().toList();
        if (studyUuids.isEmpty()) {
            return List.of();
        }

        Map<UUID, ElementAttributes> studyByUuid = directoryService.getElementsInfos(studyUuids, null, false)
                .stream().collect(Collectors.toMap(ElementAttributes::getElementUuid, Function.identity()));
        Map<UUID, List<String>> parentDirectoryNamesByStudyUuid = getParentDirectoryNames(studyByUuid.keySet());
        Map<String, UsersIdentities.UserIdentity> identityBySub = getIdentityBySub(studyByUuid.values());

        return referencedNodeUuids.stream()
                .map(nodeInfosByUuid::get)
                .filter(Objects::nonNull)
                .map(nodeInfos -> toReferencingElementInfos(nodeInfos, studyByUuid.get(nodeInfos.studyUuid()),
                        parentDirectoryNamesByStudyUuid, identityBySub))
                .filter(Objects::nonNull)
                .toList();
    }

    private ReferencingElementInfos toReferencingElementInfos(NodeInfos nodeInfos, ElementAttributes study,
                                                    Map<UUID, List<String>> parentDirectoryNamesByStudyUuid,
                                                    Map<String, UsersIdentities.UserIdentity> identityBySub) {
        if (study == null) {
            // the user cannot read this study, or it no longer exists
            return null;
        }
        return ReferencingElementInfos.builder()
                .elementName(study.getElementName())
                .type(study.getType())
                .path(parentDirectoryNamesByStudyUuid.getOrDefault(study.getElementUuid(), List.of()))
                .node(nodeInfos.nodeName())
                .ownerLabel(UsersIdentities.toLabel(study.getOwner(), identityBySub))
                .lastModificationDate(study.getLastModificationDate())
                .lastModifiedByLabel(UsersIdentities.toLabel(study.getLastModifiedBy(), identityBySub))
                .build();
    }

    private Map<UUID, List<String>> getParentDirectoryNames(Collection<UUID> elementUuids) {
        return directoryService.getElementsPaths(List.copyOf(elementUuids)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    // a path ends with the element itself
                    List<ElementAttributes> elementPath = entry.getValue();
                    return elementPath.stream()
                            .limit(Math.max(0, elementPath.size() - 1L))
                            .map(ElementAttributes::getElementName)
                            .toList();
                }));
    }

    private Map<String, UsersIdentities.UserIdentity> getIdentityBySub(Collection<ElementAttributes> elements) {
        List<String> subs = elements.stream()
                .flatMap(element -> Stream.of(element.getOwner(), element.getLastModifiedBy()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return userIdentityService.getUsersIdentitiesMap(subs);
    }

    public UUID createProcessConfig(String name, String processConfig, String description, UUID parentDirectoryUuid) {
        UUID processConfigUuid = monitorService.createProcessConfig(processConfig);
        ElementAttributes elementAttributes = new ElementAttributes(processConfigUuid, name, PROCESS_CONFIG, 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, monitorService::delete);
        return processConfigUuid;
    }

    public void updateProcessConfig(UUID uuid, String name, String processConfig, String description) {
        monitorService.updateProcessConfig(uuid, processConfig);
        updateElementNameAndDescription(uuid, name, description);
    }

    public UUID duplicateProcessConfig(UUID sourceProcessConfigUuid, UUID targetDirectoryId) {
        UUID newProcessConfigUuid = monitorService.duplicateProcessConfig(sourceProcessConfigUuid);
        duplicateDirectoryElementOrDeleteElement(sourceProcessConfigUuid, newProcessConfigUuid, targetDirectoryId, monitorService::delete);
        return newProcessConfigUuid;
    }

    public UUID createDynamicMapping(String name, String dynamicMapping, String description, UUID parentDirectoryUuid) {
        UUID dynamicMappingUuid = dynamicMappingService.createMapping(dynamicMapping);
        ElementAttributes elementAttributes = new ElementAttributes(dynamicMappingUuid, name, DYNAMIC_MAPPING, 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, dynamicMappingService::delete);
        return dynamicMappingUuid;
    }

    public void updateDynamicMapping(UUID uuid, String name, String dynamicMapping, String description) {
        dynamicMappingService.updateMapping(uuid, dynamicMapping);
        updateElementNameAndDescription(uuid, name, description);
    }

    public UUID duplicateDynamicMapping(UUID sourceDynamicMappingUuid, UUID targetDirectoryId) {
        UUID newDynamicMappingUuid = dynamicMappingService.duplicateMapping(sourceDynamicMappingUuid);
        duplicateDirectoryElementOrDeleteElement(sourceDynamicMappingUuid, newDynamicMappingUuid, targetDirectoryId, dynamicMappingService::delete);
        return newDynamicMappingUuid;
    }

    private void createDirectoryElementOrDeleteElement(ElementAttributes elementAttributes, UUID parentDirectoryUuid, Consumer<UUID> rollback) {
        executeWithRollback(() -> directoryService.createElement(elementAttributes, parentDirectoryUuid), elementAttributes.getElementUuid(), rollback);
    }

    private void createDirectoryElementWithNewNameOrDeleteElement(ElementAttributes elementAttributes, UUID parentDirectoryUuid, Consumer<UUID> rollback) {
        executeWithRollback(() -> directoryService.createElementWithNewName(elementAttributes, parentDirectoryUuid, true), elementAttributes.getElementUuid(), rollback);
    }

    private void executeWithRollback(Runnable directoryAction, UUID elementId, Consumer<UUID> rollback) {
        try {
            directoryAction.run();
        } catch (Exception directoryException) {
            try {
                rollback.accept(elementId);
            } catch (Exception rollbackException) {
                directoryException.addSuppressed(rollbackException);
            }
            throw directoryException;
        }
    }

    private void duplicateDirectoryElementOrDeleteElement(UUID elementToDuplicate, UUID elementDuplicated, UUID targetDirectoryId, Consumer<UUID> rollback) {
        duplicateDirectoryElementOrDeleteElement(elementToDuplicate, elementDuplicated, targetDirectoryId, DirectoryElementStatus.CREATED, rollback);
    }

    private void duplicateDirectoryElementOrDeleteElement(UUID elementToDuplicate, UUID elementDuplicated, UUID targetDirectoryId,
                                                          DirectoryElementStatus elementStatus, Consumer<UUID> rollback) {
        executeWithRollback(() -> directoryService.duplicateElement(elementToDuplicate, elementDuplicated, targetDirectoryId, elementStatus), elementDuplicated, rollback);
    }
}
