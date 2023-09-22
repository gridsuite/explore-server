/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.gridsuite.explore.server.services.MockRemoteServices.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@AutoConfigureMockMvc //we want to test the controller
@AutoConfigureWebClient @AutoConfigureCache //we mock http clients
@SpringBootTest(classes = {ExploreApplication.class, TestConfig.class})
@ExtendWith(ExploreTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Explore Server tests")
public class ExploreTest {
    public static final UUID SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID = UUID.randomUUID();
    public static final String STUDY_ERROR_NAME = "studyInError";
    public static final String CASE1 = "case1";
    public static final String FILTER1 = "filter1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired @Qualifier("mockRestSrvCaseService")
    private MockRestServiceServer mockRestCaseService;
    @Autowired @Qualifier("mockRestSrvContingencyListService")
    private MockRestServiceServer mockRestContingencyListService;
    @Autowired @Qualifier("mockRestSrvDirectoryService")
    private MockRestServiceServer mockRestDirectoryService;
    @Autowired @Qualifier("mockRestSrvFilterService")
    private MockRestServiceServer mockRestFilterService;
    @Autowired @Qualifier("mockRestSrvStudyService")
    private MockRestServiceServer mockRestStudyService;
    @Autowired @Qualifier("mockRestSrvParametersService")
    private Map<ParametersType, MockRestServiceServer> mockRestParametersServices;
    @Autowired
    private ObjectMapper mapper;

    private MockCaseService caseService;
    private MockContingencyListService contingencyListService;
    private MockDirectoryService directoryService;
    private MockFilterService filterService;
    private MockStudyService studyService;
    private MockParametersService parametersService;

    @BeforeAll
    void init() {
        caseService = new MockCaseService(mockRestCaseService, mapper);
        contingencyListService = new MockContingencyListService(mockRestContingencyListService, mapper);
        directoryService = new MockDirectoryService(mockRestDirectoryService, mapper);
        filterService = new MockFilterService(mockRestFilterService, mapper);
        studyService = new MockStudyService(mockRestStudyService, mapper);
        parametersService = new MockParametersService(mockRestParametersServices, mapper);
    }

    private void expectNoMoreRestCall() {
        caseService.expectNoMoreCall();
        contingencyListService.expectNoMoreCall();
        directoryService.expectNoMoreCall();
        filterService.expectNoMoreCall();
        studyService.expectNoMoreCall();
        parametersService.expectNoMoreCall();
    }

    @BeforeEach
    void setup() {
        mockRestCaseService.reset();
        mockRestContingencyListService.reset();
        mockRestDirectoryService.reset();
        mockRestFilterService.reset();
        mockRestStudyService.reset();
        mockRestParametersServices.values().forEach(MockRestServiceServer::reset);
    }

    @AfterEach
    void verifyRequestsMockRestCaseService() {
        mockRestCaseService.verify();
    }

    @AfterEach
    void verifyRequestsMockRestContingencyListService() {
        mockRestContingencyListService.verify();
    }

    @AfterEach
    void verifyRequestsMockRestDirectoryService() {
        mockRestDirectoryService.verify();
    }

    @AfterEach
    void verifyRequestsMockRestFilterService() {
        mockRestFilterService.verify();
    }

    @AfterEach
    void verifyRequestsMockRestStudyService() {
        mockRestStudyService.verify();
    }

    @AfterEach
    void verifyRequestsMockRestParametersService() {
        final List<AssertionError> errors = mockRestParametersServices.values().stream().filter(Objects::nonNull)
                .map(mockServer -> {
                    try {
                        mockServer.verify();
                        return null;
                    } catch (final AssertionError ex) {
                        return ex;
                    }
                })
                .filter(Objects::nonNull).toList();
        //TODO replace with SoftAssertions when AssertJ updated
        if (errors.size() == 1) {
            throw errors.get(0);
        } else if (errors.size() > 1) {
            final AssertionError error = new AssertionError("Some unexpected HTTP call happened");
            //error.setStackTrace(new StackTraceElement[]{Thread.currentThread().getStackTrace()[0]});
            errors.forEach(error::addSuppressed);
            throw error;
        }
    }

    @DisplayName("test creation elements")
    @Nested
    class ExploreTestCreate {
        @DisplayName("test creation studies")
        @Nested
        class ExploreTestCreateStudy {
            @Test
            void testCreateStudyFromExistingCase() throws Exception {
                studyService.expectPostStudies(); //POST /studies/cases/${CASE_UUID}?studyUuid=00000000-0000-0000-0000-000000000000&duplicateCase=false
                directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                                .param("duplicateCase", "false")
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            }

            @Test
            void testCreateStudyFromExistingCaseError() throws Exception {
                studyService.expectPostStudiesCasesNonExistingCaseUuid(); //POST /studies/cases/${NON_EXISTING_CASE_UUID}?studyUuid=00000000-0000-0000-0000-000000000000&duplicateCase=false
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNotFound());
            }
        }

        @DisplayName("test creation cases")
        @Nested
        class ExploreTestCreateCase {
            @Test
            void testCreateCase() throws Exception {
                caseService.expectPostCasesNoIncorrectOrErrorFile(); //POST /cases
                directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
                expectNoMoreRestCall();

                try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
                    MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

                    mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                        STUDY1, "description", PARENT_DIRECTORY_UUID)
                                    .file(mockFile)
                                    .header("userId", USER1)
                                    .contentType(MediaType.MULTIPART_FORM_DATA))
                            .andExpect(status().isOk());
                }
            }

            @Test
            void testCaseCreationError() throws Exception {
                caseService.expectPostCasesTestFileWithErrors(); //POST /cases
                expectNoMoreRestCall();

                try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE_WITH_ERRORS))) {
                    MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, "text/xml", is);

                    mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                        STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID)
                                    .file(mockFile)
                                    .header("userId", USER1)
                                    .contentType(MediaType.MULTIPART_FORM_DATA))
                            .andExpect(status().isBadRequest());
                }
            }

            @Test
            void testCaseCreationErrorWithBadExtension() throws Exception {
                caseService.expectPostCasesTestIncorrectFile(); //POST /cases
                expectNoMoreRestCall();

                try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_INCORRECT_FILE))) {
                    MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_INCORRECT_FILE, "text/xml", is);

                    mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                        STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID)
                                    .file(mockFile)
                                    .header("userId", USER1)
                                    .contentType(MediaType.MULTIPART_FORM_DATA))
                            .andExpect(status().isUnprocessableEntity());
                }
            }
        }

        @DisplayName("test creation contingency list")
        @Nested
        class ExploreTestCreateScriptContingencyList {
            @Test
            void testCreateScriptContingencyList() throws Exception {
                contingencyListService.expectPostScriptContingencyLists(); //POST /script-contingency-lists?id=00000000-0000-0000-0000-000000000000
                directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                                    "contingencyListScriptName", PARENT_DIRECTORY_UUID, null)
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Contingency list content\"}"))
                        .andExpect(status().isOk());
            }

            @Test
            void testCreateScriptContingencyListError() throws Exception {
                contingencyListService.expectPostScriptContingencyLists(); //POST /script-contingency-lists?id=00000000-0000-0000-0000-000000000000
                directoryService.expectPostDirectoriesParentDirectoryWithErrorUuidElements(); //POST /directories/${PARENT_DIRECTORY_WITH_ERROR_UUID}/elements
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                                    "contingencyListScriptName", PARENT_DIRECTORY_WITH_ERROR_UUID, null)
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Contingency list content\"}"))
                        .andExpect(status().isInternalServerError());
            }

            @Test
            void testCreateFormContingencyList() throws Exception {
                contingencyListService.expectPostFormContingencyLists(); //POST /form-contingency-lists?id=00000000-0000-0000-0000-000000000000
                directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/form-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                                    FILTER_CONTINGENCY_LIST, PARENT_DIRECTORY_UUID, null)
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Contingency list content\"}"))
                        .andExpect(status().isOk());
            }

            @Test
            void testCreateIdentifierContingencyList() throws Exception {
                contingencyListService.expectPostIdentifierContingencyLists(); //POST /identifier-contingency-lists?id=00000000-0000-0000-0000-000000000000
                directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
                expectNoMoreRestCall();

                mockMvc.perform(post("/v1/explore/identifier-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                                    "identifierContingencyListName", PARENT_DIRECTORY_UUID, null)
                                .header("userId", USER1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"Contingency list content\"}"))
                        .andExpect(status().isOk());
            }
        }

        @Test
        void testCreateFilter() throws Exception {
            filterService.expectPostFiltersIdAny(); //POST /filters?id=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/filters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                                "contingencyListScriptName", "", PARENT_DIRECTORY_UUID, null)
                            .header("userId", USER1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"Filter content\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void testCreateParameters() throws Exception {
            parametersService.expectHttpVoltageInitAny(); //POST http://voltage_init_parameters/v1/parameters
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/parameters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                                "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                            .header("userId", USER1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"Parameters content\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testNewScriptFromFormContingencyList() throws Exception {
        directoryService.expectGetElementsContingencyListUuid(); //GET /elements/${CONTINGENCY_LIST_UUID}
        contingencyListService.expectPostFormContingencyListsNewScript(); //POST /form-contingency-lists/${CONTINGENCY_LIST_UUID}/new-script?newId=00000000-0000-0000-0000-000000000000
        directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                            CONTINGENCY_LIST_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testReplaceFormContingencyListWithScript() throws Exception {
        directoryService.expectGetElementsContingencyListUuid(); //GET /elements/${CONTINGENCY_LIST_UUID}
        contingencyListService.expectPostFormContingencyLists(); //POST /form-contingency-lists/${CONTINGENCY_LIST_UUID}/replace-with-script
        directoryService.expectHttpElementsContingencyListUuidNotificationTypeUpdateDirectory(); //POST /elements/${CONTINGENCY_LIST_UUID}/notification?type=UPDATE_DIRECTORY
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/replace-with-script", CONTINGENCY_LIST_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateParameters() throws Exception {
        parametersService.expectHttpVoltageInitAny(); //PUT http://voltage_init_parameters/v1/parameters/${PARAMETERS_UUID}
        expectNoMoreRestCall();

        mockMvc.perform(put("/v1/explore/parameters/{id}?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                            PARAMETERS_UUID, "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"new Parameters content\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testNewScriptFromFilter() throws Exception {
        directoryService.expectGetElementsFilterUuid(); //GET /elements/${FILTER_UUID}
        filterService.expectPostFiltersNewScript(); //POST /filters/${FILTER_UUID}/new-script?newId=00000000-0000-0000-0000-000000000000
        directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                            FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testReplaceFilterWithScript() throws Exception {
        directoryService.expectGetElementsFilterUuid(); //GET /elements/${FILTER_UUID}
        filterService.expectPutFiltersReplaceWithScript(); //PUT /filters/${FILTER_UUID}/replace-with-script
        directoryService.expectHttpElementsFilterUuidNotificationTypeUpdateDirectory(); //POST /elements/${FILTER_UUID}/notification?type=UPDATE_DIRECTORY
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/filters/{id}/replace-with-script", FILTER_UUID).header("userId", USER1))
                .andExpect(status().isOk());
    }

    @DisplayName("tests delete element")
    @Nested
    class ExploreTestDeleteElements {
        private void deleteElement(UUID elementUUid) throws Exception {
            mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid).header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDeleteFilterElement() throws Exception {
            directoryService.expectGetElementsFilterUuid(); //GET /elements/${FILTER_UUID}
            filterService.expectDeleteFiltersFilterUuid(); //DELETE /filters/${FILTER_UUID}
            directoryService.expectDeleteElementsFilterUuid(); //DELETE /elements/${FILTER_UUID}
            expectNoMoreRestCall();

            deleteElement(FILTER_UUID);
        }

        @Test
        void testDeleteElementPrivateStudy() throws Exception {
            directoryService.expectGetElementsPrivateStudyUuid(); //GET /elements/${PRIVATE_STUDY_UUID}
            studyService.expectDeleteStudiesPrivateStudyUuid(); //DELETE /studies/${PRIVATE_STUDY_UUID}
            directoryService.expectDeleteElementsPrivateStudyUuid(); //DELETE /elements/${PRIVATE_STUDY_UUID}
            expectNoMoreRestCall();

            deleteElement(PRIVATE_STUDY_UUID);
        }

        @Test
        void testDeleteContingencyElement() throws Exception {
            directoryService.expectGetElementsContingencyListUuid(); //GET /elements/${CONTINGENCY_LIST_UUID}
            contingencyListService.expectDeleteContingencyListsContingencyListUuid(); //DELETE /contingency-lists/${CONTINGENCY_LIST_UUID}
            directoryService.expectDeleteElementsContingencyListUuid(); //DELETE /elements/${CONTINGENCY_LIST_UUID}
            expectNoMoreRestCall();

            deleteElement(CONTINGENCY_LIST_UUID);
        }

        @Test
        void testDeleteInvalidElement() throws Exception {
            directoryService.expectGetElementsInvalidElementUuid(); //GET /elements/${INVALID_ELEMENT_UUID}
            directoryService.expectDeleteElementsInvalidElementUuid(); //DELETE /elements/${INVALID_ELEMENT_UUID}
            expectNoMoreRestCall();

            mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", INVALID_ELEMENT_UUID).header("userId", USER1))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        void testDeleteParentDirectoryElement() throws Exception {
            directoryService.expectGetElementsParentDirectoryUuid(); //GET /elements/${PARENT_DIRECTORY_UUID}
            directoryService.expectGetDirectoriesParentDirectoryUuidElements(); //GET /directories/${PARENT_DIRECTORY_UUID}/elements
            directoryService.expectDeleteElementsParentDirectoryUuid(); //DELETE /elements/${PARENT_DIRECTORY_UUID}
            expectNoMoreRestCall();

            deleteElement(PARENT_DIRECTORY_UUID);
        }

        @Test
        void testDeleteCaseElement() throws Exception {
            directoryService.expectGetElementsCaseUuid(); //GET /elements/${CASE_UUID}
            caseService.expectDeleteCasesCaseUuid(); //DELETE /cases/${CASE_UUID}
            directoryService.expectDeleteElementsCaseUuid(); //DELETE /elements/${CASE_UUID}
            expectNoMoreRestCall();

            deleteElement(CASE_UUID);
        }

        @Test
        void testDeleteParametersElement() throws Exception {
            directoryService.expectGetElementsParametersUuid(); //GET /elements/${PARAMETERS_UUID}
            directoryService.expectGetElementsParametersUuid(); // why?  //GET /elements/${PARAMETERS_UUID}
            parametersService.expectDeleteVoltageInitParametersParametersUuid(); //DELETE http://voltage_init_parameters/v1/parameters/${PARAMETERS_UUID}
            directoryService.expectDeleteElementsParametersUuid(); //DELETE /elements/${PARAMETERS_UUID}
            expectNoMoreRestCall();

            deleteElement(PARAMETERS_UUID);
        }
    }

    @DisplayName("tests get elements metadata")
    @Nested
    class ExploreTestGetElementsMetadata {
        @Test
        void testGetElementsMetadataWithoutFilter() throws Exception {
            directoryService.expectGetElementsIdsFilterUuidPrivateStudyUuidContingencyListUuid(); //GET /elements?ids=${FILTER_UUID},${PRIVATE_STUDY_UUID},${CONTINGENCY_LIST_UUID}
            studyService.expectGetStudiesMetadataIdsPrivateStudyUuid(); //GET /studies/metadata?ids=${PRIVATE_STUDY_UUID}
            filterService.expectGetFiltersMetadataIdsFilterUuid(); //GET /filters/metadata?ids=${FILTER_UUID}
            contingencyListService.expectGetContingencyListsMetadataIdsContingencyListUuid(); //GET /contingency-lists/metadata?ids=${CONTINGENCY_LIST_UUID}
            expectNoMoreRestCall();

            mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)
                            .header("userId", USER1))
                    .andExpectAll(status().isOk());
        }

        private static final ElementAttributes FILTER1 = new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA);
        private static final ElementAttributes FILTER2 = new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA_2);

        @Test
        void testGetElementsMetadataWithFilterNoEquipment() throws Exception {
            directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter(); //GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
            filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2(); //GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
            expectNoMoreRestCall();

            mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=&elementTypes=FILTER")
                            .header("userId", USER1))
                    .andExpectAll(
                            status().isOk(),
                            content().string(mapper.writeValueAsString(List.of(FILTER1, FILTER2)))
                );
        }

        @Test
        void testGetElementsMetadataWithFilterGeneratorEquipment() throws Exception {
            directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter(); //GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
            filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2(); //GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
            expectNoMoreRestCall();

            mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=GENERATOR&elementTypes=FILTER")
                            .header("userId", USER1))
                    .andExpectAll(
                        status().isOk(),
                        content().string("[]")
                );
        }

        @Test
        void testGetElementsMetadataWithFilterLineEquipment() throws Exception {
            directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter(); //GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
            filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2(); //GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
            expectNoMoreRestCall();

            mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=LINE&elementTypes=FILTER")
                            .header("userId", USER1))
                    .andExpectAll(
                            status().isOk(),
                            content().string(mapper.writeValueAsString(List.of(FILTER2)))
                );
        }

        @Test
        void testGetCaseMetadata() throws Exception {
            directoryService.expectGetElementsIdsCaseUuid(); //GET /elements?ids=${CASE_UUID}
            caseService.expectGetCasesMetadataIdsCaseUuid(); //GET /cases/metadata?ids=${CASE_UUID}
            expectNoMoreRestCall();

            MvcResult result = mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + CASE_UUID)
                                                .header("userId", USER1))
                                        .andExpect(status().isOk())
                                        .andReturn();
            String res = result.getResponse().getContentAsString();
            assertThat(mapper.readValue(res, new TypeReference<List<ElementAttributes>>() { }))
                    .as("elementsMetadata")
                    .hasSize(1) //TODO replace only when assertj updated
                    .extracting(mapper::writeValueAsString)
                    .first()
                    .asString()
                    .as("caseAttributesAsString")
                    .isEqualTo(mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", new AccessRightsAttributes(true), USER1, 0L, null, CASE_SPECIFIC_METADATA)));
        }
    }

    @DisplayName("tests duplicate elements")
    @Nested
    class ExploreTestDuplicate {
        @Test
        void testDuplicateCase() throws Exception {
            caseService.expectPostCasesNoIncorrectOrErrorFile(); //POST /cases?duplicateFrom=${CASE_UUID}
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/cases?duplicateFrom={parentCaseUuid}&caseName={caseName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                CASE_UUID, CASE1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDuplicateFilter() throws Exception {
            filterService.expectPostFilters(); //POST /filters?duplicateFrom=${FILTER_UUID}&id=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/filters?duplicateFrom={parentFilterId}&name={filterName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                FILTER_UUID, FILTER1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDuplicateScriptContingencyList() throws Exception {
            contingencyListService.expectPostScriptContingencyLists(); //POST /script-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/script-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDuplicateFormContingencyList() throws Exception {
            contingencyListService.expectPostFormContingencyLists(); //POST /form-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/form-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDuplicateIdentifierContingencyList() throws Exception {
            contingencyListService.expectPostIdentifierContingencyLists(); //POST /identifier-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/identifier-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testDuplicateStudy() throws Exception {
            studyService.expectPostStudies(); //POST /studies?duplicateFrom=${PUBLIC_STUDY_UUID}&studyUuid=00000000-0000-0000-0000-000000000000
            directoryService.expectPostDirectoriesParentDirectoryUuidElements(); //POST /directories/${PARENT_DIRECTORY_UUID}/elements
            expectNoMoreRestCall();

            mockMvc.perform(post("/v1/explore/studies?duplicateFrom={parentStudyUuid}&studyName={studyName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                                PUBLIC_STUDY_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testChangeFilter() throws Exception {
        filterService.expectPutFilters(); //PUT /filters/${FILTER_UUID}
        directoryService.expectPutElements(); //PUT /elements/${FILTER_UUID}
        expectNoMoreRestCall();

        mockMvc.perform(put("/v1/explore/filters/{id}", FILTER_UUID)
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"CRITERIA\",\"equipmentFilterForm\":{\"equipmentType\":\"BATTERY\",\"name\":\"test bbs\",\"countries\":[\"BS\"],\"nominalVoltage\":{\"type\":\"LESS_THAN\",\"value1\":545430,\"value2\":null},\"freeProperties\":{\"region\":[\"north\"],\"totallyFree\":[\"6555\"],\"tso\":[\"ceps\"]}}}")
                        .param("name", "filter name")
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @DisplayName("tests modify contingency list")
    @Nested
    class ExploreTestModifyContingencyList {
        @Test
        void testModifyScriptContingencyList() throws Exception {
            contingencyListService.expectPutScriptContingencyLists(); //PUT /script-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            directoryService.expectPutElements(); //PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            expectNoMoreRestCall();

            mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                            .contentType(APPLICATION_JSON)
                            .content("{\"script\":\"alert(\\\"script contingency\\\")\"}")
                            .param("name", "script name")
                            .param("contingencyListType", ContingencyListType.SCRIPT.name())
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testModifyFormContingencyList() throws Exception {
            contingencyListService.expectPutFormContingencyLists(); //PUT /form-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            directoryService.expectPutElements(); //PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            expectNoMoreRestCall();

            mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                            .contentType(APPLICATION_JSON)
                            .content("{\"equipmentType\":\"LINE\",\"name\":\"contingency EN update1\",\"countries1\":[\"AL\"],\"countries2\":[],\"nominalVoltage1\":{\"type\":\"EQUALITY\",\"value1\":45340,\"value2\":null},\"nominalVoltage2\":null,\"freeProperties1\":{},\"freeProperties2\":{}}")
                            .param("name", "form contingency name")
                            .param("contingencyListType", ContingencyListType.FORM.name())
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }

        @Test
        void testModifyIdentifierContingencyList() throws Exception {
            contingencyListService.expectPutIdentifierContingencyLists(); //PUT /identifier-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            directoryService.expectPutElements(); //PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
            expectNoMoreRestCall();

            mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                            .contentType(APPLICATION_JSON)
                            .content("{\"identifierContingencyList\":{\"type\":\"identifier\",\"version\":\"1.0\",\"identifiableType\":\"LINE\",\"identifiers\":[{\"type\":\"LIST\",\"identifierList\":[{\"type\":\"ID_BASED\",\"identifier\":\"34\"},{\"type\":\"ID_BASED\",\"identifier\":\"qs\"}]}]},\"type\":\"IDENTIFIERS\"}")
                            .param("name", "identifier contingencyList name")
                            .param("contingencyListType", ContingencyListType.IDENTIFIERS.name())
                            .header("userId", USER1))
                    .andExpect(status().isOk());
        }
    }
}
