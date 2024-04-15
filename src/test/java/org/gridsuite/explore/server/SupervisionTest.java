package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.SupervisionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;


import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * @author Kevin Le Saulnier <kevin.lesaulnier at rte-france.com>
 */
@SpringBootTest
public class SupervisionTest {
    @Autowired
    SupervisionService supervisionService;

    @MockBean
    DirectoryService directoryService;

    @MockBean
    RestTemplate restTemplate;


    ElementAttributes filter = new ElementAttributes(UUID.randomUUID(), "filter", "FILTER", null, "userId", 0L, null, null);

    ElementAttributes study = new ElementAttributes(UUID.randomUUID(), "study", "STUDY", null, "userId", 0L, null, null);

    @Test
    void testDeleteElements() {
        List<UUID> uuidsToDelete = List.of(filter.getElementUuid(), study.getElementUuid());
        supervisionService.deleteElements(uuidsToDelete, "userId");
        // deletions of both elements with foreach towards respective microservice
        verify(directoryService, times(1)).deleteElement(filter.getElementUuid(), "userId");
        verify(directoryService, times(1)).deleteElement(study.getElementUuid(), "userId");
        // deletions of both elements in directory server
        verify(restTemplate, times(1)).exchange(matches(".*/supervision/.*"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void testDeleteElementsWithErrors() {
        List<UUID> uuidsToDelete = List.of(filter.getElementUuid(), study.getElementUuid());

        // one deletion will fail, this test checks deletions does not stop even when one of them is throwing an exception
        doThrow(new RuntimeException("An error occured when deleting filter")).when(directoryService).deleteElement(filter.getElementUuid(), "userId");

        supervisionService.deleteElements(uuidsToDelete, "userId");
        // deletions of both elements with foreach towards respective microservice
        verify(directoryService, times(1)).deleteElement(filter.getElementUuid(), "userId");
        verify(directoryService, times(1)).deleteElement(study.getElementUuid(), "userId");
        // deletions of both elements in directory server
        verify(restTemplate, times(1)).exchange(matches(".*/supervision/.*"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @AfterEach
    public void verifyNoMoreInteractionsMocks() {
        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(directoryService);
    }
}
