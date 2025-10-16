package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.DirectoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class DirectoryServiceTest {

    @MockitoBean
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<String> responseEntity;

    @Autowired
    private DirectoryService directoryService;

    @Test
    void testSearchElementsWithSpecialCharacters() {
        String userInput = "a+éè{}\\`b";
        String directoryUuid = UUID.randomUUID().toString();
        String userId = "testUser";
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        directoryService.searchElements(userInput, directoryUuid, userId);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), any(), any(), any(Class.class));
        String uriString = uriCaptor.getValue().toString();
        assertTrue(uriString.contains("userInput=a%2B%C3%A9%C3%A8%7B%7D%5C%60b"));
    }
}
