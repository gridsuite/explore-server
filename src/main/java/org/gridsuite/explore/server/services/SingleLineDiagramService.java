package org.gridsuite.explore.server.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

@Service
public class SingleLineDiagramService implements IDirectoryElementsService {

    private static final String SINGLE_LINE_DIAGRAM_API_VERSION = "v1";
    private static final String DELIMITER = "/";
    private static final String SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH = DELIMITER + SINGLE_LINE_DIAGRAM_API_VERSION + DELIMITER + "network-area-diagram/config";

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    String singleLineDiagramServerBaseUri;

    public SingleLineDiagramService(RemoteServicesProperties remoteServicesProperties, RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
        this.singleLineDiagramServerBaseUri = remoteServicesProperties.getServiceUri("single-line-diagram-server");
        this.restClient = restClientBuilder.baseUrl(singleLineDiagramServerBaseUri).build();
    }

    public void setSingleLineDiagramServerBaseUri(String singleLineDiagramServerBaseUri) {
        this.singleLineDiagramServerBaseUri = singleLineDiagramServerBaseUri;
        this.restClient = restClientBuilder.baseUrl(singleLineDiagramServerBaseUri).build();
    }

    @Override
    public void delete(UUID configUuid) {
        Objects.requireNonNull(configUuid);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
            .buildAndExpand()
            .toUriString();

        restClient.delete()
            .uri(path)
            .retrieve()
            .toBodilessEntity();
    }

    public UUID createDiagramConfig(String diagramConfig) {
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH)
            .buildAndExpand()
            .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(diagramConfig)
            .retrieve()
            .body(UUID.class);
    }

    public UUID duplicateDiagramConfig(UUID sourceUuid) {
        Objects.requireNonNull(sourceUuid);

        String path = UriComponentsBuilder.fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + "{uuid}" + DELIMITER + "duplicate")
            .buildAndExpand(sourceUuid)
            .toUriString();

        return restClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(UUID.class);
    }

    public void updateDiagramConfig(UUID configUuid, String diagramConfig) {
        Objects.requireNonNull(configUuid);
        Objects.requireNonNull(diagramConfig);

        var path = UriComponentsBuilder
            .fromPath(SINGLE_LINE_DIAGRAM_CONFIG_ROOT_PATH + DELIMITER + configUuid)
            .buildAndExpand()
            .toUriString();

        restClient.put()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(diagramConfig)
            .retrieve()
            .toBodilessEntity();
    }
}
