/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.gridsuite.explore.server.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.MAX_ELEMENTS_EXCEEDED;
import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {ExploreApplication.class})
public class ExploreTest {
    private static final String TEST_FILE = "testCase.xiidm";
    private static final String TEST_FILE_WITH_ERRORS = "testCase_with_errors.xiidm";

    private static final String TEST_INCORRECT_FILE = "application-default.yml";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_WITH_ERROR_UUID = UUID.randomUUID();
    private static final UUID PRIVATE_STUDY_UUID = UUID.randomUUID();
    private static final UUID FORBIDDEN_STUDY_UUID = UUID.randomUUID();
    private static final UUID NOT_FOUND_STUDY_UUID = UUID.randomUUID();
    private static final UUID PUBLIC_STUDY_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID_2 = UUID.randomUUID();
    private static final UUID CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final UUID INVALID_ELEMENT_UUID = UUID.randomUUID();
    private static final UUID PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID MODIFICATION_UUID = UUID.randomUUID();
    private static final UUID STUDY_COPY_UUID = UUID.randomUUID();
    private static final UUID CASE_COPY_UUID = UUID.randomUUID();
    private static final UUID CONTINGENCY_LIST_COPY_UUID = UUID.randomUUID();
    private static final UUID FILTER_COPY_UUID = UUID.randomUUID();
    private static final UUID PARAMETER_COPY_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_COPY_UUID = UUID.randomUUID();
    private static final String STUDY_ERROR_NAME = "studyInError";
    private static final String STUDY1 = "study1";
    private static final String USER1 = "user1";
    private static final String USER_WITH_CASE_LIMIT_EXCEEDED = "limitedUser";
    private static final String USER_WITH_CASE_LIMIT_NOT_EXCEEDED = "limitedUser2";
    private static final String USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 = "limitedUser3";

    private static final String USER_NOT_FOUND = "userNotFound";
    private static final String USER_UNEXPECTED_ERROR = "unexpectedErrorUser";
    public static final String FILTER_CONTINGENCY_LIST = "filterContingencyList";
    public static final String FILTER_CONTINGENCY_LIST_2 = "filterContingencyList2";
    public static final String FILTER = "FILTER";
    private final Map<String, Object> specificMetadata = Map.of("id", FILTER_UUID);
    private final Map<String, Object> specificMetadata2 = Map.of("equipmentType", "LINE", "id", FILTER_UUID_2);
    private final Map<String, Object> caseSpecificMetadata = Map.of("uuid", CASE_UUID, "name", TEST_FILE, "format", "XIIDM");
    private final Map<String, Object> modificationSpecificMetadata = Map.of("id", MODIFICATION_UUID, "type", "LOAD_MODIFICATION");

    private static final UUID SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UUID = UUID.randomUUID();

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
    private NetworkModificationService networkModificationService;
    @Autowired
    private CaseService caseService;
    @Autowired
    private RemoteServicesProperties remoteServicesProperties;
    @Autowired
    private ObjectMapper mapper;
    private MockWebServer server;
    @Autowired
    private UserAdminService userAdminService;

