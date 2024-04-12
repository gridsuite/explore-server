package org.gridsuite.explore.server.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupervisionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionService.class);
    private DirectoryService directoryService;

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private static final String SUPERVISION_PATH = "/supervision";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER +  SUPERVISION_PATH + DELIMITER
        + "elements";

    private String directoryServerBaseUri;

    private final RestTemplate restTemplate;

    public SupervisionService (DirectoryService directoryService, RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.directoryServerBaseUri = remoteServicesProperties.getServiceUri("directory-server");
        this.directoryService = directoryService;
        this.restTemplate = restTemplate;
    }

    public void deleteElements(List<UUID> uuids, String userId) {
        uuids.forEach(id -> {
            try {
                directoryService.deleteElement(id, userId);
            } catch (Exception e) {
                // if deletion fails (element does not exist, server is down...), the process keeps proceeding to at least delete references in directory-server
                // orphan elements will be deleted in a dedicated script
                LOGGER.error(e.toString(), e);
            }
        });
        deleteElements(uuids);
    }



    // DOES NOT CHECK OWNER BEFORE DELETING
    public void deleteElements(List<UUID> elementUuids) {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        String path = UriComponentsBuilder
            .fromPath(ELEMENTS_SERVER_ROOT_PATH)
            .queryParam("ids", ids)
            .buildAndExpand()
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

}
