/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.powsybl.ws.commons.security.PermissionType;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.explore.server.dto.*;
import org.gridsuite.explore.server.error.ExploreException;
import org.gridsuite.explore.server.services.cases.CaseRestService;
import org.gridsuite.explore.server.services.contingency.ContingencyListRestService;
import org.gridsuite.explore.server.services.diagram.SingleLineDiagramRestService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.gridsuite.explore.server.services.dynamic.DynamicMappingRestService;
import org.gridsuite.explore.server.services.filter.FilterRestService;
import org.gridsuite.explore.server.services.modification.NetworkModificationRestService;
import org.gridsuite.explore.server.services.monitor.MonitorRestService;
import org.gridsuite.explore.server.services.parameters.ParametersRestService;
import org.gridsuite.explore.server.services.spreadsheet.SpreadsheetConfigCollectionRestService;
import org.gridsuite.explore.server.services.spreadsheet.SpreadsheetConfigRestService;
import org.gridsuite.explore.server.services.study.StudyRestService;
import org.gridsuite.explore.server.services.workspace.WorkspaceRestService;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gridsuite.explore.server.dto.DirectoryElementStatus.CREATING;
import static org.gridsuite.explore.server.dto.DirectoryElementType.*;
import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.ELEMENT_ALREADY_EXISTS;
import static org.gridsuite.explore.server.error.ExploreBusinessErrorCode.EXPLORE_MAX_ELEMENTS_EXCEEDED;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Etienne Homer <jacques.borsenberger at rte-france.com>
 */

