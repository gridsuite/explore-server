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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.gridsuite.explore.server.services.MockRemoteServices.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
//@TestExecutionListeners //(listeners = MockRestServiceServerResetTestExecutionListener.class)
@AutoConfigureMockMvc //we want to test the controller
@AutoConfigureWebClient @AutoConfigureCache //we mock http clients
//@DataJpaTest @AutoConfigureTestDatabase(replace = Replace.NONE) //reset datas between tests
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
    void teardownMockRestCaseService() {
        mockRestCaseService.verify();
    }

    @AfterEach
    void teardownMockRestContingencyListService() {
        mockRestContingencyListService.verify();
    }

    @AfterEach
    void teardownMockRestDirectoryService() {
        mockRestDirectoryService.verify();
    }

    @AfterEach
    void teardownMockRestFilterService() {
        mockRestFilterService.verify();
    }

    @AfterEach
    void teardownMockRestStudyService() {
        mockRestStudyService.verify();
    }

    @AfterEach
    void teardownMockRestParametersService() {
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
        if(errors.size() == 1) {
            throw errors.get(0);
        } else if (errors.size() > 1) {
            final AssertionError error = new AssertionError("Some unexpected HTTP call happened");
            //error.setStackTrace(new StackTraceElement[]{Thread.currentThread().getStackTrace()[0]});
            errors.forEach(error::addSuppressed);
            throw error;
        }
    }

    @Test
    void testCreateStudyFromExistingCase() throws Exception {
        //TODO POST /studies/cases/${CASE_UUID}?studyUuid=7b504820-25b6-4ec1-a074-75899b7f058b&duplicateCase=false
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        studyService.expectPostStudies();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .param("duplicateCase", "false")
                        .header("userId", "userId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateStudyFromExistingCaseError() throws Exception {
        //TODO POST /studies/cases/${NON_EXISTING_CASE_UUID}?studyUuid=ee9753e3-24f3-4ba9-9ea3-58fcf14e224a&duplicateCase=false
        studyService.expectPostStudiesCasesNonExistingCaseUuid();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCase() throws Exception {
        //TODO POST /cases
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        caseService.expectPostCasesNoIncorrectOrErrorFile();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
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
        //TODO POST /cases
        caseService.expectPostCasesTestFileWithErrors();
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
    void testCreateScriptContingencyList() throws Exception {
        //TODO POST /script-contingency-lists?id=5d9c5d73-c033-4739-a3df-150d89da773b
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostScriptContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
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
        //TODO POST /script-contingency-lists?id=b6b6ffd8-907c-48b8-81e6-f14879745baa
        //TODO POST /directories/${PARENT_DIRECTORY_WITH_ERROR_UUID}/elements
        contingencyListService.expectPostScriptContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryWithErrorUuidElements();
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
        //TODO POST /form-contingency-lists?id=c3197b47-d3a9-4180-b8e7-0dd027adcf1b
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostFormContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
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
        //TODO POST /identifier-contingency-lists?id=9e5fe771-1587-4d80-a6cb-e541da4febc5
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostIdentifierContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/identifier-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                            "identifierContingencyListName", PARENT_DIRECTORY_UUID, null)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Contingency list content\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testNewScriptFromFormContingencyList() throws Exception {
        //TODO GET /elements/${CONTINGENCY_LIST_UUID}
        //TODO POST /form-contingency-lists/${CONTINGENCY_LIST_UUID}/new-script?newId=0e61aaf8-8984-430b-9e19-2a004897a2eb
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        directoryService.expectGetElementsContingencyListUuid();
        contingencyListService.expectPostFormContingencyListsNewScript();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                            CONTINGENCY_LIST_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testReplaceFormContingencyListWithScript() throws Exception {
        //TODO GET /elements/${CONTINGENCY_LIST_UUID}
        //TODO POST /form-contingency-lists/${CONTINGENCY_LIST_UUID}/replace-with-script
        //TODO POST /elements/${CONTINGENCY_LIST_UUID}/notification?type=UPDATE_DIRECTORY
        directoryService.expectGetElementsContingencyListUuid();
        contingencyListService.expectPostFormContingencyLists();
        directoryService.expectHttpElementsContingencyListUuidNotificationTypeUpdateDirectory();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/replace-with-script", CONTINGENCY_LIST_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateFilter() throws Exception {
        //TODO POST /filters?id=b4a0ce8a-a1be-4e96-9e00-0a269811f9d0
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        filterService.expectPostFiltersIdAny();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
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
        //TODO POST http://voltage_init_parameters/v1/parameters
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        parametersService.expectHttpVoltageInitAny();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/parameters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                            "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Parameters content\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateParameters() throws Exception {
        //TODO PUT http://voltage_init_parameters/v1/parameters/${PARAMETERS_UUID}
        parametersService.expectHttpVoltageInitAny();
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
        //TODO GET /elements/${FILTER_UUID}
        //TODO POST /filters/${FILTER_UUID}/new-script?newId=ef4be392-6346-4cce-997f-7520224e050a
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        directoryService.expectGetElementsFilterUuid();
        filterService.expectPostFiltersNewScript();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                            FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testReplaceFilterWithScript() throws Exception {
        //TODO GET /elements/${FILTER_UUID}
        //TODO PUT /filters/${FILTER_UUID}/replace-with-script
        //TODO POST /elements/${FILTER_UUID}/notification?type=UPDATE_DIRECTORY
        directoryService.expectGetElementsFilterUuid();
        filterService.expectPutFiltersReplaceWithScript();
        directoryService.expectHttpElementsFilterUuidNotificationTypeUpdateDirectory();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/filters/{id}/replace-with-script", FILTER_UUID).header("userId", USER1))
                .andExpect(status().isOk());
    }

    private void deleteElement(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid).header("userId", USER1))
                .andExpect(status().isOk());
    }

    private void deleteElementInvalidType(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid).header("userId", USER1))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void testDeleteElement() throws Exception {
        //TODO GET /elements/${FILTER_UUID}
        //TODO DELETE /filters/${FILTER_UUID}
        //TODO DELETE /elements/${FILTER_UUID}
        //TODO GET /elements/${PRIVATE_STUDY_UUID}
        //TODO DELETE /studies/${PRIVATE_STUDY_UUID}
        //TODO DELETE /elements/${PRIVATE_STUDY_UUID}
        //TODO GET /elements/${CONTINGENCY_LIST_UUID}
        //TODO DELETE /contingency-lists/${CONTINGENCY_LIST_UUID}
        //TODO DELETE /elements/${CONTINGENCY_LIST_UUID}
        //TODO GET /elements/${INVALID_ELEMENT_UUID}
        //TODO DELETE /elements/${INVALID_ELEMENT_UUID}
        //TODO GET /elements/${PARENT_DIRECTORY_UUID}
        //TODO GET /directories/${PARENT_DIRECTORY_UUID}/elements
        //TODO DELETE /elements/${PARENT_DIRECTORY_UUID}
        //TODO GET /elements/${CASE_UUID}
        //TODO DELETE /cases/${CASE_UUID}
        //TODO DELETE /elements/${CASE_UUID}
        //TODO GET /elements/${PARAMETERS_UUID}
        //TODO GET /elements/${PARAMETERS_UUID}
        //TODO DELETE http://voltage_init_parameters/v1/parameters/${PARAMETERS_UUID}
        //TODO DELETE /elements/${PARAMETERS_UUID}
        directoryService.expectGetElementsFilterUuid();
        filterService.expectDeleteFiltersFilterUuid();
        directoryService.expectDeleteElementsFilterUuid();
        directoryService.expectGetElementsPrivateStudyUuid();
        studyService.expectDeleteStudiesPrivateStudyUuid();
        directoryService.expectDeleteElementsPrivateStudyUuid();
        directoryService.expectGetElementsContingencyListUuid();
        contingencyListService.expectDeleteContingencyListsContingencyListUuid();
        directoryService.expectDeleteElementsContingencyListUuid();
        directoryService.expectGetElementsInvalidElementUuid();
        directoryService.expectDeleteElementsInvalidElementUuid();
        directoryService.expectGetElementsParentDirectoryUuid();
        directoryService.expectGetDirectoriesParentDirectoryUuidElements();
        directoryService.expectDeleteElementsParentDirectoryUuid();
        directoryService.expectGetElementsCaseUuid();
        caseService.expectDeleteCasesCaseUuid();
        directoryService.expectDeleteElementsCaseUuid();
        directoryService.expectGetElementsParametersUuid();
        directoryService.expectGetElementsParametersUuid(); // why?
        parametersService.expectDeleteVoltageInitParametersParametersUuid();
        directoryService.expectDeleteElementsParametersUuid();
        expectNoMoreRestCall();

        deleteElement(FILTER_UUID);
        deleteElement(PRIVATE_STUDY_UUID);
        deleteElement(CONTINGENCY_LIST_UUID);
        deleteElementInvalidType(INVALID_ELEMENT_UUID);
        deleteElement(PARENT_DIRECTORY_UUID);
        deleteElement(CASE_UUID);
        deleteElement(PARAMETERS_UUID);
    }

    @Test
    void testGetElementsMetadataWithoutFilter() throws Exception {
        //TODO GET /elements?ids=${FILTER_UUID},${PRIVATE_STUDY_UUID},${CONTINGENCY_LIST_UUID}
        //TODO GET /studies/metadata?ids=${PRIVATE_STUDY_UUID}
        //TODO GET /filters/metadata?ids=${FILTER_UUID}
        //TODO GET /contingency-lists/metadata?ids=${CONTINGENCY_LIST_UUID}
        directoryService.expectGetElementsIdsFilterUuidPrivateStudyUuidContingencyListUuid();
        studyService.expectGetStudiesMetadataIdsPrivateStudyUuid();
        filterService.expectGetFiltersMetadataIdsFilterUuid();
        contingencyListService.expectGetContingencyListsMetadataIdsContingencyListUuid();
        expectNoMoreRestCall();

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)
                        .header("userId", USER1))
                .andExpectAll(status().isOk());
    }

    private static final ElementAttributes filter1 = new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA);
    private static final ElementAttributes filter2 = new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, new AccessRightsAttributes(true), USER1, 0L, null, SPECIFIC_METADATA_2);

    @Test
    void testGetElementsMetadataWithFilterNoEquipment() throws Exception {
        //TODO GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
        //TODO GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
        directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter();
        filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2();
        expectNoMoreRestCall();

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=&elementTypes=FILTER")
                        .header("userId", USER1))
                .andExpectAll(
                    status().isOk(),
                    content().string(mapper.writeValueAsString(List.of(filter1, filter2)))
                );
    }

    @Test
    void testGetElementsMetadataWithFilterGeneratorEquipment() throws Exception {
        //TODO GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
        //TODO GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
        directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter();
        filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2();
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
        //TODO GET /elements?ids=${FILTER_UUID},${FILTER_UUID_2}&elementTypes=FILTER
        //TODO GET /filters/metadata?ids=${FILTER_UUID},${FILTER_UUID_2}
        directoryService.expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter();
        filterService.expectGetFiltersMetadataIdsFilterUuidFilterUuid2();
        expectNoMoreRestCall();

        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&equipmentTypes=LINE&elementTypes=FILTER")
                        .header("userId", USER1))
                .andExpectAll(
                    status().isOk(),
                    content().string(mapper.writeValueAsString(List.of(filter2)))
                );
    }

    @Test
    void testDuplicateCase() throws Exception {
        //TODO POST /cases?duplicateFrom=${CASE_UUID}
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        caseService.expectPostCasesNoIncorrectOrErrorFile();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={parentCaseUuid}&caseName={caseName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            CASE_UUID, CASE1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateFilter() throws Exception {
        //TODO POST /filters?duplicateFrom=${FILTER_UUID}&id=c49165e4-c6fa-4fd9-92ca-ff02c60e0927
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        filterService.expectPostFilters();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/filters?duplicateFrom={parentFilterId}&name={filterName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            FILTER_UUID, FILTER1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateScriptContingencyList() throws Exception {
        //TODO POST /script-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=36a4a418-0e90-4493-80d1-128368a961e0
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostScriptContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/script-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateFormContingencyList() throws Exception {
        //TODO POST /form-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=b85d5ca6-08b7-480c-92df-d80678bccabf
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostFormContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/form-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateIdentifierContingencyList() throws Exception {
        //TODO POST /identifier-contingency-lists?duplicateFrom=${CONTINGENCY_LIST_UUID}&id=6b8c828a-cd29-4685-b6c3-06e871c86e5d
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        contingencyListService.expectPostIdentifierContingencyLists();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/identifier-contingency-lists?duplicateFrom={parentListId}&listName={listName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            CONTINGENCY_LIST_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateStudy() throws Exception {
        //TODO POST /studies?duplicateFrom=${PUBLIC_STUDY_UUID}&studyUuid=5ecab065-afd8-4dcf-908c-d20d206c3e32
        //TODO POST /directories/${PARENT_DIRECTORY_UUID}/elements
        studyService.expectPostStudies();
        directoryService.expectPostDirectoriesParentDirectoryUuidElements();
        expectNoMoreRestCall();

        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={parentStudyUuid}&studyName={studyName}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            PUBLIC_STUDY_UUID, STUDY1, "description", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testCaseCreationErrorWithBadExtension() throws Exception {
        //TODO POST /cases
        caseService.expectPostCasesTestIncorrectFile();
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

    @Test
    void testChangeFilter() throws Exception {
        //TODO PUT /filters/${FILTER_UUID}
        //TODO PUT /elements/${FILTER_UUID}
        filterService.expectPutFilters();
        directoryService.expectPutElements();
        expectNoMoreRestCall();

        final String filter = "{\"type\":\"CRITERIA\",\"equipmentFilterForm\":{\"equipmentType\":\"BATTERY\",\"name\":\"test bbs\",\"countries\":[\"BS\"],\"nominalVoltage\":{\"type\":\"LESS_THAN\",\"value1\":545430,\"value2\":null},\"freeProperties\":{\"region\":[\"north\"],\"totallyFree\":[\"6555\"],\"tso\":[\"ceps\"]}}}";
        final String name = "filter name";
        mockMvc.perform(put("/v1/explore/filters/{id}", FILTER_UUID)
                        .contentType(APPLICATION_JSON)
                        .content(filter)
                        .param("name", name)
                        .header("userId", USER1))
                .andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/filters/");
    }

    @Test
    void testModifyScriptContingencyList() throws Exception {
        //TODO PUT /script-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        //TODO PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        contingencyListService.expectPutScriptContingencyLists();
        directoryService.expectPutElements();
        expectNoMoreRestCall();

        final String scriptContingency = "{\"script\":\"alert(\\\"script contingency\\\")\"}";
        final String name = "script name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                        .contentType(APPLICATION_JSON)
                        .content(scriptContingency)
                        .param("name", name)
                        .param("contingencyListType", ContingencyListType.SCRIPT.name())
                        .header("userId", USER1))
                .andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/script-contingency-lists");
    }

    @Test
    void testModifyFormContingencyList() throws Exception {
        //TODO PUT /form-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        //TODO PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        contingencyListService.expectPutFormContingencyLists();
        directoryService.expectPutElements();
        expectNoMoreRestCall();

        final String formContingency = "{\"equipmentType\":\"LINE\",\"name\":\"contingency EN update1\",\"countries1\":[\"AL\"],\"countries2\":[],\"nominalVoltage1\":{\"type\":\"EQUALITY\",\"value1\":45340,\"value2\":null},\"nominalVoltage2\":null,\"freeProperties1\":{},\"freeProperties2\":{}}";
        final String name = "form contingency name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                        .contentType(APPLICATION_JSON)
                        .content(formContingency)
                        .param("name", name)
                        .param("contingencyListType", ContingencyListType.FORM.name())
                        .header("userId", USER1))
                .andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/form-contingency-lists/");
    }

    @Test
    void testModifyIdentifierContingencyList() throws Exception {
        //TODO PUT /identifier-contingency-lists/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        //TODO PUT /elements/${SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID}
        contingencyListService.expectPutIdentifierContingencyLists();
        directoryService.expectPutElements();
        expectNoMoreRestCall();

        final String identifierContingencyList = "{\"identifierContingencyList\":{\"type\":\"identifier\",\"version\":\"1.0\",\"identifiableType\":\"LINE\",\"identifiers\":[{\"type\":\"LIST\",\"identifierList\":[{\"type\":\"ID_BASED\",\"identifier\":\"34\"},{\"type\":\"ID_BASED\",\"identifier\":\"qs\"}]}]},\"type\":\"IDENTIFIERS\"}";
        final String name = "identifier contingencyList name";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}", SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                        .contentType(APPLICATION_JSON)
                        .content(identifierContingencyList)
                        .param("name", name)
                        .param("contingencyListType", ContingencyListType.IDENTIFIERS.name())
                        .header("userId", USER1))
                .andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests("/v1/identifier-contingency-lists/");
    }

    private void verifyFilterOrContingencyUpdateRequests(String contingencyOrFilterPath) throws UncheckedInterruptedException, AssertionError {
        /*TODO var requests = IntStream.range(0, 2).mapToObj(i -> {
            try {
                var request = server.takeRequest(100L, TimeUnit.MILLISECONDS);
                if (request == null) {
                    throw new AssertionError("Expected 2 requests, got only " + i);
                }
                return new RequestWithBody(request.getPath(), request.getBody().readUtf8());
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }).collect(Collectors.toSet());
        assertThat(requests).as("elementAttributes updated")
                .extracting(RequestWithBody::getPath)
                .anyMatch(path -> path.startsWith(contingencyOrFilterPath));
        assertThat(requests).as("name updated")
                .extracting(RequestWithBody::getPath)
                .anyMatch(path -> path.startsWith("/v1/elements/"));*/
    }

    @Test
    void testGetMetadata() throws Exception {
        //TODO GET /elements?ids=${CASE_UUID}
        //TODO GET /cases/metadata?ids=${CASE_UUID}
        directoryService.expectGetElementsIdsCaseUuid();
        caseService.expectGetCasesMetadataIdsCaseUuid();
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
