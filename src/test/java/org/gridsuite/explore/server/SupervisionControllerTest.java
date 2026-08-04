package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.SupervisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SupervisionControllerTest {

    private static final List<UUID> IDS = List.of(UUID.randomUUID(), UUID.randomUUID());

    @Mock
    private SupervisionService supervisionService;

    @InjectMocks
    private SupervisionController controller;

    @Test
    void deleteElementsForwardsIdsAndUserId() {
        assertEquals(HttpStatus.OK, controller.deleteElements(IDS, "userId").getStatusCode());

        verify(supervisionService).deleteElements(IDS, "userId");
    }
}
