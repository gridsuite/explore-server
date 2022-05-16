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
import okio.Buffer;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.CaseService;
import org.gridsuite.explore.server.services.ContingencyListService;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.FilterService;
import org.gridsuite.explore.server.services.StudyService;
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
import java.util.UUID;

import static org.junit.Assert.assertEquals;

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
    private CaseService caseService;

    @Autowired
    private ObjectMapper mapper;

    private MockWebServer server;

    private static final String TEST_FILE = "testCase.xiidm";
    private static final String TEST_FILE_WITH_ERRORS = "testCase_with_errors.xiidm";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID PRIVATE_STUDY_UUID = UUID.randomUUID();
    private static final UUID PUBLIC_STUDY_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();
    private static final UUID CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final UUID INVALID_ELEMENT_UUID = UUID.randomUUID();
    private static final String STUDY_ERROR_NAME = "studyInError";
    private static final String STUDY1 = "study1";
    private static final String CASE1 = "case1";
    private static final String FILTER1 = "filter1";
    private static final String USER1 = "user1";

    @Before
    public void setup() throws IOException {
        server = new MockWebServer();

        // Start the server.
        server.start();

        // Ask the server for its URL. You'll need this to make HTTP requests.
        HttpUrl baseHttpUrl = server.url("");
        String baseUrl = baseHttpUrl.toString().substring(0, baseHttpUrl.toString().length() - 1);

        directoryService.setDirectoryServerBaseUri(baseUrl);
        studyService.setStudyServerBaseUri(baseUrl);
        filterService.setFilterServerBaseUri(baseUrl);
        contingencyListService.setActionsServerBaseUri(baseUrl);
        caseService.setBaseUri(baseUrl);

        String privateStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0, null));
        String publicStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PUBLIC_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(false), USER1, 0, null));
        String invalidElementAsString = mapper.writeValueAsString(new ElementAttributes(INVALID_ELEMENT_UUID, "invalidElementName", "INVALID", new AccessRightsAttributes(false), USER1, 0, null));
        String formContingencyListAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CONTINGENCY_LIST_UUID, "filterContingencyList", "CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0, null));
        String filterAttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID, "filterContingencyList", "FILTER", new AccessRightsAttributes(true), USER1, 0, null));
        String directoryAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PARENT_DIRECTORY_UUID, "directory", "DIRECTORY", new AccessRightsAttributes(true), USER1, 0, null));
        String caseAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", new AccessRightsAttributes(true), USER1, 0, null));

        String listElementsAttributesAsString = "[" + filterAttributesAsString + "," + privateStudyAttributesAsString + "," + formContingencyListAttributesAsString + "]";
        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = Objects.requireNonNull(request.getPath());
                Buffer body = request.getBody();

                if (path.matches("/v1/studies/cases/" + NON_EXISTING_CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(404);
                } else if (path.matches("/v1/studies.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse().setResponseCode(409);
                    } else {
                        return new MockResponse().setResponseCode(200);
                    }
                } else if (path.matches("/v1/cases.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse().setResponseCode(409).setBody("invalid file");
                    } else {
                        return new MockResponse().setResponseCode(200);
                    }
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(formContingencyListAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + FILTER_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(filterAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + CASE_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(caseAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + PRIVATE_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + PUBLIC_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(publicStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(listElementsAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/contingency-lists/metadata[?]ids=" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(formContingencyListAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/script-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/form-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/form-contingency-lists/.*/new-script/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/new-script.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters\\?id=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/replace-with-script") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if ("GET".equals(request.getMethod())) {
                    if (path.matches("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse().setBody(invalidElementAsString).setResponseCode(200) .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                        return new MockResponse().setResponseCode(200) .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse().setBody(directoryAttributesAsString).setResponseCode(200) .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/filters/metadata[?]ids=" + FILTER_UUID)) {
                        return new MockResponse().setBody(filterAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)                            .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/studies/metadata[?]ids=" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setBody(privateStudyAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                    }
                } else if ("DELETE".equals(request.getMethod())) {
                    if (path.matches("/v1/filters/" + FILTER_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/studies/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/contingency-lists/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + FILTER_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/(cases|elements)/" + CASE_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    }
                    return new MockResponse().setResponseCode(404);
                }
                return  new MockResponse().setResponseCode(418);
            }
        };
        server.setDispatcher(dispatcher);
    }

    @Test
    public void testCreateStudyFromExistingCase() {
        webTestClient.post()
                .uri("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateStudyFromExistingCaseError() {
        webTestClient.post()
                .uri("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
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
                    .uri("/v1/explore/studies/{studyName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID)
                    .header("userId", USER1)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Test
    public void testCreateCase() throws IOException {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                .filename(TEST_FILE)
                .contentType(MediaType.TEXT_XML);

            webTestClient.post()
                .uri("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                    STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk();
        }
    }

    @Test
    public void testCaseCreationError() throws IOException {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE_WITH_ERRORS))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                .filename(TEST_FILE_WITH_ERRORS)
                .contentType(MediaType.TEXT_XML);

            webTestClient.post()
                .uri("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                    STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    public void testCreateStudyError() throws IOException {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE_WITH_ERRORS))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_FILE_WITH_ERRORS)
                    .contentType(MediaType.TEXT_XML);

            webTestClient.post()
                    .uri("/v1/explore/studies/{studyName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID)
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
                .uri("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                        "contingencyListScriptName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Contingency list content"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateFormContingencyList() {
        webTestClient.post()
                .uri("/v1/explore/form-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                        "filterContingencyList", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Contingency list content"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testNewScriptFromFormContingencyList() {
        webTestClient.post()
            .uri("/v1/explore/form-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                    CONTINGENCY_LIST_UUID, "scriptName", PARENT_DIRECTORY_UUID)
            .header("userId", USER1)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void testReplaceFormContingencyListWithScript() {
        webTestClient.post()
            .uri("/v1/explore/form-contingency-lists/{id}/replace-with-script",
                    CONTINGENCY_LIST_UUID)
            .header("userId", USER1)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    public void testCreateFilter() {
        webTestClient.post()
                .uri("/v1/explore/filters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                        "contingencyListScriptName", "", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("Filter content"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testNewScriptFromFilter() {
        webTestClient.post()
                .uri("/v1/explore/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                        FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testReplaceFilterWithScript() {
        webTestClient.post()
                .uri("/v1/explore/filters/{id}/replace-with-script",
                        FILTER_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    public void deleteElement(UUID elementUUid) {
        webTestClient.delete()
                .uri("/v1/explore/elements/{elementUuid}",
                        elementUUid)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    public void deleteElementInvalidType(UUID elementUUid) {
        var res = webTestClient.delete()
            .uri("/v1/explore/elements/{elementUuid}",
                elementUUid)
            .header("userId", USER1)
            .exchange()
            .expectStatus().is5xxServerError().expectBody(Object.class).returnResult().getResponseBody();
        assertEquals(ExploreException.Type.UNKNOWN_ELEMENT_TYPE.name(), res);
    }

    @Test
    public void testDeleteElement() {
        deleteElement(FILTER_UUID);
        deleteElement(PRIVATE_STUDY_UUID);
        deleteElement(CONTINGENCY_LIST_UUID);
        deleteElementInvalidType(INVALID_ELEMENT_UUID);
        deleteElement(PARENT_DIRECTORY_UUID);
        deleteElement(CASE_UUID);
    }

    @Test
    public void testGetElementsMetadata() {
        webTestClient.get()
                .uri("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDuplicateCase() {
        webTestClient.post()
                .uri("/v1/explore/cases?duplicateFrom={parentCaseUuid}&caseName={caseName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, CASE1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDuplicateFilter() {
        webTestClient.post()
                .uri("/v1/explore/filters?duplicateFrom={parentFilterId}&name={filterName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        FILTER_UUID, FILTER1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDuplicateScriptContingencyList() {
        webTestClient.post()
                .uri("/v1/explore/script-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDuplicateFormContingencyList() {
        webTestClient.post()
                .uri("/v1/explore/form-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDuplicateStudy() {
        webTestClient.post()
                .uri("/v1/explore/studies?duplicateFrom={parentStudyUuid}&studyName={studyName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        PUBLIC_STUDY_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .exchange()
                .expectStatus().isOk();
    }
}
