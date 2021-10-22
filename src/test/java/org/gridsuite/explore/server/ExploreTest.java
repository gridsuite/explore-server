/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import okhttp3.mockwebserver.MockWebServer;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "20000")
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
public class ExploreTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ExploreService exploreService;

    private MockWebServer server;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreTest.class);

    private static final String TEST_FILE = "testCase.xiidm";

//    @Before
//    public void setup() throws IOException {
//        server = new MockWebServer();
//
//        // Start the server.
//        server.start();
//
//        // Ask the server for its URL. You'll need this to make HTTP requests.
//        HttpUrl baseHttpUrl = server.url("");
//        String baseUrl = baseHttpUrl.toString().substring(0, baseHttpUrl.toString().length() - 1);
//
//    }
//
//    @After
//    public void tearDown() {
//        cleanDB();
//        assertNull("Should not be any messages", output.receive(1000));
//    }

    @Test
    public void test() throws Exception {

    }
}
