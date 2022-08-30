/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.ExploreException;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    private static final String DIRECTORIES_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "directories";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER + "elements";

    private final Map<String, IDirectoryElementsService> genericServices;
    private String directoryServerBaseUri;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public DirectoryService(@Value("${backing-services.directory-server.base-uri:http://directory-server/}") String directoryServerBaseUri,
                            FilterService filterService, ContingencyListService contingencyListService, StudyService studyService, CaseService caseService) {
        this.directoryServerBaseUri = directoryServerBaseUri;
        this.genericServices = Map.of(
                FILTER, filterService,
                CONTINGENCY_LIST, contingencyListService,
                STUDY, studyService,
                DIRECTORY, this,
                CASE, caseService);
    }

    public void setDirectoryServerBaseUri(String directoryServerBaseUri) {
        this.directoryServerBaseUri = directoryServerBaseUri;
    }

    public ElementAttributes createElement(ElementAttributes elementAttributes, UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements")
                .buildAndExpand(directoryUuid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ElementAttributes> httpEntity = new HttpEntity<>(elementAttributes, headers);
        try {
            return restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.POST, httpEntity, ElementAttributes.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ExploreException(CREATE_ELEMENT_FAILED);
        }

    }

    public void deleteDirectoryElement(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{elementUuid}")
                .buildAndExpand(elementUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public ElementAttributes getElementInfos(UUID elementUuid) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{directoryUuid}")
                .buildAndExpand(elementUuid)
                .toUriString();
        return restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null, ElementAttributes.class).getBody();

    }

    private List<ElementAttributes> getElementsInfos(List<UUID> elementsUuids) {
        var ids = elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH).toUriString() + "?ids=" + ids;
        List<ElementAttributes> elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null, new ParameterizedTypeReference<List<ElementAttributes>>() {
        }).getBody();
        if (elementAttributesList != null) {
            return elementAttributesList;
        } else {
            return Collections.EMPTY_LIST;
        }

    }

    public void notifyDirectoryChanged(UUID elementUuid, String userId) {
        String path = UriComponentsBuilder
                .fromPath(ELEMENTS_SERVER_ROOT_PATH + "/{elementUuid}/notification?type={update_directory}")
                .buildAndExpand(elementUuid, NotificationType.UPDATE_DIRECTORY.name())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        try {
            restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        } catch (HttpStatusCodeException e) {
            throw new ExploreException(NOTIFICATION_DIRECTORY_CHANGED);
        }
    }

    private List<ElementAttributes> getDirectoryElements(UUID directoryUuid, String userId) {
        String path = UriComponentsBuilder.fromPath(DIRECTORIES_SERVER_ROOT_PATH + "/{directoryUuid}/elements")
                .buildAndExpand(directoryUuid)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        List<ElementAttributes> elementAttributesList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<List<ElementAttributes>>() {
        }).getBody();
        if (elementAttributesList != null) {
            return elementAttributesList;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public void deleteElement(UUID id, String userId) {
        ElementAttributes elementAttribute = getElementInfos(id);
        delete(elementAttribute.getElementUuid(), userId);
    }

    private IDirectoryElementsService getGenericService(String type) {
        IDirectoryElementsService iDirectoryElementsService = genericServices.get(type);
        if (iDirectoryElementsService == null) {
            throw new ExploreException(UNKNOWN_ELEMENT_TYPE, "Unknown element type " + type);
        }
        return iDirectoryElementsService;
    }

    public List<ElementAttributes> getElementsMetadata(List<UUID> ids) {

        Map<String, List<ElementAttributes>> elementAttributesListByType = getElementsInfos(ids).stream()
                .collect(Collectors.groupingBy(ElementAttributes::getType));

        List<List<ElementAttributes>> listOfListElements = new ArrayList<>();
        for (Map.Entry<String, List<ElementAttributes>> elementAttribute : elementAttributesListByType.entrySet()) {
            IDirectoryElementsService service = getGenericService(elementAttribute.getKey());
            listOfListElements.add(service.completeElementAttribute(elementAttribute.getValue()));
        }
        return listOfListElements.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    // TODO get id/type recursively then do batch delete
    @Override
    public void delete(UUID id, String userId) {
        List<ElementAttributes> elementAttributesList = getDirectoryElements(id, userId);
        if (elementAttributesList != null) {
            elementAttributesList.forEach(elementAttributes ->
                deleteElement(elementAttributes.getElementUuid(), userId)
            );
        }
    }

}