    @Before
    public void setup() throws IOException {
        server = new MockWebServer();
        server.start();

        // Ask the server for its URL. You'll need this to make HTTP requests.
        HttpUrl baseHttpUrl = server.url("");
        String baseUrl = baseHttpUrl.toString().substring(0, baseHttpUrl.toString().length() - 1);

        directoryService.setDirectoryServerBaseUri(baseUrl);
        studyService.setStudyServerBaseUri(baseUrl);
        filterService.setFilterServerBaseUri(baseUrl);
        contingencyListService.setActionsServerBaseUri(baseUrl);
        networkModificationService.setNetworkModificationServerBaseUri(baseUrl);
        caseService.setBaseUri(baseUrl);
        userAdminService.setUserAdminServerBaseUri(baseUrl);
        remoteServicesProperties.getServices().forEach(s -> s.setBaseUri(baseUrl));

        String privateStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", USER1, 0, null));
        String listOfPrivateStudyAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", USER1, 0, null)));
        String publicStudyAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PUBLIC_STUDY_UUID, STUDY1, "STUDY", USER1, 0, null));
        String invalidElementAsString = mapper.writeValueAsString(new ElementAttributes(INVALID_ELEMENT_UUID, "invalidElementName", "INVALID", USER1, 0, null));
        String formContingencyListAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", USER1, 0, null));
        String listOfFormContingencyListAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", USER1, 0, null)));
        String filterAttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, USER1, 0, null));
        String filter2AttributesAsString = mapper.writeValueAsString(new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, USER1, 0, null));
        String listOfFilterAttributesAsString = mapper.writeValueAsString(List.of(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, USER1, 0, null)));
        String directoryAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PARENT_DIRECTORY_UUID, "directory", "DIRECTORY", USER1, 0, null));
        String caseElementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", USER1, 0L, null));
        String parametersElementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(PARAMETERS_UUID, "voltageInitParametersName", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), USER1, 0, null));
        String listElementsAttributesAsString = "[" + filterAttributesAsString + "," + privateStudyAttributesAsString + "," + formContingencyListAttributesAsString + "]";
        String caseInfosAttributesAsString = mapper.writeValueAsString(List.of(caseSpecificMetadata));
        String modificationElementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(MODIFICATION_UUID, "one modif", "MODIFICATION", USER1, 0L, null));
        String modificationInfosAttributesAsString = mapper.writeValueAsString(List.of(modificationSpecificMetadata));
        String compositeModificationIdAsString = mapper.writeValueAsString(MODIFICATION_UUID);
        String newStudyUuidAsString = mapper.writeValueAsString(STUDY_COPY_UUID);
        String newCaseUuidAsString = mapper.writeValueAsString(CASE_COPY_UUID);
        String newContingencyUuidAsString = mapper.writeValueAsString(CONTINGENCY_LIST_COPY_UUID);
        String newFilterUuidAsString = mapper.writeValueAsString(FILTER_COPY_UUID);
        String newParametersUuidAsString = mapper.writeValueAsString(PARAMETER_COPY_UUID);
        String newElementUuidAsString = mapper.writeValueAsString(ELEMENT_COPY_UUID);
        String newElementAttributesAsString = mapper.writeValueAsString(new ElementAttributes(ELEMENT_UUID, STUDY1, "STUDY", USER1, 0, null));
        String listElementsAsString = "[" + newElementAttributesAsString + "," + publicStudyAttributesAsString + "]";

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = Objects.requireNonNull(request.getPath());
                Buffer body = request.getBody();

                if (path.matches("/v1/studies/cases/" + NON_EXISTING_CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(404);
                } else if (path.matches("/v1/studies/.*/notification?type=metadata_updated") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/studies\\?duplicateFrom=" + PUBLIC_STUDY_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(newStudyUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/studies.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse().setResponseCode(409);
                    } else {
                        return new MockResponse().setResponseCode(200);
                    }
                } else if (path.matches("/v1/cases\\?duplicateFrom=" + CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(newCaseUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/cases.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse().setResponseCode(409).setBody("invalid file");
                    } else if (bodyStr.contains("filename=\"" + TEST_INCORRECT_FILE + "\"")) {  // import file with errors
                        return new MockResponse().setResponseCode(422).setBody("file with bad extension");
                    } else {
                        return new MockResponse().setResponseCode(200);
                    }
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_WITH_ERROR_UUID + "/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(500);
                } else if (path.matches("/v1/elements/" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(formContingencyListAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.equals("/v1/elements/" + CONTINGENCY_LIST_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse().setBody(formContingencyListAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + FILTER_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(filterAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.equals("/v1/elements/" + FILTER_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse().setBody(filterAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + CASE_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(caseElementAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + MODIFICATION_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(modificationElementAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + PRIVATE_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(privateStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + PUBLIC_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(publicStudyAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + PARAMETERS_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(parametersElementAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&elementTypes=FILTER") && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody("[" + filterAttributesAsString + "," + filter2AttributesAsString + "]")
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + CASE_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody("[" + caseElementAttributesAsString + "]")
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + MODIFICATION_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody("[" + modificationElementAttributesAsString + "]")
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/filters/metadata\\?ids=" + FILTER_UUID + "," + FILTER_UUID_2) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody("[" + mapper.writeValueAsString(specificMetadata) + "," + mapper.writeValueAsString(specificMetadata2) + "]")
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(listElementsAttributesAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?ids=" + ELEMENT_UUID + "," + PUBLIC_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(listElementsAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/" + ELEMENT_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?targetDirectoryUuid=" + PARENT_DIRECTORY_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setBody(newElementUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/elements\\?duplicateFrom=.*&newElementUuid=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/contingency-lists/metadata[?]ids=" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse().setBody(listOfFormContingencyListAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/.*contingency-lists\\?duplicateFrom=" + CONTINGENCY_LIST_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(newContingencyUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/script-contingency-lists\\?id=" + PARENT_DIRECTORY_WITH_ERROR_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(500);
                } else if (path.matches("/v1/script-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/form-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/identifier-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/form-contingency-lists/.*/new-script/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/new-script.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters\\?duplicateFrom=" + FILTER_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(newFilterUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/filters.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters\\?id=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*/replace-with-script") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/filters/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/script-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/form-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/identifier-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/parameters\\?duplicateFrom=" + PARAMETERS_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse().setBody(newParametersUuidAsString).setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/parameters.*")) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.matches("/v1/network-composite-modifications")) {
                    return new MockResponse().setBody(compositeModificationIdAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                } else if (path.matches("/v1/messages/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/user-message.*")) {
                    return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json; charset=utf-8");
                } else if ("GET".equals(request.getMethod())) {
                    if (path.matches("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse().setBody(invalidElementAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/elements/" + ELEMENT_UUID)) {
                        return new MockResponse().setBody(newElementAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                        return new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse().setBody(directoryAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/filters/metadata[?]ids=" + FILTER_UUID)) {
                        return new MockResponse().setBody(listOfFilterAttributesAsString.replace("elementUuid", "id")).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/cases/metadata[?]ids=" + CASE_UUID)) {
                        return new MockResponse().setBody(caseInfosAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/network-modifications/metadata[?]ids=" + MODIFICATION_UUID)) {
                        return new MockResponse().setBody(modificationInfosAttributesAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/studies/metadata[?]ids=" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setBody(listOfPrivateStudyAttributesAsString.replace("elementUuid", "id")).setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_EXCEEDED + "/profile/max-cases")) {
                        return new MockResponse().setBody("3").setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/profile/max-cases")) {
                        return new MockResponse().setBody("5").setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 + "/profile/max-cases")) {
                        return new MockResponse().setBody("5").setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_NOT_FOUND + "/profile/max-cases")) {
                        return new MockResponse().setResponseCode(404)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_UNEXPECTED_ERROR + "/profile/max-cases")) {
                        return new MockResponse().setResponseCode(500)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/.*/profile/max-cases")) {
                        return new MockResponse().setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_EXCEEDED + "/cases/count")) {
                        return new MockResponse().setBody("4").setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/cases/count")) {
                        return new MockResponse().setBody("2").setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 + "/cases/count")) {
                        return new MockResponse().setBody("1").setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/users/.*/cases/count")) {
                        return new MockResponse().setBody("0").setResponseCode(200)
                                .addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/elements/" + ELEMENT_UUID)) {
                        return new MockResponse().setBody(invalidElementAsString).setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8");
                    } else if (path.matches("/v1/cases-alert-threshold")) {
                        return new MockResponse().setBody("40").setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8");
                    }
                } else if ("DELETE".equals(request.getMethod())) {
                    if (path.matches("/v1/filters/" + FILTER_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/studies/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/contingency-lists/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/network-modifications\\?uuids=" + MODIFICATION_UUID)) {
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
                    } else if (path.matches("/v1/elements/" + PARAMETERS_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements/" + MODIFICATION_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/(cases|elements)/" + CASE_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/parameters/" + PARAMETERS_UUID)) {
                        return new MockResponse().setResponseCode(200);
                    } else if (path.matches("/v1/elements\\?ids=([^,]+,){2,}[^,]+$")) {
                        return new MockResponse().setResponseCode(200);
                    }
                    return new MockResponse().setResponseCode(404);
                } else if ("HEAD".equals(request.getMethod())) {
                    if (path.matches("/v1/elements\\?forDeletion=true&ids=" + FORBIDDEN_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(403);
                    } else if (path.matches("/v1/elements\\?forDeletion=true&ids=" + NOT_FOUND_STUDY_UUID)) {
                        return new MockResponse().setResponseCode(404);
                    } else if (path.matches("/v1/elements\\?forDeletion=true&ids=.*")) {
                        return new MockResponse().setResponseCode(200);
                    }
                }
                return new MockResponse().setResponseCode(418);
            }
        };
        server.setDispatcher(dispatcher);
    }

    @Test
    public void testCreateStudyFromExistingCase() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .param("duplicateCase", "false")
                .header("userId", "userId")
                .param("caseFormat", "XIIDM")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    public void testCreateStudyFromExistingCaseError() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .param("caseFormat", "XIIDM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateCase() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testCaseCreationError() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE_WITH_ERRORS))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    public void testCreateScriptContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                "contingencyListScriptName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Contingency list content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testCreateScriptContingencyListError() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                "contingencyListScriptName", PARENT_DIRECTORY_WITH_ERROR_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Contingency list content")
        ).andExpect(status().isInternalServerError());
    }

    @Test
    public void testCreateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                FILTER_CONTINGENCY_LIST, PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Contingency list content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testCreateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/identifier-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "identifierContingencyListName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Contingency list content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testNewScriptFromFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testReplaceFormContingencyListWithScript() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/replace-with-script",
                CONTINGENCY_LIST_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testCreateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "contingencyListScriptName", "", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Filter content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testCreateParameters() throws Exception {
        mockMvc.perform(post("/v1/explore/parameters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("Parameters content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testUpdateParameters() throws Exception {
        mockMvc.perform(put("/v1/explore/parameters/{id}?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                PARAMETERS_UUID, "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("new Parameters content")
        ).andExpect(status().isOk());
    }

    @Test
    public void testNewScriptFromFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters/{id}/new-script/{scriptName}?parentDirectoryUuid={parentDirectoryUuid}",
                FILTER_UUID, "scriptName", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testReplaceFilterWithScript() throws Exception {
        mockMvc.perform(post("/v1/explore/filters/{id}/replace-with-script",
                FILTER_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    public void deleteElement(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}",
                        elementUUid).header("userId", USER1))
                .andExpect(status().isOk());
    }

    public void deleteElements(List<UUID> elementUuids, UUID parentUuid) throws Exception {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        mockMvc.perform(delete("/v1/explore/elements/{parentUuid}?ids=" + ids, parentUuid)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    public void deleteElementInvalidType(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid)
                        .header("userId", USER1))
                .andExpect(status().is2xxSuccessful());
    }

    public void deleteElementNotAllowed(UUID elementUUid, int status) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}",
                        elementUUid).header("userId", USER1))
                .andExpect(status().is(status));
    }

    public void deleteElementsNotAllowed(List<UUID> elementUuids, UUID parentUuid, int status) throws Exception {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        mockMvc.perform(delete("/v1/explore/elements/{parentUuid}?ids=" + ids, parentUuid)
                        .header("userId", USER1))
                .andExpect(status().is(status));
    }

    @Test
    public void testDeleteElement() throws Exception {
        deleteElements(List.of(FILTER_UUID, PRIVATE_STUDY_UUID, CONTINGENCY_LIST_UUID, CASE_UUID), PARENT_DIRECTORY_UUID);
        deleteElement(FILTER_UUID);
        deleteElement(PRIVATE_STUDY_UUID);
        deleteElement(CONTINGENCY_LIST_UUID);
        deleteElementInvalidType(INVALID_ELEMENT_UUID);
        deleteElement(PARENT_DIRECTORY_UUID);
        deleteElement(CASE_UUID);
        deleteElement(PARAMETERS_UUID);
        deleteElement(MODIFICATION_UUID);
        deleteElementsNotAllowed(List.of(FORBIDDEN_STUDY_UUID), PARENT_DIRECTORY_UUID, 403);
        deleteElementsNotAllowed(List.of(NOT_FOUND_STUDY_UUID), PARENT_DIRECTORY_UUID, 404);
        deleteElementNotAllowed(FORBIDDEN_STUDY_UUID, 403);
        deleteElementNotAllowed(NOT_FOUND_STUDY_UUID, 404);
    }

    @Test
    public void testGetElementsMetadata() throws Exception {
        mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID)
                .header("userId", USER1)
        ).andExpectAll(status().isOk());

        ElementAttributes filter1 = new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, USER1, 0L, null, specificMetadata);
        ElementAttributes filter2 = new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, USER1, 0L, null, specificMetadata2);

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
    public void testDuplicateCase() throws Exception {
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    public void testDuplicateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?duplicateFrom={filterUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                FILTER_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)).andExpect(status().isOk());
    }

    @Test
    public void testDuplicateScriptContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={scriptContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                        CONTINGENCY_LIST_UUID, ContingencyListType.SCRIPT, PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    public void testDuplicateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={formContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, ContingencyListType.FORM, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testDuplicateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={identifierContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, ContingencyListType.IDENTIFIERS, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testDuplicateStudy() throws Exception {
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    public void testDuplicateParameters() throws Exception {
        mockMvc.perform(post("/v1/explore/parameters?duplicateFrom={parameterUuid}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                        PARAMETERS_UUID, ParametersType.LOADFLOW_PARAMETERS, PARENT_DIRECTORY_UUID)
                .header("userId", USER1))
            .andExpect(status().isOk());
    }

    @Test
    public void testCaseCreationErrorWithBadExtension() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_INCORRECT_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_INCORRECT_FILE, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    public void testChangeFilter() throws Exception {
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
    public void testModifyScriptContingencyList() throws Exception {
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
    public void testModifyFormContingencyList() throws Exception {
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
    public void testModifyIdentifierContingencyList() throws Exception {
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

    private void verifyFilterOrContingencyUpdateRequests(String contingencyOrFilterPath) {
        var requests = TestUtils.getRequestsWithBodyDone(2, server);
        assertTrue("elementAttributes updated", requests.stream().anyMatch(r -> r.getPath().contains(contingencyOrFilterPath)));
        assertTrue("name updated", requests.stream().anyMatch(r -> r.getPath().contains("/v1/elements/")));
    }

    @Test
    public void testGetMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + CASE_UUID)
                .header("userId", USER1))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        List<ElementAttributes> elementsMetadata = mapper.readValue(res, new TypeReference<>() {
        });
        String caseAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", USER1, 0L, null, caseSpecificMetadata));
        assertEquals(1, elementsMetadata.size());
        assertEquals(mapper.writeValueAsString(elementsMetadata.get(0)), caseAttributesAsString);
    }

    @Test
    @SneakyThrows
    public void testCreateNetworkCompositeModifications() {
        List<UUID> modificationUuids = Arrays.asList(MODIFICATION_UUID, UUID.randomUUID());
        mockMvc.perform(post("/v1/explore/composite-modifications?name={name}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                "nameModif", "descModif", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(modificationUuids))
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    public void testGetModificationMetadata() {
        final String expectedResult = mapper.writeValueAsString(new ElementAttributes(MODIFICATION_UUID, "one modif", "MODIFICATION", USER1, 0L, null, modificationSpecificMetadata));
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + MODIFICATION_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        List<ElementAttributes> elementsMetadata = mapper.readValue(response, new TypeReference<>() { });
        assertEquals(1, elementsMetadata.size());
        assertEquals(mapper.writeValueAsString(elementsMetadata.get(0)), expectedResult);
    }

    @Test
    public void testMaxCaseCreationExceeded() throws Exception {

        //test create a study with a user that already exceeded his cases limit
        MvcResult result = mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .param("duplicateCase", "false")
                        .header("userId", USER_WITH_CASE_LIMIT_EXCEEDED)
                        .param("caseFormat", "XIIDM")
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isForbidden())
                .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(MAX_ELEMENTS_EXCEEDED.name()));

        //test duplicate a study with a user that already exceeded his cases limit
        result = mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER_WITH_CASE_LIMIT_EXCEEDED)
        ).andExpect(status().isForbidden())
                .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(MAX_ELEMENTS_EXCEEDED.name()));

        //test duplicate a case with a user that already exceeded his cases limit
        result = mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER_WITH_CASE_LIMIT_EXCEEDED))
                .andExpect(status().isForbidden())
                .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(MAX_ELEMENTS_EXCEEDED.name()));

        //test create a case with a user that already exceeded his cases limit
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

            result = mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER_WITH_CASE_LIMIT_EXCEEDED)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isForbidden())
                    .andReturn();
            assertTrue(result.getResponse().getContentAsString().contains(MAX_ELEMENTS_EXCEEDED.name()));
            assertTrue(result.getResponse().getContentAsString().contains("max allowed cases : 3"));
        }
    }

    @Test
    public void testMaxCaseCreationNotExceeded() throws Exception {

        //test create a study with a user that hasn't already exceeded his cases limit
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .param("duplicateCase", "false")
                        .header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED)
                        .param("caseFormat", "XIIDM")
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk());

        //test duplicate a study with a user that hasn't already exceeded his cases limit
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                        .header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED)
                ).andExpect(status().isOk());

        //test duplicate a case with a user that hasn't already exceeded his cases limit
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED))
                .andExpect(status().isOk());

        //test create a case with a user that hasn't already exceeded his cases limit
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    @Test
    public void testMaxCaseCreationProfileNotSet() throws Exception {

        //test create a study with a user that has no profile to limit his case creation
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .param("duplicateCase", "false")
                .header("userId", USER_NOT_FOUND)
                .param("caseFormat", "XIIDM")
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());

        //test duplicate a study with a user that has no profile to limit his case creation
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER_NOT_FOUND)
        ).andExpect(status().isOk());

        //test duplicate a case with a user that has no profile to limit his case creation
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER_NOT_FOUND))
                .andExpect(status().isOk());

        //test create a case with a user that has no profile to limit his case creation
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER_NOT_FOUND)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    @Test
    public void testMaxCaseCreationWithRemoteException() throws Exception {

        //test create a study with a remote unexpected exception
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .param("duplicateCase", "false")
                .header("userId", USER_UNEXPECTED_ERROR)
                .param("caseFormat", "XIIDM")
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isBadRequest());

        //test duplicate a study with a remote unexpected exception
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER_UNEXPECTED_ERROR)
        ).andExpect(status().isBadRequest());

        //test duplicate a case with a remote unexpected exception
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER_UNEXPECTED_ERROR))
                .andExpect(status().isBadRequest());

        //test create a case with a remote unexpected exception
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, "text/xml", is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER_UNEXPECTED_ERROR)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    public void testCaseAlertThreshold() throws Exception {
        //Perform a study creation while USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 has not yet reached the defined case alert threshold, no message sent to him
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
            .param("duplicateCase", "false")
            .header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2)
            .param("caseFormat", "XIIDM")
            .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());
        var requests = TestUtils.getRequestsWithBodyDone(5, server);
        assertTrue(requests.stream().noneMatch(r -> r.getPath().contains("/messages/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 + "/user-message")));

        //Perform a study creation while USER_WITH_CASE_LIMIT_NOT_EXCEEDED has reached the defined case alert threshold, a message has been sent to him
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
            .param("duplicateCase", "false")
            .header("userId", USER_WITH_CASE_LIMIT_NOT_EXCEEDED)
            .param("caseFormat", "XIIDM")
            .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());
        requests = TestUtils.getRequestsWithBodyDone(6, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/messages/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/user-message")));
    }

    @Test
    public void testUpdateElement() throws Exception {
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(STUDY1);
        mockMvc.perform(put("/v1/explore/elements/{id}",
                ELEMENT_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(elementAttributes))
        ).andExpect(status().isOk());
    }

    @Test
    public void testMoveElementsDirectory() throws Exception {
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(STUDY1);
        mockMvc.perform(put("/v1/explore/elements?targetDirectoryUuid={parentDirectoryUuid}",
                PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(List.of(ELEMENT_UUID, PUBLIC_STUDY_UUID)))
        ).andExpect(status().isOk());
    }
}
