/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.utils.ParametersType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.*;
import static org.gridsuite.explore.server.services.ExploreService.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class DirectoryService implements IDirectoryElementsService {

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private static final String DIRECTORIES_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER
            + "directories";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER
            + "elements";

    private static final String ELEMENTS_SERVER_ELEMENT_PATH = ELEMENTS_SERVER_ROOT_PATH + DELIMITER
            + "{elementUuid}";

    private static final String PARAM_IDS = "ids";
    private static final String PARAM_FOR_DELETION = "forDeletion";
    private static final String PARAM_TARGET_DIRECTORY_UUID = "targetDirectoryUuid";

    private final Map<String, IDirectoryElementsService> genericServices;
    private final RestTemplate restTemplate;
    private String directoryServerBaseUri;

    public DirectoryService(
            FilterService filterService, ContingencyListService contingencyListService, StudyService studyService, NetworkModificationService networkModificationService,
            CaseService caseService, SpreadsheetConfigService spreadsheetConfigService, ParametersService parametersService, RestTemplate restTemplate,
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
            Map.entry(ParametersType.VOLTAGE_INIT_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SECURITY_ANALYSIS_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.LOADFLOW_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SENSITIVITY_PARAMETERS.name(), parametersService),
            Map.entry(ParametersType.SHORT_CIRCUIT_PARAMETERS.name(), parametersService)
        );
    }

    public void setDirectoryServerBaseUri(String directoryServerBaseUri) {
        this.directoryServerBaseUri = directoryServerBaseUri;
    }

    public ElementAttributes createElement(ElementAttributes elementAttributes, UUID directoryUuid, String userId) {
        return createElementWithNewName(elementAttributes, directoryUuid, userId, false);
    }

    public ElementAttributes createElementWithNewName(ElementAttributes elementAttributes, UUID directoryUuid, String userId, boolean allowNewName) {
        String path = UriComponentsBuilder
                .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements?allowNewName={allowNewName}")
                .buildAndExpand(directoryUuid, allowNewName)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ElementAttributes> httpEntity = new HttpEntity<>(elementAttributes, headers);
        return restTemplate
                .exchange(directoryServerBaseUri + path, HttpMethod.POST, httpEntity, ElementAttributes.class)
                .getBody();
    }

    public ElementAttributes duplicateElement(UUID elementUuid, UUID newElementUuid, UUID targetDirectoryId, String userId) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam("duplicateFrom", elementUuid)
                .queryParam("newElementUuid", newElementUuid);
        if (targetDirectoryId != null) {
            uri.queryParam("targetDirectoryId", targetDirectoryId);
        }
        String path = uri.buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate
                .exchange(directoryServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), ElementAttributes.class)
                .getBody();
    }

    public void deleteDirectoryElement(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
                .buildAndExpand(elementUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public void areDirectoryElementsDeletable(List<UUID> elementUuids, String userId) {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam(PARAM_FOR_DELETION, true)
                .queryParam(PARAM_IDS, ids)
                .buildAndExpand()
                .toUriString();

        ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.HEAD, new HttpEntity<>(headers), Void.class);

        } catch (HttpStatusCodeException e) {
            if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
                throw new ExploreException(NOT_ALLOWED);
            } else if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new ExploreException(NOT_FOUND);
            } else {
                throw e;
            }
        }

        if (HttpStatus.NO_CONTENT.equals(response.getStatusCode())) {
            throw new ExploreException(NOT_ALLOWED);
        }
    }

    public void deleteElementsFromDirectory(List<UUID> elementUuids, UUID parentDirectoryUuid, String userId) {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam(PARAM_IDS, ids)
                .queryParam("parentDirectoryUuid", parentDirectoryUuid)
                .buildAndExpand()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public ElementAttributes getElementInfos(UUID elementUuid) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
                .buildAndExpand(elementUuid)
                .toUriString();
        return restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null, ElementAttributes.class)
                .getBody();
    }

    public List<ElementAttributes> getElementsInfos(List<UUID> elementsUuids, List<String> elementTypes) {
        var ids = elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH).toUriString() + "?ids=" + ids;

        if (!CollectionUtils.isEmpty(elementTypes)) {
            path += "&elementTypes=" + elementTypes.stream().collect(Collectors.joining(","));
        }

        List<ElementAttributes> elementAttributesList;
        elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ElementAttributes>>() {
                }).getBody();
        return Objects.requireNonNullElse(elementAttributesList, Collections.emptyList());
    }

    public int getUserCasesCount(String userId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "users/{userId}/cases/count")
                .buildAndExpand(userId)
                .toUriString();

        Integer casesCount = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null, Integer.class).getBody();
        if (casesCount == null) {
            throw new ExploreException(REMOTE_ERROR, "Could not get cases count");
        }
        return casesCount;
    }

    public void notifyDirectoryChanged(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ELEMENT_PATH + "/notification?type={update_directory}")
                .buildAndExpand(elementUuid, NotificationType.UPDATE_DIRECTORY.name())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
    }

    private List<ElementAttributes> getDirectoryElements(UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements")
                .buildAndExpand(directoryUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        List<ElementAttributes> elementAttributesList;
        elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET,
                new HttpEntity<>(headers), new ParameterizedTypeReference<List<ElementAttributes>>() {
                }).getBody();

        return Objects.requireNonNullElse(elementAttributesList, Collections.emptyList());
    }

    public void deleteElement(UUID id, String userId) {
        ElementAttributes elementAttribute = getElementInfos(id);
        IDirectoryElementsService service = getGenericService(elementAttribute.getType());
        service.delete(elementAttribute.getElementUuid(), userId);
    }

    private IDirectoryElementsService getGenericService(String type) {
        IDirectoryElementsService iDirectoryElementsService = genericServices.get(type);
        if (iDirectoryElementsService == null) {
            throw new ExploreException(UNKNOWN_ELEMENT_TYPE, "Unknown element type " + type);
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
                    .filter(element -> "DIRECTORY".equals(element.getType())
                            || equipmentTypes.contains(element.getSpecificMetadata().get("equipmentType")))
                    .collect(Collectors.toList());
        }

        return listOfElements;
    }

    public void updateElement(UUID elementUuid, ElementAttributes elementAttributes, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ELEMENT_PATH)
                .buildAndExpand(elementUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ElementAttributes> httpEntity = new HttpEntity<>(elementAttributes, headers);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }

    // TODO get id/type recursively then do batch delete
    @Override
    public void delete(UUID id, String userId) {
        List<ElementAttributes> elementAttributesList = getDirectoryElements(id, userId);
        elementAttributesList.forEach(elementAttributes -> deleteElement(elementAttributes.getElementUuid(), userId));
    }

    public void moveElementsDirectory(List<UUID> elementsUuids, UUID targetDirectoryUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam(PARAM_TARGET_DIRECTORY_UUID, targetDirectoryUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<UUID>> httpEntity = new HttpEntity<>(elementsUuids, headers);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }
}
