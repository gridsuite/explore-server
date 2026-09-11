/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.DirectoryElementStatus;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionDTO;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.services.ExploreService.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class DirectoryService implements IDirectoryElementsService {

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private static final String DIRECTORIES_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION;

    private static final String DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH = DIRECTORIES_SERVER_ROOT_PATH + DELIMITER
        + "directories";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER
        + "elements";

    private static final String ELEMENTS_SERVER_ELEMENT_PATH = ELEMENTS_SERVER_ROOT_PATH + DELIMITER
        + "{elementUuid}";

    private static final String PARAM_IDS = "ids";
    private static final String PARAM_ACCESS_TYPE = "accessType";
    private static final String PARAM_TARGET_DIRECTORY_UUID = "targetDirectoryUuid";
    private static final String PARAM_ELEMENT_TYPES = "elementTypes";
    private static final String PARAM_RECURSIVE = "recursive";
    private static final String PARAM_DIRECTORY_NAME = "directoryName";
    private static final String PARAM_RECURSIVE_CHECK = "recursiveCheck";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_DIRECTORY_UUID = "directoryUuid";
    private static final String PARAM_USER_INPUT = "userInput";
    private static final String PARAM_STATUS = "status";

    private final Map<String, IDirectoryElementsService> genericServices;
    private final RestTemplate restTemplate;
    private String directoryServerBaseUri;

    public DirectoryService(
        FilterService filterService, ContingencyListService contingencyListService, StudyService studyService, NetworkModificationService networkModificationService,
        CaseService caseService, SpreadsheetConfigService spreadsheetConfigService, SpreadsheetConfigCollectionService spreadsheetConfigCollectionService, ParametersService parametersService,
        SingleLineDiagramService singleLineDiagramService, WorkspaceService workspaceService, MonitorService monitorService, DynamicMappingService dynamicMappingService, RestTemplate restTemplate,
                RemoteServicesProperties remoteServicesProperties) {
        this.directoryServerBaseUri = remoteServicesProperties.getServiceUri("directory-server");
        this.restTemplate = restTemplate;
        this.genericServices = Map.ofEntries(
            Map.entry(FILTER, filterService),
            Map.entry(CONTINGENCY_LIST, contingencyListService),
            Map.entry(STUDY, studyService),
            Map.entry(DIRECTORY, this),
            Map.entry(MODIFICATION, networkModificationService),
            Map.entry(CASE, caseService),
            Map.entry(SPREADSHEET_CONFIG, spreadsheetConfigService),
            Map.entry(SPREADSHEET_CONFIG_COLLECTION, spreadsheetConfigCollectionService),
            Map.entry(DIAGRAM_CONFIG, singleLineDiagramService),
            Map.entry(WORKSPACE, workspaceService),
            Map.entry(PROCESS_CONFIG, monitorService),
            Map.entry(DYNAMIC_MAPPING, dynamicMappingService),
            Map.entry(ParametersType.VOLTAGE_INIT_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SECURITY_ANALYSIS_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.LOADFLOW_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SENSITIVITY_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SHORT_CIRCUIT_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.PCC_MIN_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.NETWORK_VISUALIZATIONS_PARAMETERS.name(), parametersService)
        );
    }

    public void setDirectoryServerBaseUri(String directoryServerBaseUri) {
        this.directoryServerBaseUri = directoryServerBaseUri;
    }

    public String getRootDirectories(List<String> types) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/root-directories")
            .queryParam(PARAM_ELEMENT_TYPES, types)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, String.class)
            .getBody();
    }

    public String createRootDirectory(String rootDirectoryAttributes) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/root-directories")
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(rootDirectoryAttributes), String.class)
            .getBody();
    }

    public String getDirectoryElements(UUID directoryUuid, List<String> types, boolean recursive) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/elements")
            .queryParam(PARAM_ELEMENT_TYPES, types)
            .queryParam(PARAM_RECURSIVE, recursive)
            .buildAndExpand(directoryUuid)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, String.class)
            .getBody();
    }

    public String getPath(UUID elementUuid) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/elements/{elementUuid}/path")
            .buildAndExpand(elementUuid)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, String.class)
            .getBody();
    }

    /**
     * @return the path of each element, indexed by element uuid. Each path is ordered from the root directory to the
     * element itself, and unknown elements are absent from the result.
     */
    public Map<UUID, List<ElementAttributes>> getElementsPaths(List<UUID> elementUuids) {
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH + "/paths")
            .queryParam(PARAM_IDS, elementUuids)
            .buildAndExpand()
            .toUriString();

        Map<UUID, List<ElementAttributes>> elementsPaths = restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<UUID, List<ElementAttributes>>>() { })
            .getBody();
        return Objects.requireNonNullElse(elementsPaths, Collections.emptyMap());
    }

    public HttpStatusCode elementExists(UUID directoryUuid, String elementName, String type) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/elements/{elementName}/types/{type}")
            .buildAndExpand(directoryUuid, elementName, type)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class)
            .getStatusCode();
    }

    public HttpStatusCode rootDirectoryExists(String directoryName) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/root-directories")
            .queryParam(PARAM_DIRECTORY_NAME, directoryName)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class)
            .getStatusCode();
    }

    public String getNameCandidate(UUID directoryUuid, String elementName, String type) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/{elementName}/newNameCandidate")
            .queryParam(PARAM_TYPE, type)
            .buildAndExpand(directoryUuid, elementName)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, String.class)
            .getBody();
    }

    public String searchElements(String userInput, String directoryUuid) {
        URI uri = UriComponentsBuilder
            .fromUriString(directoryServerBaseUri + DIRECTORIES_SERVER_ROOT_PATH + "/elements/indexation-infos")
            .queryParam(PARAM_DIRECTORY_UUID, "{directoryUuid}")
            .queryParam(PARAM_USER_INPUT, "{userInput}")
            .build(directoryUuid, userInput);

        return restTemplate
            .exchange(uri, HttpMethod.GET, HttpEntity.EMPTY, String.class)
            .getBody();
    }

    public ElementAttributes createElement(ElementAttributes elementAttributes, UUID directoryUuid) {
        return createElementWithNewName(elementAttributes, directoryUuid, false);
    }

    public ElementAttributes createElementWithNewName(ElementAttributes elementAttributes, UUID directoryUuid, boolean allowNewName) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/elements?allowNewName={allowNewName}")
            .buildAndExpand(directoryUuid, allowNewName)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(elementAttributes), ElementAttributes.class)
            .getBody();
    }

    public ElementAttributes duplicateElement(UUID elementUuid, UUID newElementUuid, UUID targetDirectoryId, DirectoryElementStatus newElementStatus) {
        UriComponentsBuilder uri = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
            .queryParam("newElementUuid", newElementUuid)
            .queryParam("newElementStatus", newElementStatus);
        if (targetDirectoryId != null) {
            uri.queryParam("targetDirectoryId", targetDirectoryId);
        }
        String path = uri.buildAndExpand(elementUuid)
            .toUriString();

        return restTemplate
            .exchange(directoryServerBaseUri + path, HttpMethod.POST, HttpEntity.EMPTY, ElementAttributes.class)
            .getBody();
    }

    public void deleteDirectoryElement(UUID elementUuid) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
            .buildAndExpand(elementUuid)
            .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    public void deleteElementsFromDirectory(List<UUID> elementUuids, UUID parentDirectoryUuid) {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH)
            .queryParam(PARAM_IDS, ids)
            .queryParam("parentDirectoryUuid", parentDirectoryUuid)
            .buildAndExpand()
            .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    public ElementAttributes getElementInfos(UUID elementUuid) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
            .buildAndExpand(elementUuid)
            .toUriString();
        return Objects.requireNonNull(restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, ElementAttributes.class).getBody());
    }

    public List<ElementAttributes> getElementsInfos(List<UUID> elementsUuids, List<String> elementTypes) {
        return getElementsInfos(elementsUuids, elementTypes, true);
    }

    /**
     * @param strictMode when false, elements the user cannot read or that no longer exist are absent from the result
     *                   instead of failing the whole call
     */
    public List<ElementAttributes> getElementsInfos(List<UUID> elementsUuids, List<String> elementTypes, boolean strictMode) {
        var ids = elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH).toUriString() + "?ids=" + ids;

        if (!strictMode) {
            path += "&strictMode=false";
        }

        if (!CollectionUtils.isEmpty(elementTypes)) {
            path += "&elementTypes=" + elementTypes.stream().collect(Collectors.joining(","));
        }

        List<ElementAttributes> elementAttributesList;
        elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<List<ElementAttributes>>() { }).getBody();
        return Objects.requireNonNullElse(elementAttributesList, Collections.emptyList());
    }

    // TODO: est ce que je laisse userId en argument ici ?
    public int getUserCasesCount(String userId) {
        String path = UriComponentsBuilder
            .fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "users/{userId}/cases/count")
            .buildAndExpand(userId)
            .toUriString();

        return Objects.requireNonNull(restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, Integer.class).getBody());
    }

    // TODO: method not used -> to delete ?
    public void notifyDirectoryChanged(UUID elementUuid) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ELEMENT_PATH + "/notification?type={update_directory}")
            .buildAndExpand(elementUuid, NotificationType.UPDATE_DIRECTORY.name())
            .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
    }

    private List<ElementAttributes> getDirectoryElements(UUID directoryUuid) {
        String path = UriComponentsBuilder.fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/elements")
            .buildAndExpand(directoryUuid)
            .toUriString();
        List<ElementAttributes> elementAttributesList;
        elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<ElementAttributes>>() { }).getBody();

        return Objects.requireNonNullElse(elementAttributesList, Collections.emptyList());
    }

    public void deleteElement(UUID id) {
        ElementAttributes elementAttribute = getElementInfos(id);
        IDirectoryElementsService service = getGenericService(elementAttribute.getType());
        service.delete(elementAttribute.getElementUuid());
    }

    private IDirectoryElementsService getGenericService(String type) {
        IDirectoryElementsService iDirectoryElementsService = genericServices.get(type);
        if (iDirectoryElementsService == null) {
            throw new IllegalArgumentException("Unknown element type " + type);
        }
        return iDirectoryElementsService;
    }

    public List<ElementAttributes> getElementsMetadata(List<UUID> ids, List<String> elementTypes,
                                                       List<String> equipmentTypes) {
        Map<String, List<ElementAttributes>> elementAttributesListByType = getElementsInfos(ids, elementTypes)
            .stream()
            .collect(Collectors.groupingBy(ElementAttributes::getType));
        List<ElementAttributes> listOfElements = new ArrayList<>();
        for (Map.Entry<String, List<ElementAttributes>> elementAttribute : elementAttributesListByType.entrySet()) {
            IDirectoryElementsService service = getGenericService(elementAttribute.getKey());
            listOfElements.addAll(service.completeElementAttribute(elementAttribute.getValue()));
        }

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

    public Map<UUID, String> getElementsName(List<UUID> ids) {
        String path = UriComponentsBuilder
            .fromPath("/v1/elements/names")
            .queryParam(PARAM_IDS, ids)
            .queryParam("strictMode", false)
            .buildAndExpand()
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ElementAttributes> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<Map<UUID, String>>() {
        }).getBody();
    }

    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
            .buildAndExpand(elementUuid)
            .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.PUT, new HttpEntity<>(elementAttributes), Void.class);
    }

    // TODO get id/type recursively then do batch delete
    @Override
    public void delete(UUID id) {
        List<ElementAttributes> elementAttributesList = getDirectoryElements(id);
        elementAttributesList.forEach(elementAttributes -> deleteElement(elementAttributes.getElementUuid()));
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid) {
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH)
            .queryParam(PARAM_TARGET_DIRECTORY_UUID, targetDirectoryUuid)
            .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.PUT, new HttpEntity<>(elementsUuids), Void.class);
    }

    public void checkPermission(List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType) {
        checkPermission(elementUuids, targetDirectoryUuid, permissionType, false);
    }

    public void checkPermission(List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType, boolean recursiveCheck) {
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH + "/authorized")
                .queryParam(PARAM_ACCESS_TYPE, permissionType)
                .queryParam(PARAM_IDS, elementUuids)
                .queryParam(PARAM_TARGET_DIRECTORY_UUID, targetDirectoryUuid)
                .queryParam(PARAM_RECURSIVE_CHECK, recursiveCheck)
                .buildAndExpand()
                .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, HttpEntity.EMPTY, Void.class);
    }

    public List<PermissionDTO> getDirectoryPermissions(UUID directoryUuid) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/permissions")
            .buildAndExpand(directoryUuid)
            .toUriString();

        ResponseEntity<List<PermissionDTO>> response = restTemplate.exchange(
            directoryServerBaseUri + path,
            HttpMethod.GET,
            HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() { }
        );

        return response.getBody();
    }

    public void setDirectoryPermissions(UUID directoryUuid, List<PermissionDTO> permissions) {
        String path = UriComponentsBuilder
            .fromPath(DIRECTORIES_SERVER_DIRECTORIES_ROOT_PATH + "/{directoryUuid}/permissions")
            .buildAndExpand(directoryUuid)
            .toUriString();

        restTemplate.exchange(
            directoryServerBaseUri + path,
            HttpMethod.PUT,
            new HttpEntity<>(permissions),
            Void.class
        );
    }

    public void updateElementsStatus(List<UUID> elementUuids, DirectoryElementStatus status) {
        if (elementUuids == null || elementUuids.isEmpty()) {
            return;
        }
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                    .fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam(PARAM_IDS, ids)
                .queryParam(PARAM_STATUS, status)
                .buildAndExpand()
                .toUriString();

        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.PUT, HttpEntity.EMPTY, Void.class);
    }
}
