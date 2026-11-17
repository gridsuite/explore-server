/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreConstants.HEADER_USER_ID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class NetworkModificationService implements IDirectoryElementsService {
    private static final String NETWORK_MODIFICATION_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    public static final String UUIDS = "uuids";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String ROOT_NETWORK_TAGS = "rootNetworkTags";
    public static final String NETWORK_COMPOSITE_MODIFICATIONS_PATH = "network-composite-modifications";
    private static final String NETWORK_MODIFICATIONS_PATH = "network-modifications";
    private static final String CONTAINERS_PATH = "containers";
    @Setter
    private String networkModificationServerBaseUri;
    private final RestClient restClient;
    private final NotificationService notificationService;

    public NetworkModificationService(RestClient restClient, RemoteServicesProperties remoteServicesProperties, NotificationService notificationService) {
        this.restClient = restClient;
        this.networkModificationServerBaseUri = remoteServicesProperties.getServiceUri("network-modification-server");
        this.notificationService = notificationService;
    }

    public Map<UUID, UUID> duplicateCompositeModifications(List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + "duplication")
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public UUID createCompositeModification(List<UUID> modificationUuids, String compositeModificationName, List<String> rootNetworkTags) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH)
                .queryParam(NAME, compositeModificationName)
                .queryParam(ROOT_NETWORK_TAGS, rootNetworkTags)
                .buildAndExpand()
                .toUriString();

        return restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .body(UUID.class);
    }

    public void createModification(UUID groupUuid, JsonNode modification) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .queryParam("groupUuid", groupUuid)
                .buildAndExpand()
                .toUriString();

        Map<String, Object> body = Map.of("first", modification, "second", List.of());

        restClient.post()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    }

    public void replaceCompositeModification(UUID compositeModificationId, String newName, String description, List<UUID> modificationUuids) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_COMPOSITE_MODIFICATIONS_PATH + DELIMITER + compositeModificationId + "/replace")
                .queryParam(NAME, newName)
                .queryParam(DESCRIPTION, description) // TODO : handle this in network modification server
                .buildAndExpand()
                .toUriString();

        restClient.put()
            .uri(networkModificationServerBaseUri + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationUuids)
            .retrieve()
            .toBodilessEntity();
    }

    /**
     * @param newName null if the name shouldn't be updated
     * @param newDescription null if the description shouldn't be updated
     */
    public void updateModificationNameAndDescription(UUID compositeModificationId, String newName, String newDescription) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(
                DELIMITER + NETWORK_MODIFICATION_API_VERSION +
                        DELIMITER + NETWORK_MODIFICATIONS_PATH +
                        DELIMITER + compositeModificationId +
                        DELIMITER + "name-and-description"
                );
        String path = uriComponentsBuilder.buildAndExpand().toUriString();
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> modificationMetadata = new HashMap<>();
        modificationMetadata.put("id", compositeModificationId);
        if (newName != null) {
            modificationMetadata.put(NAME, newName);
        }
        if (newDescription != null) {
            modificationMetadata.put(DESCRIPTION, newDescription);
        }

        restClient.put()
            .uri(networkModificationServerBaseUri + path)
            .header(HEADER_USER_ID, userId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(modificationMetadata)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void delete(UUID id) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH)
                .queryParam(UUIDS, List.of(id))
                .buildAndExpand()
                .toUriString();

        restClient.delete()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public List<Map<String, Object>> getMetadata(List<UUID> modificationUuids) {
        var ids = modificationUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
                .fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + DELIMITER + NETWORK_MODIFICATIONS_PATH + "/metadata" + "?ids=" + ids)
                .buildAndExpand()
                .toUriString();

        return restClient.get()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public List<Object> getCompositeModificationContent(UUID compositeModificationId) {
        String path = UriComponentsBuilder
                .fromPath(DELIMITER +
                        NETWORK_MODIFICATION_API_VERSION +
                        DELIMITER +
                        CONTAINERS_PATH +
                        DELIMITER +
                        "{containerUuid}" +
                        DELIMITER +
                        NETWORK_MODIFICATIONS_PATH)
                .queryParam("onlyMetadata", true)
                .buildAndExpand(compositeModificationId)
                .toUriString();
        return restClient.get()
            .uri(networkModificationServerBaseUri + path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    @Override
    public void updateElement(UUID modificationToNotifyId, ElementAttributes elementAttributes, DirectoryService directoryService) {
        // modificationToNotifyId can be the id of the shared parent modification
        // elementAttributes.getElementUuid() is the real id of the network modification to update
        updateModificationNameAndDescription(
                elementAttributes.getElementUuid(), elementAttributes.getElementName(), elementAttributes.getDescription());
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.emitElementMetadataUpdated(modificationToNotifyId, userId);
    }

    @Override
    public ElementAttributes populateMetadataItem(ElementAttributes elementAttributes, Map<String, Object> specificServiceMetadata) {
        if (specificServiceMetadata != null) {
            if (specificServiceMetadata.get(NAME) != null) {
                elementAttributes.setElementName(specificServiceMetadata.get(NAME).toString());
            }
            if (specificServiceMetadata.get(DESCRIPTION) != null) {
                elementAttributes.setDescription(specificServiceMetadata.get(DESCRIPTION).toString());
            }
        }
        return IDirectoryElementsService.setSpecificMetadata(elementAttributes, specificServiceMetadata);
    }

    @Override
    public List<ElementAttributes> overrideBasicDirectoryElements(List<ElementAttributes> elementsAttributes) {
        List<UUID> modificationUuids = elementsAttributes.stream().map(ElementAttributes::getElementUuid).toList();
        List<Map<String, Object>> specificServiceMetadataList = getMetadata(modificationUuids);

        modificationUuids.forEach(modificationUuid -> {
            Map<String, Object> specificServiceMetadata = specificServiceMetadataList.stream()
                    .filter(metadata ->
                            metadata.get("id").equals(modificationUuid.toString()))
                    .findFirst().orElse(null);
            ElementAttributes elementAttributes = elementsAttributes.stream()
                    .filter(attributes -> attributes.getElementUuid().equals(modificationUuid))
                    .findFirst().orElse(null);
            if (specificServiceMetadata != null && elementAttributes != null) {
                if (specificServiceMetadata.get(NAME) != null) {
                    elementAttributes.setElementName(specificServiceMetadata.get(NAME).toString());
                }
                if (specificServiceMetadata.get(DESCRIPTION) != null) {
                    elementAttributes.setDescription(specificServiceMetadata.get(DESCRIPTION).toString());
                }
            }
        });

        return elementsAttributes;
    }
}
