/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class DirectoryServiceTest {

    @MockitoBean
    private RestClient restClient;

    @Autowired
    private DirectoryService directoryService;

    @Test
    void testSearchElementsWithSpecialCharacters() {
        String userInput = "a+éè{}\\`b";
        String directoryUuid = UUID.randomUUID().toString();
        RestClient.RequestHeadersUriSpec<?> requestUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> requestSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(requestUriSpec).when(restClient).get();
        doReturn(requestSpec).when(requestUriSpec).uri(any(URI.class));
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("");

        directoryService.searchElements(userInput, directoryUuid);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestUriSpec).uri(uriCaptor.capture());
        assertTrue(uriCaptor.getValue().toString().contains("userInput=a%2B%C3%A9%C3%A8%7B%7D%5C%60b"));
    }
}
