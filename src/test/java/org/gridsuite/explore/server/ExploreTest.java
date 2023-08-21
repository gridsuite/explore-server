/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.exceptions.UncheckedInterruptedException;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.RequestWithBody;
import org.gridsuite.explore.server.utils.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@DirtiesContext
@ContextConfiguration(classes = {ExploreApplication.class, TestChannelBinderConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Explore Server tests")
class ExploreTest {
    private static final String TEST_FILE = "testCase.xiidm";
    private static final String TEST_FILE_WITH_ERRORS = "testCase_with_errors.xiidm";
    private static final String TEST_INCORRECT_FILE = "application-default.yml";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_WITH_ERROR_UUID = UUID.randomUUID();
    private static final UUID PRIVATE_STUDY_UUID = UUID.randomUUID();
    private static final UUID PUBLIC_STUDY_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID_2 = UUID.randomUUID();
    private static final UUID CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final UUID INVALID_ELEMENT_UUID = UUID.randomUUID();
    private static final String STUDY_ERROR_NAME = "studyInError";
    private static final String STUDY1 = "study1";
    private static final String CASE1 = "case1";
    private static final String FILTER1 = "filter1";
    private static final String USER1 = "user1";
    public static final String FILTER_CONTINGENCY_LIST = "filterContingencyList";
    public static final String FILTER_CONTINGENCY_LIST_2 = "filterContingencyList2";
    public static final String FILTER = "FILTER";

    private static final Map<String, Object> SPECIFIC_METADATA = Map.of("id", FILTER_UUID);
    private static final Map<String, Object> SPECIFIC_METADATA_2 = Map.of("equipmentType", "LINE", "id", FILTER_UUID_2);
    private static final Map<String, Object> CASE_SPECIFIC_METADATA = Map.of(
            "uuid", CASE_UUID,
            "name", TEST_FILE,
            "format", "XIIDM"
    );

    private static final UUID SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;
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

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();

        // Start the server.
        server.start();

        // Ask the server for its URL. You'll need this to make HTTP requests.
        String baseUrl = server.url("").toString();
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        directoryService.setServerBaseUri(baseUrl);
        studyService.setServerBaseUri(baseUrl);
        filterService.setServerBaseUri(baseUrl);
        contingencyListService.setServerBaseUri(baseUrl);
        caseService.setServerBaseUri(baseUrl);

        String privateStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0, null));
        String listOfPrivateStudyAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0, null)));
        String publicStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PUBLIC_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(false), USER1, 0, null));
        String invalidElementAsString = mapper.writeValueAsString(new ElementAttributes(INVALID_ELEMENT_UUID, "invalidElementName", "INVALID", new AccessRightsAttributes(false), USER1, 0, null));
        String formContingencyListAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0, null));
        String listOfFormContingencyListAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0, null)));
        String filterAttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0, null));
        String filter2AttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, new AccessRightsAttributes(true), USER1, 0, null));
        String listOfFilterAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0, null)));
        String directoryAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PARENT_DIRECTORY_UUID, "directory", "DIRECTORY", new AccessRightsAttributes(true), USER1, 0, null));
        String caseElementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", new AccessRightsAttributes(true), USER1, 0L, null));
        String listElementsAttributesAsString = "[" + filterAttributesAsString + "," + privateStudyAttributesAsString + "," + formContingencyListAttributesAsString + "]";
        String caseInfosAttributesAsString = mapper.writeValueAsString(List.of(CASE_SPECIFIC_METADATA));

        server.setDispatcher(new Dispatcher() {
            @NotNull
            @SneakyThrows
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                final String path = Objects.requireNonNull(request.getPath());
                final String bodyStr = request.getBody().readUtf8();

                if (path.equals("/v1/elements/" + CONTINGENCY_LIST_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse().setBody(formContingencyListAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.equals("/v1/elements/" + FILTER_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse().setBody(filterAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if ("POST".equals(request.getMethod())) {
                    if (path.startsWith("/v1/studies/cases/" + NON_EXISTING_CASE_UUID)) {
                        return new MockResponse().setResponseCode(404);
                    } else if (path.startsWith("/v1/studies")) {
                        if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                            return new MockResponse().setResponseCode(409);
                        } else {
                            return new MockResponse().setResponseCode(200);
                        }
                    } else if (path.startsWith("/v1/cases")) {
                        if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                            return new MockResponse().setResponseCode(409).setBody("invalid file");
                        } else if (bodyStr.contains("filename=\"" + TEST_INCORRECT_FILE + "\"")) {  // import file with errors
                            return new MockResponse().setResponseCode(422).setBody("file with bad extension");
                        } else {
                            return new MockResponse().setResponseCode(200);
                        }
                    } else if (path.equals("/v1/script-contingency-lists?id=" + PARENT_DIRECTORY_WITH_ERROR_UUID)) {
                        return new MockResponse().setResponseCode(500); //TODO not encountered case?
                    } else if (path.startsWith("/v1/script-contingency-lists")) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/form-contingency-lists")) { //include `/v1/form-contingency-lists/.*/new-script/.*`
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/identifier-contingency-lists")) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/filters")) { //include `/v1/filters?id=` & `/v1/filters/.*/new-script.*`
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                        return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/directories/" + PARENT_DIRECTORY_WITH_ERROR_UUID + "/elements")) {
                        return new MockResponse().setResponseCode(500);
                    }
                } else if ("PUT".equals(request.getMethod())) {
                    if (path.startsWith("/v1/elements/")) {
                        return new MockResponse().setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.startsWith("/v1/filters/")) { //include `/v1/filters/.*/replace-with-script`
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/script-contingency-lists/")) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/form-contingency-lists/")) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.startsWith("/v1/identifier-contingency-lists/")) {
                        return new MockResponse().setResponseCode(200);
                    }
                } else if ("GET".equals(request.getMethod())) {
                    if (path.equals("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse().setBody(invalidElementAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse().setBody(directoryAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setBody(formContingencyListAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + FILTER_UUID)) {
                        return new MockResponse().setBody(filterAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + CASE_UUID)) {
                        return new MockResponse().setBody(caseElementAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements/" + PUBLIC_STUDY_UUID)) {
                        return new MockResponse().setBody(publicStudyAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&elementTypes=FILTER")) {
                        return new MockResponse().setBody("[" + filterAttributesAsString + "," + filter2AttributesAsString + "]")
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements?ids=" + CASE_UUID)) {
                        return new MockResponse().setBody("[" + caseElementAttributesAsString + "]")
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/elements?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setBody(listElementsAttributesAsString).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                        return new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/filters/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2)) {
                        return new MockResponse().setBody("[" + mapper.writeValueAsString(SPECIFIC_METADATA) + "," + mapper.writeValueAsString(SPECIFIC_METADATA_2) + "]")
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/filters/metadata?ids=" + FILTER_UUID)) {
                        return new MockResponse().setBody(listOfFilterAttributesAsString.replace("elementUuid", "id")).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/cases/metadata?ids=" + CASE_UUID)) {
                        return new MockResponse().setBody(caseInfosAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/studies/metadata?ids=" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setBody(listOfPrivateStudyAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.equals("/v1/contingency-lists/metadata?ids=" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setBody(listOfFormContingencyListAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    }
                } else if ("DELETE".equals(request.getMethod())) {
                    if (path.equals("/v1/filters/" + FILTER_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/studies/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/contingency-lists/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/elements/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/elements/" + FILTER_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/elements/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.equals("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/(cases|elements)/" + CASE_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else {
                        return new MockResponse().setResponseCode(404);
                    }
                }
                return new MockResponse().setResponseCode(418);
            }
        });
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    @Test
    void testCreateStudyFromExistingCase() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .param("duplicateCase", "false")
                .header("userId", "userId")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateStudyFromExistingCaseError() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCase() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_FILE)
                    .contentType(MediaType.TEXT_XML);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testCaseCreationError() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE_WITH_ERRORS))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_FILE_WITH_ERRORS)
                    .contentType(MediaType.TEXT_XML);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testCreateScriptContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                "contingencyListScriptName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Contingency list content\"}")
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateScriptContingencyListError() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                "contingencyListScriptName", PARENT_DIRECTORY_WITH_ERROR_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Contingency list content\"}")
        ).andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                FILTER_CONTINGENCY_LIST, PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Contingency list content\"}")
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/identifier-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "identifierContingencyListName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Contingency list content\"}")
        ).andExpect(status().isOk());
    }

    @Test
    void testNewScriptFromFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testReplaceFormContingencyListWithScript() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/replace-with-script", CONTINGENCY_LIST_UUID).header("userId", USER1)).andExpect(status().isOk());
    }

    @Test
    void testCreateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "contingencyListScriptName", "", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Filter content\"}")
        ).andExpect(status().isOk());
    }

    @Test
    void testNewScriptFromFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testReplaceFilterWithScript() throws Exception {
        mockMvc.perform(post("/v1/explore/filters/{id}/replace-with-script",
                FILTER_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    public void deleteElement(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid).header("userId", USER1)).andExpect(status().isOk());
    }

    public void deleteElementInvalidType(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid).header("userId", USER1)).andExpect(status().is2xxSuccessful());
    }

    @Test
    void testDeleteElement() throws Exception {
        deleteElement(FILTER_UUID);
        deleteElement(PRIVATE_STUDY_UUID);
        deleteElement(CONTINGENCY_LIST_UUID);
        deleteElementInvalidType(INVALID_ELEMENT_UUID);
        deleteElement(PARENT_DIRECTORY_UUID);
        deleteElement(CASE_UUID);
    }

    @Test
    void testGetElementsMetadata() throws Exception {
        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)
                .header("userId", USER1)
        ).andExpectAll(status().isOk());

        ElementAttributes filter1 = new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA);
        ElementAttributes filter2 = new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA_2);

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=&elementTypes=FILTER")
            .header("userId", USER1))
            .andExpectAll(
                status().isOk(),
                content().string(mapper.writeValueAsString(List.of(filter1, filter2)))
            );

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=GENERATOR&elementTypes=FILTER")
            .header("userId", USER1))
            .andExpectAll(
                status().isOk(),
                content().string(mapper.writeValueAsString(List.of()))
            );

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=LINE&elementTypes=FILTER")
            .header("userId", USER1))
            .andExpectAll(
                status().isOk(),
                content().string(mapper.writeValueAsString(List.of(filter2)))
            );
    }

    @Test
    void testDuplicateCase() throws Exception {
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={parentCaseUuid}&caseName={caseName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}", CASE_UUID, CASE1, "description", PARENT_DIRECTORY_UUID).header("userId", USER1)).andExpect(status().isOk());
    }

    @Test
    void testDuplicateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?duplicateFrom={parentFilterId}&name={filterName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                FILTER_UUID, FILTER1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)).andExpect(status().isOk());
    }

    @Test
    void testDuplicateScriptContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                        CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testDuplicateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/identifier-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testDuplicateStudy() throws Exception {
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={parentStudyUuid}&studyName={studyName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                PUBLIC_STUDY_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testCaseCreationErrorWithBadExtension() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_INCORRECT_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_INCORRECT_FILE, "text/xml", is);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("caseFile", mockFile.getBytes())
                    .filename(TEST_INCORRECT_FILE)
                    .contentType(MediaType.TEXT_XML);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void testChangeFilter() throws Exception {
        final String filter = "{\"type\":\"CRITERIA\",\"equipmentFilterForm\":{\"equipmentType\":\"BATTERY\",\"name\":\"test bbs\",\"countries\":[\"BS\"],\"nominalVoltage\":{\"type\":\"LESS_THAN\",\"value1\":545430,\"value2\":null},\"freeProperties\":{\"region\":[\"north\"],\"totallyFree\":[\"6555\"],\"tso\":[\"ceps\"]}}}";
        final String name = "filter name";
        mockMvc.perform(put("/v1/explore/filters/{id}",
                FILTER_UUID)
                .contentType(APPLICATION_JSON)
                .content(filter)
                .param("name", name)
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/filters/");
    }

    @Test
    void testModifyScriptContingencyList() throws Exception {
        final String scriptContingency = "{\"script\":\"alert(\\\"script contingency\\\")\"}";
        final String name = "script name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(scriptContingency)
                .param("name", name)
                .param("contingencyListType", ContingencyListType.SCRIPT.name())
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/script-contingency-lists");
    }

    @Test
    void testModifyFormContingencyList() throws Exception {
        final String formContingency = "{\"equipmentType\":\"LINE\",\"name\":\"contingency EN update1\",\"countries1\":[\"AL\"],\"countries2\":[],\"nominalVoltage1\":{\"type\":\"EQUALITY\",\"value1\":45340,\"value2\":null},\"nominalVoltage2\":null,\"freeProperties1\":{},\"freeProperties2\":{}}";
        final String name = "form contingency name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(formContingency)
                .param("name", name)
                .param("contingencyListType", ContingencyListType.FORM.name())
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/form-contingency-lists/");
    }

    @Test
    void testModifyIdentifierContingencyList() throws Exception {
        final String identifierContingencyList = "{\"identifierContingencyList\":{\"type\":\"identifier\",\"version\":\"1.0\",\"identifiableType\":\"LINE\",\"identifiers\":[{\"type\":\"LIST\",\"identifierList\":[{\"type\":\"ID_BASED\",\"identifier\":\"34\"},{\"type\":\"ID_BASED\",\"identifier\":\"qs\"}]}]},\"type\":\"IDENTIFIERS\"}";
        final String name = "identifier contingencyList name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(identifierContingencyList)
                .param("name", name)
                .param("contingencyListType", ContingencyListType.IDENTIFIERS.name())
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/identifier-contingency-lists/");
    }

    private void verifyFilterOrContingencyUpdateRequests(String contingencyOrFilterPath) throws UncheckedInterruptedException, AssertionError {
        var requests = TestUtils.getRequestsWithBodyDone(2, server);
        assertThat(requests).as("elementAttributes updated")
                .extracting(RequestWithBody::getPath)
                .anyMatch(path -> path.startsWith(contingencyOrFilterPath));
        assertThat(requests).as("name updated")
                .extracting(RequestWithBody::getPath)
                .anyMatch(path -> path.startsWith("/v1/elements/"));
    }

    @Test
    void testGetMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + CASE_UUID)
                .header("userId", USER1))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        assertThat(mapper.readValue(res, new TypeReference<List<ElementAttributes>>() { }))
                .as("elementsMetadata")
                .hasSize(1)
                .extracting(mapper::writeValueAsString)
                .first()
                .asString()
                .as("caseAttributesAsString")
                .isEqualTo(mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", new AccessRightsAttributes(true), USER1, 0L, null, CASE_SPECIFIC_METADATA)));
    }
}
