/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.config.EnableWebFlux;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private DirectoryService directoryService;

    @Autowired
    private ContingencyListService contingencyListService;

    @Autowired
    private FilterService filterService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private ObjectMapper mapper;

    private MockWebServer server;

    private static final String TEST_FILE = "testCase.xiidm";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();
    private static final String STUDY_ERROR_NAME = "studyInError";
    private static final String STUDY1 = "study1";
    private static final String USER1 = "user1";

    @Before
    public void setup() throws IOException {
        server = new MockWebServer();

        // Start the server.
        server.start();

        // Ask the server for its URL. You'll need this to make HTTP requests.
        HttpUrl baseHttpUrl = server.url("");
        String baseUrl = baseHttpUrl.toString().substring(0, baseHttpUrl.toString().length() - 1);

        directoryService.setDirectyServerBaseUri(baseUrl);
        studyService.setStudyServerBaseUri(baseUrl);
        filterService.setFilterServerBaseUri(baseUrl);
        contingencyListService.setActionsServerBaseUri(baseUrl);

        String elementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(ELEMENT_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0));
        String filterContingencyListAttributesAsString = mapper.writeValueAsString(new ElementAttributes(ELEMENT_UUID, "filterContingencyList", "FILTERS_CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0));
        String filterAttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID, "filterContingencyList", "FILTER", new AccessRightsAttributes(true), USER1, 0));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String path = Objects.requireNonNull(request.getPath());

                if (path.matches("/v1/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(404);
                } else if (path.matches("/v1/studies/" + STUDY_ERROR_NAME + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(409);
                } else if (path.matches("/v1/studies/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1//studies/.*/private") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(String.valueOf(elementAttributesAsString)).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/directories/" + ELEMENT_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(String.valueOf(filterContingencyListAttributesAsString)).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/directories/" + FILTER_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(String.valueOf(filterAttributesAsString)).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/directories/" + ELEMENT_UUID) && "DELETE".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/script-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters-contingency-lists/.*/new-script/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/new-script/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters\\?id=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/replace-with-script") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/directories/.*/updateType/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/directories/.*/rights") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                }
                return  new MockResponse().setResponseCode(500);
            }
        };
        server.setDispatcher(dispatcher);
    }

    @Test
    public void testCreateStudyFromExistingCase() {
        webTestClient.post()
                .uri("/v1/directories/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&isPrivate=true&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateStudyFromExistingCaseError() {
        webTestClient.post()
                .uri("/v1/directories/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&isPrivate=true&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testCreateStudy() throws IOException {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_FILE)
                    .contentType(MediaType.TEXT_XML);

            webTestClient.post()
                    .uri("/v1/directories/studies/{studyName}?description={description}&isPrivate={isPrivate}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", true, PARENT_DIRECTORY_UUID)
                    .header("userId", USER1)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Test
    public void testCreateStudyError() throws IOException {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_FILE)
                    .contentType(MediaType.TEXT_XML);

            webTestClient.post()
                    .uri("/v1/directories/studies/{studyName}?description={description}&isPrivate={isPrivate}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", true, PARENT_DIRECTORY_UUID)
                    .header("userId", USER1)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    public void testCreateScriptContingencyList() {
        webTestClient.post()
                .uri("/v1/directories/script-contingency-lists/{listName}?isPrivate={isPrivate}&parentDirectoryUuid={parentDirectoryUuid}",
                        "contingencyListScriptName", true, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Contingency list content"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateFiltersContingencyList() {
        webTestClient.post()
                .uri("/v1/directories/filters-contingency-lists/{listName}?isPrivate={isPrivate}&parentDirectoryUuid={parentDirectoryUuid}",
                        "contingencyListScriptName", true, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Contingency list content"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testNewScriptFromFiltersContingencyList() {
        webTestClient.post()
            .uri("/v1/directories/filters-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                    ELEMENT_UUID, "scriptName", PARENT_DIRECTORY_UUID)
            .header("userId", USER1)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void testReplaceFilterContingencyListWithScript() {
        webTestClient.post()
            .uri("/v1/directories/filters-contingency-lists/{id}/replace-with-script",
                    ELEMENT_UUID)
            .header("userId", USER1)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void testCreateFilter() {
        webTestClient.post()
                .uri("/v1/directories/filters?name={name}&type={type}&isPrivate={isPrivate}&parentDirectoryUuid={parentDirectoryUuid}",
                        "contingencyListScriptName", "", true, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Filter content"))
                .exchange()
                .expectStatus().isOk();
        var requests = getRequestsDone(1);
    }

    private Set<String> getRequestsDone(int n) {
        return IntStream.range(0, n).mapToObj(i -> {
            try {
                return server.takeRequest(0, TimeUnit.SECONDS).getPath();
            } catch (InterruptedException e) {
                //LOGGER.error("Error while attempting to get the request done : ", e);
            }
            return null;
        }).collect(Collectors.toSet());
    }

    @Test
    public void testNewScriptFromFilter() {
        webTestClient.post()
                .uri("/v1/directories/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                        FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testReplaceFilterWithScript() {
        webTestClient.post()
                .uri("/v1/directories/filters/{id}/replace-with-script",
                        FILTER_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDeleteElement() {
        webTestClient.delete()
                .uri("/v1/directories/{elementUuid}",
                        FILTER_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testSetAccessRights() {
        webTestClient.put()
                .uri("/v1/directories/{elementUuid}/rights",
                        FILTER_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(true))
                .exchange()
                .expectStatus().isOk();
    }
}