@Service
public class ExploreService {
    private final DirectoryService directoryService;
    private final StudyRestService studyRestService;
    private final ContingencyListRestService contingencyListRestService;
    private final NetworkModificationRestService networkModificationRestService;
    private final FilterRestService filterRestService;
    private final CaseRestService caseRestService;
    private final ParametersRestService parametersRestService;
    private final SpreadsheetConfigRestService spreadsheetConfigRestService;
    private final SpreadsheetConfigCollectionRestService spreadsheetConfigCollectionRestService;
    private final WorkspaceRestService workspaceRestService;
    private final UserIdentityService userIdentityService;
    private final NotificationService notificationService;
    private final MonitorRestService monitorRestService;
    private final DynamicMappingRestService dynamicMappingRestService;
    private final ExploreServerExecutionService exploreServerExecutionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreService.class);
    private final UserAdminService userAdminService;
    private final SingleLineDiagramRestService singleLineDiagramRestService;

    private final Map<DirectoryElementType, IDirectoryElementsService> genericServices;

    public ExploreService(
            List<IDirectoryElementsService> directoryElementsServices,
            DirectoryService directoryService,
            StudyRestService studyRestService,
            ContingencyListRestService contingencyListRestService,
            NetworkModificationRestService networkModificationRestService,
            FilterRestService filterRestService,
            CaseRestService caseRestService,
            ParametersRestService parametersRestService,
            SpreadsheetConfigRestService spreadsheetConfigRestService,
            UserAdminService userAdminService,
            SpreadsheetConfigCollectionRestService spreadsheetConfigCollectionRestService,
            WorkspaceRestService workspaceRestService,
            DynamicMappingRestService dynamicMappingRestService,
            UserIdentityService userIdentityService,
            NotificationService notificationService,
            MonitorRestService monitorRestService,
            ExploreServerExecutionService exploreServerExecutionService,
            SingleLineDiagramRestService singleLineDiagramRestService) {
        this.directoryService = directoryService;
        this.studyRestService = studyRestService;
        this.contingencyListRestService = contingencyListRestService;
        this.networkModificationRestService = networkModificationRestService;
        this.filterRestService = filterRestService;
        this.caseRestService = caseRestService;
        this.parametersRestService = parametersRestService;
        this.spreadsheetConfigRestService = spreadsheetConfigRestService;
        this.userAdminService = userAdminService;
        this.spreadsheetConfigCollectionRestService = spreadsheetConfigCollectionRestService;
        this.workspaceRestService = workspaceRestService;
        this.dynamicMappingRestService = dynamicMappingRestService;
        this.userIdentityService = userIdentityService;
        this.notificationService = notificationService;
        this.monitorRestService = monitorRestService;
        this.exploreServerExecutionService = exploreServerExecutionService;
        this.singleLineDiagramRestService = singleLineDiagramRestService;
        this.genericServices = buildGenericServicesRegistry(directoryElementsServices);
    }

    /**
     * Maps each element type to the service declaring it through {@link DirectoryElementTypes}.
     * Fails at startup if a service is not annotated, if two services declare the same type
     * or if a {@link DirectoryElementType} is handled by no service.
     */
    static Map<DirectoryElementType, IDirectoryElementsService> buildGenericServicesRegistry(List<IDirectoryElementsService> services) {
        Map<DirectoryElementType, IDirectoryElementsService> registry = new EnumMap<>(DirectoryElementType.class);
        for (IDirectoryElementsService service : services) {
            for (DirectoryElementType elementType : getDeclaredElementTypes(service)) {
                IDirectoryElementsService previous = registry.putIfAbsent(elementType, service);
                if (previous != null) {
                    throw new IllegalStateException("Element type " + elementType + " is declared by both "
                            + AopUtils.getTargetClass(previous).getName() + " and " + AopUtils.getTargetClass(service).getName());
                }
            }
        }
        EnumSet<DirectoryElementType> unhandledTypes = EnumSet.complementOf(EnumSet.copyOf(registry.keySet()));
        if (!unhandledTypes.isEmpty()) {
            throw new IllegalStateException("No " + IDirectoryElementsService.class.getSimpleName() + " handles element types " + unhandledTypes);
        }
        return Collections.unmodifiableMap(registry);
    }

    private static DirectoryElementType[] getDeclaredElementTypes(IDirectoryElementsService service) {
        // AopUtils + findMergedAnnotation : resolves the annotation through Spring proxies and Mockito subclass mocks
        Class<?> serviceClass = AopUtils.getTargetClass(service);
        DirectoryElementTypes declaration = AnnotatedElementUtils.findMergedAnnotation(serviceClass, DirectoryElementTypes.class);
        if (declaration == null) {
            throw new IllegalStateException(serviceClass.getName() + " must be annotated with @" + DirectoryElementTypes.class.getSimpleName());
        }
        return declaration.value();
    }

    public IDirectoryElementsService getGenericService(DirectoryElementType type) {
        return genericServices.get(type);
    }

    public IDirectoryElementsService getGenericService(String type) {
        DirectoryElementType elementType = Arrays.stream(DirectoryElementType.values())
                .filter(t -> t.name().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown element type " + type));
        return getGenericService(elementType);
    }

    /**
     * Deletes an element and all sub-elements if @param id refers to a directory.
     */
    public void delete(UUID id) {
        ElementAttributes element = directoryService.getElementInfos(id);
        if (DIRECTORY.name().equals(element.getType())) {
            directoryService.getDirectoryElements(id, null, true).stream()
                    .filter(e -> !DIRECTORY.name().equals(e.getType()))
                    .collect(Collectors.groupingBy(ElementAttributes::getType))
                    .forEach((type, elems) -> elems.forEach(e -> getGenericService(type).delete(e.getElementUuid())));
        } else {
            getGenericService(element.getType()).delete(id);
        }
    }

    public UUID createStudy(String studyName, CaseInfo caseInfo, String description, UUID parentDirectoryUuid, Map<String, Object> importParams, Boolean duplicateCase) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), studyName, STUDY.name(), 0L, description, CREATING);

        String elementName = getElementName(caseInfo.caseUuid());

        studyRestService.insertStudyWithExistingCaseFile(elementAttributes.getElementUuid(), caseInfo.caseUuid(), caseInfo.caseFormat(), importParams, duplicateCase, elementName);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(STUDY)::delete);
        return elementAttributes.getElementUuid();
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

    public List<ElementAttributes> getElementsMetadata(List<UUID> ids, List<String> elementTypes, List<String> equipmentTypes) {
        List<ElementAttributes> elements = directoryService.getElementsInfos(ids, elementTypes);
        List<ElementAttributes> listOfElements = completeElementsAttributes(elements);

        if (!CollectionUtils.isEmpty(equipmentTypes) && !listOfElements.isEmpty()) {
            listOfElements = listOfElements.stream()
                    .filter(element -> {
                        Object equipmentType = element.getSpecificMetadata().get("equipmentType");
                        if (equipmentType != null) { // could be null for some elements
                            return equipmentTypes.contains(equipmentType);
                        }
                        return true; // keep other elements
                    })
                    .collect(Collectors.toList());
        }
        return listOfElements;
    }

    public boolean doesElementNameAlreadyExists(String elementName, UUID parentDirectoryUuid, String elementType, UUID excludedElementUuid) {
        List<ElementAttributes> elements = getOverridableDirectoryElements(parentDirectoryUuid, List.of(elementType), false);
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream()
                .filter(element -> !element.getElementUuid().equals(excludedElementUuid))
                .anyMatch(element -> elementName.equals(element.getElementName()));
    }

    public UUID duplicateStudy(UUID sourceStudyUuid, UUID targetDirectoryId) {
        UUID newStudyId = studyRestService.duplicateStudy(sourceStudyUuid);
        duplicateDirectoryElementOrDeleteElement(sourceStudyUuid, newStudyId, targetDirectoryId, CREATING, getGenericService(STUDY)::delete);
        return newStudyId;
    }

    public UUID createCase(String caseName, MultipartFile caseFile, String description, UUID parentDirectoryUuid) {
        UUID uuid = caseRestService.importMultipartCase(caseFile);
        ElementAttributes elementAttributes = new ElementAttributes(uuid, caseName, CASE.name(), 0L, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(CASE)::delete);
        return uuid;
    }

    public void persistCase(String caseName, UUID caseUuid, String description, UUID parentDirectoryUuid) {
        caseRestService.persistCase(caseUuid);
        ElementAttributes elementAttributes = new ElementAttributes(caseUuid, caseName, CASE.name(), 0L, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(CASE)::delete);
    }

    public UUID duplicateCase(UUID sourceCaseUuid, UUID targetDirectoryId) {
        UUID newCaseId = caseRestService.duplicateCase(sourceCaseUuid);
        duplicateDirectoryElementOrDeleteElement(sourceCaseUuid, newCaseId, targetDirectoryId, getGenericService(CASE)::delete);
        return newCaseId;
    }

    public UUID duplicateExistingCase(UUID sourceCaseUuid, String caseName, String description, UUID targetDirectoryId) {
        UUID newCaseUuid = caseRestService.duplicateCase(sourceCaseUuid);
        ElementAttributes elementAttributes = new ElementAttributes(newCaseUuid, caseName, CASE.name(), 0L, description);
        createDirectoryElementOrDeleteElement(elementAttributes, targetDirectoryId, getGenericService(CASE)::delete);
        return newCaseUuid;
    }

    public UUID duplicateContingencyList(UUID contingencyListsId, UUID targetDirectoryId, ContingencyListType contingencyListType) {
        UUID newId = switch (contingencyListType) {
            case IDENTIFIERS -> contingencyListRestService.duplicateIdentifierContingencyList(contingencyListsId);
            case FILTERS -> contingencyListRestService.duplicateFilterBasedContingencyList(contingencyListsId);
        };
        duplicateDirectoryElementOrDeleteElement(contingencyListsId, newId, targetDirectoryId, getGenericService(CONTINGENCY_LIST)::delete);
        return newId;
    }

    public UUID createIdentifierContingencyList(String listName, String content, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST.name(), 0L, description);
        contingencyListRestService.insertIdentifierContingencyList(elementAttributes.getElementUuid(), content);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(CONTINGENCY_LIST)::delete);
        return elementAttributes.getElementUuid();
    }

    public UUID createFilterBasedContingencyList(String listName, String content, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), listName, CONTINGENCY_LIST.name(), 0L, description);
        contingencyListRestService.insertFilterBasedContingencyList(elementAttributes.getElementUuid(), content);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(CONTINGENCY_LIST)::delete);
        return elementAttributes.getElementUuid();
    }

    public UUID createFilter(String filter, String filterName, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(UUID.randomUUID(), filterName, FILTER.name(), 0, description);
        filterRestService.insertFilter(filter, elementAttributes.getElementUuid());
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(FILTER)::delete);
        return elementAttributes.getElementUuid();
    }

    public UUID duplicateFilter(UUID sourceFilterId, UUID targetDirectoryId) {
        UUID newFilterId = filterRestService.duplicateFilter(sourceFilterId);
        duplicateDirectoryElementOrDeleteElement(sourceFilterId, newFilterId, targetDirectoryId, getGenericService(FILTER)::delete);
        return newFilterId;
    }

    public CompletableFuture<Void> deleteElement(UUID id) {
        return exploreServerExecutionService.runAsync(() -> doDeleteElement(id));
    }

    private void doDeleteElement(UUID id) {
        try {
            directoryService.updateElementsStatus(List.of(id), DirectoryElementStatus.DELETING);
            // FIXME dirty fix to ignore errors and still delete the elements in the directory-server. To delete when handled properly.
            delete(id);
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
                delete(id);
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
        filterRestService.updateFilter(id, filter);

        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes);
    }

    public void updateContingencyList(UUID id, String content, String name, String description, ContingencyListType contingencyListType) {
        contingencyListRestService.updateContingencyList(id, content, getProperPath(contingencyListType));
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setDescription(description);
        if (StringUtils.isNotBlank(name)) {
            elementAttributes.setElementName(name);
        }
        directoryService.updateElement(id, elementAttributes);
    }

    public void replaceCompositeModification(UUID id, List<UUID> modificationUuids, String name, String description) {
        networkModificationRestService.replaceCompositeModification(id, name, description, modificationUuids);
    }

    public void updateCompositeModificationActivation(UUID compositeModificationId, List<UUID> childrenModificationUuids, boolean active) {
        networkModificationRestService.updateCompositeModificationActivation(childrenModificationUuids, active);
        notificationService.emitElementUpdated(compositeModificationId);
    }

    public void updateCompositeModificationApplicability(UUID compositeModificationId, List<UUID> childrenModificationUuids, String rootNetworkTag, boolean applicable) {
        networkModificationRestService.updateRootNetworkApplicability(childrenModificationUuids, rootNetworkTag, applicable);
        notificationService.emitElementUpdated(compositeModificationId);
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        return networkModificationRestService.getCompositeModificationContent(compositeModificationId);
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

    public UUID createParameters(String parameters, ParametersType parametersType, String parametersName, String description, UUID parentDirectoryUuid) {
        UUID parametersUuid = parametersRestService.createParameters(parameters, parametersType);
        ElementAttributes elementAttributes = new ElementAttributes(parametersUuid, parametersName, parametersType.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(parametersType.name())::delete);
        return parametersUuid;
    }

    public void updateParameters(UUID id, String parameters, ParametersType parametersType, String name, String description) {
        parametersRestService.updateParameters(id, parameters, parametersType);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public UUID duplicateParameters(UUID sourceId, UUID targetDirectoryId, ParametersType parametersType) {
        UUID newParametersUuid = parametersRestService.duplicateParameters(sourceId, parametersType);
        duplicateDirectoryElementOrDeleteElement(sourceId, newParametersUuid, targetDirectoryId, getGenericService(parametersType.name())::delete);
        return newParametersUuid;
    }

    public UUID createDiagramConfig(String diagramConfig, String diagramConfigName, String description, UUID parentDirectoryUuid) {
        UUID diagramConfigUuid = singleLineDiagramRestService.createDiagramConfig(diagramConfig);
        ElementAttributes elementAttributes = new ElementAttributes(diagramConfigUuid, diagramConfigName, DIAGRAM_CONFIG.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(DIAGRAM_CONFIG)::delete);
        return diagramConfigUuid;
    }

    public UUID duplicateDiagramConfig(UUID sourceId, UUID targetDirectoryId) {
        UUID newConfigUuid = singleLineDiagramRestService.duplicateDiagramConfig(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newConfigUuid, targetDirectoryId, getGenericService(DIAGRAM_CONFIG)::delete);
        return newConfigUuid;
    }

    public void updateDiagramConfig(UUID id, String diagramConfig, String name, String description) {
        singleLineDiagramRestService.updateDiagramConfig(id, diagramConfig);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public UUID createSpreadsheetConfig(String spreadsheetConfigDto, String configName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigRestService.createSpreadsheetConfig(spreadsheetConfigDto);
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, configName, SPREADSHEET_CONFIG.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(SPREADSHEET_CONFIG)::delete);
        return spreadsheetConfigUuid;
    }

    public UUID createSpreadsheetConfigCollection(String spreadsheetConfigCollectionDto, String collectionName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionRestService.createSpreadsheetConfigCollection(spreadsheetConfigCollectionDto);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid);
        return spreadsheetConfigUuid;
    }

    public UUID createSpreadsheetConfigCollectionFromConfigIds(List<UUID> configIds, String collectionName, String description, UUID parentDirectoryUuid) {
        UUID spreadsheetConfigUuid = spreadsheetConfigCollectionRestService.createSpreadsheetConfigCollectionFromConfigIds(configIds);
        createSpreadsheetConfigCollectionElement(spreadsheetConfigUuid, collectionName, description, parentDirectoryUuid);
        return spreadsheetConfigUuid;
    }

    private void createSpreadsheetConfigCollectionElement(UUID spreadsheetConfigUuid, String collectionName, String description, UUID parentDirectoryUuid) {
        ElementAttributes elementAttributes = new ElementAttributes(spreadsheetConfigUuid, collectionName, SPREADSHEET_CONFIG_COLLECTION.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(SPREADSHEET_CONFIG_COLLECTION)::delete);
    }

    public void updateSpreadsheetConfig(UUID id, String spreadsheetConfigDto, String name, String description) {
        spreadsheetConfigRestService.updateSpreadsheetConfig(id, spreadsheetConfigDto);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public void updateSpreadsheetConfigCollection(UUID id, String spreadsheetConfigCollectionDto, String name, String description) {
        spreadsheetConfigCollectionRestService.updateSpreadsheetConfigCollection(id, spreadsheetConfigCollectionDto);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public void replaceAllSpreadsheetConfigsInCollection(UUID id, List<UUID> configIds, String name, String description) {
        spreadsheetConfigCollectionRestService.replaceAllSpreadsheetConfigsInCollection(id, configIds);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public UUID duplicateSpreadsheetConfig(UUID sourceId, UUID targetDirectoryId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigRestService.duplicateSpreadsheetConfig(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, getGenericService(SPREADSHEET_CONFIG)::delete);
        return newSpreadsheetConfigUuid;
    }

    public UUID duplicateSpreadsheetConfigCollection(UUID sourceId, UUID targetDirectoryId) {
        UUID newSpreadsheetConfigUuid = spreadsheetConfigCollectionRestService.duplicateSpreadsheetConfigCollection(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newSpreadsheetConfigUuid, targetDirectoryId, getGenericService(SPREADSHEET_CONFIG_COLLECTION)::delete);
        return newSpreadsheetConfigUuid;
    }

    public UUID createWorkspace(UUID workspaceId, String workspaceName, String description, UUID parentDirectoryUuid) {
        UUID newWorkspaceId = workspaceRestService.duplicateWorkspace(workspaceId);
        ElementAttributes elementAttributes = new ElementAttributes(newWorkspaceId, workspaceName, WORKSPACE.name(), 0, description);
        createDirectoryElementOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(WORKSPACE)::delete);
        return newWorkspaceId;
    }

    public void replaceWorkspace(UUID id, UUID workspaceId, String name, String description) {
        workspaceRestService.replaceWorkspace(id, workspaceId);
        updateElementNameAndDescription(id, name, description);
        notificationService.emitElementUpdated(id);
    }

    public UUID duplicateWorkspace(UUID sourceId, UUID targetDirectoryId) {
        UUID newWorkspaceId = workspaceRestService.duplicateWorkspace(sourceId);
        duplicateDirectoryElementOrDeleteElement(sourceId, newWorkspaceId, targetDirectoryId, getGenericService(WORKSPACE)::delete);
        return newWorkspaceId;
    }

    public UUID createCompositeModification(List<UUID> modificationUuids, String name,
                                            String description, UUID parentDirectoryUuid, List<String> rootNetworkTags) {
        UUID modificationsUuid = networkModificationRestService.createCompositeModification(modificationUuids, name, rootNetworkTags);
        // element names should always be null for the modifications because their names are stored in network modification server
        ElementAttributes elementAttributes = new ElementAttributes(modificationsUuid, null, MODIFICATION.name(), 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(MODIFICATION)::delete);
        return modificationsUuid;
    }

    public UUID duplicateCompositeModification(UUID sourceId, UUID parentDirectoryUuid) {
        // create duplicated modification
        Map<UUID, UUID> newModificationsUuids = networkModificationRestService.duplicateCompositeModifications(List.of(sourceId));
        UUID newNetworkModification = newModificationsUuids.get(sourceId);
        // create corresponding directory element
        duplicateDirectoryElementOrDeleteElement(sourceId, newNetworkModification, parentDirectoryUuid, getGenericService(MODIFICATION)::delete);
        return newNetworkModification;
    }

    public void assertCanCreateCase() {
        Integer userMaxAllowedStudiesAndCases = userAdminService.getUserMaxAllowedCases();
        if (userMaxAllowedStudiesAndCases != null) {
            int userCasesCount = directoryService.getUserCasesCount();
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

    public void updateElement(UUID id, ElementAttributes updatedElementAttributes, String elementType) {
        String finalElementType = elementType == null ? directoryService.getElementInfos(id).getType() : elementType;
        getGenericService(finalElementType).updateElement(id, updatedElementAttributes, directoryService);
    }

    private void notifyElementMoved(ElementAttributes element) {
        // send notification if the study name was updated
        if (STUDY.name().equals(element.getType())) {
            studyRestService.notifyStudyUpdate(element.getElementUuid());
        }
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid) {
        directoryService.moveElementsDirectory(elementsUuids, targetDirectoryUuid);
        List<ElementAttributes> elementsAttributes = directoryService.getElementsInfos(elementsUuids, null);
        elementsAttributes.forEach(this::notifyElementMoved);

    }

    public Map<UUID, String> getElementsName(List<UUID> ids, String type) {
        if (type != null) {
            return getGenericService(type).getNames(ids, directoryService);
        } else {
            // if the user didn't specify a type, then the types of each elements have to be determined
            Map<String, List<UUID>> idsByType = directoryService.getElementsInfos(ids, null).stream()
                    .collect(Collectors.groupingBy(
                            ElementAttributes::getType,
                            Collectors.mapping(ElementAttributes::getElementUuid, Collectors.toList())));
            Map<UUID, String> names = new HashMap<>();
            idsByType.forEach((elementType, elementIds) ->
                    names.putAll(getGenericService(elementType).getNames(elementIds, directoryService)));
            return names;
        }
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
        List<UUID> referencedNodeUuids = getReferencedNodeUuids(elementUuid);
        if (referencedNodeUuids.isEmpty()) {
            return List.of();
        }

        // a node can be referenced several times, and several nodes often belong to the same study: query each only once
        Map<UUID, NodeInfos> nodeInfosByUuid = studyRestService.getNodesInfos(referencedNodeUuids.stream().distinct().toList())
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

    /**
     * For each root network tag, counts the studies using a shared composite: the studies of the nodes referencing it
     */
    public Map<String, Long> countStudiesByTagFromSharedComposite(UUID compositeUuid) {
        List<UUID> referencedNodeUuids = getReferencedNodeUuids(compositeUuid).stream().distinct().toList();
        if (referencedNodeUuids.isEmpty()) {
            return Map.of();
        }
        List<UUID> studyUuids = studyRestService.getNodesInfos(referencedNodeUuids).stream()
                .map(NodeInfos::studyUuid)
                .distinct()
                .toList();
        if (studyUuids.isEmpty()) {
            return Map.of();
        }
        // a study counts once for each of its tags
        return studyRestService.getRootNetworkTags(studyUuids).values().stream()
                .flatMap(tags -> tags.stream().distinct())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * @return the nodes referencing the shared element, once per reference
     */
    private List<UUID> getReferencedNodeUuids(UUID elementUuid) {
        // for now only references resolving to a study node: STUDY_NODE and STUDY_NODE_NETWORK_MODIFICATION
        return directoryService.getElementInfos(elementUuid).getReferences().stream()
                .map(ExploreService::getReferencedNodeUuid)
                .filter(Objects::nonNull)
                .toList();
    }

    private static UUID getReferencedNodeUuid(ReferenceAttributes reference) {
        return switch (reference.getReferenceType()) {
            case STUDY_NODE -> reference.getReferenceContainer().getContainerId();
            case STUDY_NODE_NETWORK_MODIFICATION -> reference.getReferenceContainer().getRootContainerId();
            case DIRECTORY_NETWORK_MODIFICATION -> null;
        };
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
        UUID processConfigUuid = monitorRestService.createProcessConfig(processConfig);
        ElementAttributes elementAttributes = new ElementAttributes(processConfigUuid, name, PROCESS_CONFIG.name(), 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(PROCESS_CONFIG)::delete);
        return processConfigUuid;
    }

    public String getProcessConfig(UUID uuid) {
        return monitorRestService.getProcessConfig(uuid);
    }

    public void updateProcessConfig(UUID uuid, String name, String processConfig, String description) {
        monitorRestService.updateProcessConfig(uuid, processConfig);
        updateElementNameAndDescription(uuid, name, description);
        notificationService.emitElementUpdated(uuid);
    }

    public UUID duplicateProcessConfig(UUID sourceProcessConfigUuid, UUID targetDirectoryId) {
        UUID newProcessConfigUuid = monitorRestService.duplicateProcessConfig(sourceProcessConfigUuid);
        duplicateDirectoryElementOrDeleteElement(sourceProcessConfigUuid, newProcessConfigUuid, targetDirectoryId, getGenericService(PROCESS_CONFIG)::delete);
        return newProcessConfigUuid;
    }

    public UUID createDynamicMapping(String name, String dynamicMapping, String description, UUID parentDirectoryUuid) {
        UUID dynamicMappingUuid = dynamicMappingRestService.createMapping(dynamicMapping);
        ElementAttributes elementAttributes = new ElementAttributes(dynamicMappingUuid, name, DYNAMIC_MAPPING.name(), 0L, description);
        createDirectoryElementWithNewNameOrDeleteElement(elementAttributes, parentDirectoryUuid, getGenericService(DYNAMIC_MAPPING)::delete);
        return dynamicMappingUuid;
    }

    public void updateDynamicMapping(UUID uuid, String name, String dynamicMapping, String description) {
        dynamicMappingRestService.updateMapping(uuid, dynamicMapping);
        updateElementNameAndDescription(uuid, name, description);
        notificationService.emitElementUpdated(uuid);
    }

    public UUID duplicateDynamicMapping(UUID sourceDynamicMappingUuid, UUID targetDirectoryId) {
        UUID newDynamicMappingUuid = dynamicMappingRestService.duplicateMapping(sourceDynamicMappingUuid);
        duplicateDirectoryElementOrDeleteElement(sourceDynamicMappingUuid, newDynamicMappingUuid, targetDirectoryId, getGenericService(DYNAMIC_MAPPING)::delete);
        return newDynamicMappingUuid;
    }

    void createDirectoryElementOrDeleteElement(ElementAttributes elementAttributes, UUID parentDirectoryUuid, Consumer<UUID> rollback) {
        executeWithRollback(() -> directoryService.createElement(elementAttributes, parentDirectoryUuid), elementAttributes.getElementUuid(), rollback);
    }

    void createDirectoryElementWithNewNameOrDeleteElement(ElementAttributes elementAttributes, UUID parentDirectoryUuid, Consumer<UUID> rollback) {
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

    /**
     * returns the directory elements from directory service but possibly overriden with some specific data hold by the objects that override basic elements data
     */
    public List<ElementAttributes> getOverridableDirectoryElements(UUID directoryUuid, List<String> types, Boolean recursive) {
        return directoryService.getDirectoryElements(directoryUuid, types, recursive)
                .stream()
                .collect(Collectors.groupingBy(ElementAttributes::getType))
                .entrySet().stream()
                .map(entry -> getGenericService(entry.getKey()).overrideBasicDirectoryElements(entry.getValue()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void shareNetworkModification(UUID studyUuid,
                                         UUID nodeUuid,
                                         UUID modificationUuid,
                                         String name,
                                         String description,
                                         UUID parentDirectoryUuid) {
        directoryService.checkPermission(List.of(), parentDirectoryUuid, PermissionType.WRITE, false);
        if (doesElementNameAlreadyExists(name, parentDirectoryUuid, MODIFICATION.name(), modificationUuid)) {
            throw new ExploreException(ELEMENT_ALREADY_EXISTS,
                    "A composite modification named '" + name + "' already exists in directory", Map.of("elementName", name));
        }
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        studyRestService.shareModification(studyUuid, nodeUuid, modificationUuid, name, description, parentDirectoryUuid, userId);
    }

    public List<ElementAttributes> completeElementsAttributes(List<ElementAttributes> elements) {
        return elements.stream()
                .collect(Collectors.groupingBy(ElementAttributes::getType))
                .entrySet().stream()
                .flatMap(entry -> completeElementsAttributes(entry.getKey(), entry.getValue()).stream())
                .collect(Collectors.toList());
    }

    private List<ElementAttributes> completeElementsAttributes(String elementType, List<ElementAttributes> elements) {
        // each service decides how its metadata enrich the element (default: IDirectoryElementsService::setSpecificMetadata)
        IDirectoryElementsService specificService = getGenericService(elementType);

        Map<String, ElementAttributes> baseMetadataById = elements.stream()
                .collect(Collectors.toMap(e -> e.getElementUuid().toString(), Function.identity()));
        try {
            List<UUID> elementsUuids = elements.stream().map(ElementAttributes::getElementUuid).toList();
            return specificService.getMetadata(elementsUuids).stream()
                    .map(specificServiceMetadataItem -> specificService.addSpecificMetadata(
                            baseMetadataById.get(getMetadataItemId(specificServiceMetadataItem)),
                            specificServiceMetadataItem)
                    )
                    .collect(Collectors.toList());
        } catch (ResourceAccessException _) {
            LOGGER.warn("{} metadata service is unavailable, returning elements with empty specific metadata", elementType);
            return elements.stream()
                    .map(element -> specificService.addSpecificMetadata(element, Map.of()))
                    .collect(Collectors.toList());
        }
    }

    public static String getMetadataItemId(Map<String, Object> metadataItem) {
        Object id = metadataItem.get("id");
        if (id == null) {
            id = metadataItem.getOrDefault("uuid", "");
        }
        return id.toString();
    }
}
