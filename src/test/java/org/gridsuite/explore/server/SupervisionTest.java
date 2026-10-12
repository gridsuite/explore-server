package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.RemoteServicesProperties;
import org.gridsuite.explore.server.services.SupervisionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * @author Kevin Le Saulnier <kevin.lesaulnier at rte-france.com>
 */
@SpringBootTest
class SupervisionTest {
    private SupervisionService supervisionService;
    private DirectoryService directoryService;
    private RestClient.Builder restClientBuilder;
    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec<?> deleteSpec;
    private RestClient.RequestHeadersSpec<?> requestSpec;
    private RestClient.ResponseSpec responseSpec;
    private RemoteServicesProperties remoteServicesProperties;

    private final ElementAttributes filter = new ElementAttributes(UUID.randomUUID(), "filter", "FILTER", "userId", 0L, null);
    private final ElementAttributes study = new ElementAttributes(UUID.randomUUID(), "study", "STUDY", "userId", 0L, null);

    @BeforeEach
    void setUp() {
        directoryService = mock(DirectoryService.class);
        restClientBuilder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        remoteServicesProperties = mock(RemoteServicesProperties.class);

        when(remoteServicesProperties.getServiceUri("directory-server")).thenReturn("http://directory-server");
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        doReturn(deleteSpec).when(restClient).delete();

        doReturn(requestSpec).when(deleteSpec).uri(anyString());
        when(requestSpec.retrieve()).thenReturn(responseSpec);

        supervisionService = new SupervisionService(directoryService, restClientBuilder, remoteServicesProperties);
    }

    @Test
    void testDeleteElements() {
        List<UUID> uuidsToDelete = List.of(filter.getElementUuid(), study.getElementUuid());
        supervisionService.deleteElements(uuidsToDelete);
        // deletions of both elements with foreach towards respective microservice
        verify(directoryService, times(1)).deleteElement(filter.getElementUuid());
        verify(directoryService, times(1)).deleteElement(study.getElementUuid());
        // deletions of both elements in directory server
        verify(restClient, times(1)).delete();
    }

    @Test
    void testDeleteElementsWithErrors() {
        List<UUID> uuidsToDelete = List.of(filter.getElementUuid(), study.getElementUuid());

        // one deletion will fail, this test checks deletions does not stop even when one of them is throwing       an exception
        doThrow(new RuntimeException("An error occured when deleting filter")).when(directoryService).deleteElement(filter.getElementUuid());

        supervisionService.deleteElements(uuidsToDelete);
        // deletions of both elements with foreach towards respective microservice
        verify(directoryService, times(1)).deleteElement(filter.getElementUuid());
        verify(directoryService, times(1)).deleteElement(study.getElementUuid());
        // deletions of both elements in directory server
        verify(restClient, times(1)).delete();
    }

    @AfterEach
    void verifyNoMoreInteractionsMocks() {
        verifyNoMoreInteractions(restClient);
        verifyNoMoreInteractions(directoryService);
    }
}
