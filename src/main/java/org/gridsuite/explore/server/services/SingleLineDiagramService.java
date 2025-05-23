package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

@Service
public class SingleLineDiagramService implements IDirectoryElementsService {

    private static final String SINGLE_LINE_DIAGRAM_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH = DELIMITER + SINGLE_LINE_DIAGRAM_API_VERSION + DELIMITER + "network-area-diagram/config";
    private static final String DUPLICATE_FROM_PARAMETER = "duplicateFrom";

    @Setter
    String singleLineDiagramServerBaseUri;

    private final RestTemplate restTemplate;

    public SingleLineDiagramService(RemoteServicesProperties remoteServicesProperties, RestTemplate restTemplate) {
        this.singleLineDiagramServerBaseUri = remoteServicesProperties.getServiceUri("single-line-diagram-server");
        this.restTemplate = restTemplate;
    }

    @Override
    public void delete(UUID configUuid, String userId) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
            .buildAndExpand()
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);

        restTemplate.exchange(singleLineDiagramServerBaseUri + path, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public UUID createDiagramConfig(String diagramConfig) {
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH)
            .buildAndExpand()
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(diagramConfig, headers);

        return restTemplate.exchange(singleLineDiagramServerBaseUri + path, HttpMethod.POST, httpEntity, UUID.class).getBody();
    }

    public UUID duplicateDiagramConfig(UUID sourceUuid) {
        Objects.requireNonNull(sourceUuid);

        String path = UriComponentsBuilder.fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH)
            .queryParam(DUPLICATE_FROM_PARAMETER, sourceUuid)
            .buildAndExpand()
            .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(singleLineDiagramServerBaseUri + path, HttpMethod.POST, new HttpEntity<>(headers), UUID.class)
            .getBody();
    }

    public void updateDiagramConfig(UUID configUuid, String diagramConfig) {
        Objects.requireNonNull(configUuid);
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
            .buildAndExpand()
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(diagramConfig, headers);

        restTemplate.exchange(singleLineDiagramServerBaseUri + path, HttpMethod.PUT, httpEntity, Void.class);
    }
}
