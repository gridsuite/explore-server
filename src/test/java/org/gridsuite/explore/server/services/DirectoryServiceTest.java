/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class DirectoryServiceTest {
    private static final String DIRECTORY_SERVER_BASE_URI = "http://directory-server";

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private RestClient.Builder restClientBuilder;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        /*
         * DirectoryService creates its RestClient from RestClient.Builder.
         * Rebuild it after MockRestServiceServer has been bound to the builder.
         */
        directoryService.setDirectoryServerBaseUri(DIRECTORY_SERVER_BASE_URI);
    }

    @Test
    void testSearchElementsWithSpecialCharacters() {
        String userInput = "a+éè{}\\`b";
        String directoryUuid = UUID.randomUUID().toString();

        mockServer.expect(requestTo(
            DIRECTORY_SERVER_BASE_URI
                + "/v1/elements/indexation-infos"
                + "?directoryUuid=" + directoryUuid
                + "&userInput=a%2B%C3%A9%C3%A8%7B%7D%5C%60b"))
            .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

        directoryService.searchElements(userInput, directoryUuid);

        mockServer.verify();
    }
}
