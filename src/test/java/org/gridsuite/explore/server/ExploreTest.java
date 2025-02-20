/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.internal.MockWebServerExtension;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okio.Buffer;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.services.*;
import org.gridsuite.explore.server.utils.ContingencyListType;
import org.gridsuite.explore.server.utils.ParametersType;
import org.gridsuite.explore.server.utils.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.explore.server.ExploreException.Type.MAX_ELEMENTS_EXCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ExtendWith(MockWebServerExtension.class)
@AutoConfigureMockMvc
@SpringBootTest(classes = {ExploreApplication.class})
class ExploreTest {
    private static final String TEST_FILE = "testCase.xiidm";
    private static final String TEST_FILE_WITH_ERRORS = "testCase_with_errors.xiidm";

    private static final String TEST_INCORRECT_FILE = "application-default.yml";
    private static final UUID CASE_UUID = UUID.randomUUID();
    private static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID2 = UUID.randomUUID();
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
    private static final UUID MODIFICATION2_UUID = UUID.randomUUID();
    private static final UUID COMPOSITE_MODIFICATION_UUID = UUID.randomUUID();
    private static final UUID STUDY_COPY_UUID = UUID.randomUUID();
    private static final UUID CASE_COPY_UUID = UUID.randomUUID();
    private static final UUID CONTINGENCY_LIST_COPY_UUID = UUID.randomUUID();
    private static final UUID FILTER_COPY_UUID = UUID.randomUUID();
    private static final UUID PARAMETER_COPY_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_COPY_UUID = UUID.randomUUID();
    private static final String STUDY_ERROR_NAME = "studyInError";
    private static final String STUDY1 = "study1";
    private static final String USER1 = "user1";
    private static final String DIRECTORY1 = "directory1";
    private static final String USER_NOT_ALLOWED = "user not allowed";
    private static final String USER_WITH_CASE_LIMIT_EXCEEDED = "limitedUser";
    private static final String USER_WITH_CASE_LIMIT_NOT_EXCEEDED = "limitedUser2";
    private static final String USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 = "limitedUser3";
    private static final String GENERIC_STRING = "a generic string";

    private static final String USER_NOT_FOUND = "userNotFound";
    private static final String USER_UNEXPECTED_ERROR = "unexpectedErrorUser";
    private static final String FILTER_CONTINGENCY_LIST = "filterContingencyList";
    private static final String FILTER_CONTINGENCY_LIST_2 = "filterContingencyList2";
    private static final String FILTER = "FILTER";
    private final Map<String, Object> specificMetadata = Map.of("id", FILTER_UUID);
    private final Map<String, Object> specificMetadata2 = Map.of("equipmentType", "LINE", "id", FILTER_UUID_2);
    private final Map<String, Object> caseSpecificMetadata = Map.of("uuid", CASE_UUID, "name", TEST_FILE, "format", "XIIDM");
    private final Map<String, Object> modificationSpecificMetadata = Map.of("id", MODIFICATION_UUID, "type", "LOAD_MODIFICATION");
    private final List<Map<String, Object>> compositeModificationMetadata = List.of(
            Map.of(
            "uuid", MODIFICATION_UUID,
            "type", "LOAD_MODIFICATION",
            "messageType", "LOAD_MODIFICATION",
            "messageValues", "{\"equipmentId\":\"equipmentId1\"}",
            "activated", true),
            Map.of(
            "uuid", MODIFICATION2_UUID,
            "type", "SHUNT_COMPENSATOR_MODIFICATION",
            "messageType", "SHUNT_COMPENSATOR_MODIFICATION",
            "messageValues", "{\"equipmentId\":\"equipmentId2\"}",
            "activated", true)
    );

    private static final UUID SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final UUID FORBIDDEN_ELEMENT_UUID = UUID.randomUUID();

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
    @Autowired
    private UserAdminService userAdminService;

    @BeforeEach
    void setup(final MockWebServer server) throws Exception {
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
        String newDirectoryAttributesAsString = mapper.writeValueAsString(new ElementAttributes(ELEMENT_UUID, DIRECTORY1, "DIRECTORY", USER1, 0, null));
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
            @SneakyThrows(JsonProcessingException.class)
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = Objects.requireNonNull(request.getPath());
                Buffer body = request.getBody();
                if (path.matches("/v1/studies/cases/" + NON_EXISTING_CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(404);
                } else if (path.matches("/v1/studies/.*/notification?type=metadata_updated") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/studies\\?duplicateFrom=" + PUBLIC_STUDY_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newStudyUuidAsString);
                } else if (path.matches("/v1/studies.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse(409);
                    } else {
                        return new MockResponse(200);
                    }
                } else if (path.matches("/v1/cases\\?duplicateFrom=" + CASE_UUID + ".*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newCaseUuidAsString);
                } else if (path.matches("/v1/cases.*") && "POST".equals(request.getMethod())) {
                    String bodyStr = body.readUtf8();
                    if (bodyStr.contains("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")) {  // import file with errors
                        return new MockResponse.Builder().code(409).body("invalid file").build();
                    } else if (bodyStr.contains("filename=\"" + TEST_INCORRECT_FILE + "\"")) {  // import file with errors
                        return new MockResponse.Builder().code(422).body("file with bad extension").build();
                    } else {
                        return new MockResponse(200);
                    }
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), privateStudyAttributesAsString);
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newDirectoryAttributesAsString);
                } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_WITH_ERROR_UUID + "/elements\\?allowNewName=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(500);
                } else if (path.matches("/v1/elements/" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), formContingencyListAttributesAsString);
                } else if (path.equals("/v1/elements/" + CONTINGENCY_LIST_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), formContingencyListAttributesAsString);
                } else if (path.matches("/v1/elements/" + FILTER_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), filterAttributesAsString);
                } else if (path.equals("/v1/elements/" + FILTER_UUID + "/notification?type=UPDATE_DIRECTORY")) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), filterAttributesAsString);
                } else if (path.matches("/v1/elements/" + CASE_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), caseElementAttributesAsString);
                } else if (path.matches("/v1/elements/" + MODIFICATION_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), modificationElementAttributesAsString);
                } else if (path.matches("/v1/elements/" + PRIVATE_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), privateStudyAttributesAsString);
                } else if (path.matches("/v1/elements/" + PUBLIC_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), publicStudyAttributesAsString);
                } else if (path.matches("/v1/elements/" + PARAMETERS_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), parametersElementAttributesAsString);
                } else if (path.matches("/v1/elements\\?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&elementTypes=FILTER") && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "[" + filterAttributesAsString + "," + filter2AttributesAsString + "]");
                } else if (path.matches("/v1/elements\\?ids=" + CASE_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "[" + caseElementAttributesAsString + "]");
                } else if (path.matches("/v1/elements\\?ids=" + MODIFICATION_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "[" + modificationElementAttributesAsString + "]");
                } else if (path.matches("/v1/filters/metadata\\?ids=" + FILTER_UUID + "," + FILTER_UUID_2) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "[" + mapper.writeValueAsString(specificMetadata) + "," + mapper.writeValueAsString(specificMetadata2) + "]");
                } else if (path.matches("/v1/elements\\?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), listElementsAttributesAsString);
                } else if (path.matches("/v1/elements\\?ids=" + ELEMENT_UUID + "," + PUBLIC_STUDY_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), listElementsAsString);
                } else if (path.matches("/v1/elements/" + ELEMENT_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/elements\\?targetDirectoryUuid=" + PARENT_DIRECTORY_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/elements/" + FORBIDDEN_ELEMENT_UUID) && "PUT".equals(request.getMethod())) {
                    return new MockResponse(403);
                } else if (path.matches("/v1/elements/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newElementUuidAsString);
                } else if (path.matches("/v1/elements\\?duplicateFrom=.*&newElementUuid=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/contingency-lists/metadata[?]ids=" + CONTINGENCY_LIST_UUID) && "GET".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), listOfFormContingencyListAttributesAsString.replace("elementUuid", "id"));
                } else if (path.matches("/v1/.*contingency-lists\\?duplicateFrom=" + CONTINGENCY_LIST_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newContingencyUuidAsString);
                } else if (path.matches("/v1/script-contingency-lists\\?id=" + PARENT_DIRECTORY_WITH_ERROR_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse(500);
                } else if (path.matches("/v1/script-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/form-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/identifier-contingency-lists.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/form-contingency-lists/.*/new-script/.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/filters/.*/new-script.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/filters\\?duplicateFrom=" + FILTER_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newFilterUuidAsString);
                } else if (path.matches("/v1/filters.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/filters\\?id=.*") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/filters/.*/replace-with-script") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/filters/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/script-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/form-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/identifier-contingency-lists/.*") && "PUT".equals(request.getMethod())) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/parameters\\?duplicateFrom=" + PARAMETERS_UUID) && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newParametersUuidAsString);
                } else if (path.matches("/v1/parameters.*")) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/network-composite-modifications")) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), compositeModificationIdAsString);
                } else if (path.matches("/v1/messages/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/user-message.*")) {
                    return new MockResponse(200);
                } else if (path.matches("/v1/root-directories") && "POST".equals(request.getMethod())) {
                    return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                } else if ("GET".equals(request.getMethod())) {
                    if (path.matches("/v1/root-directories[?]elementTypes")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements[?]elementTypes&recursive=false")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID2 + "/path")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elementName/newNameCandidate[?]type=type")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                    } else if (path.matches("/v1/elements/indexation-infos[?]directoryUuid=directoryUuid&userInput=userInput")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), GENERIC_STRING);
                    } else if (path.matches("/v1/elements/" + ELEMENT_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), newElementAttributesAsString);
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements")) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), directoryAttributesAsString);
                    } else if (path.matches("/v1/filters/metadata[?]ids=" + FILTER_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), listOfFilterAttributesAsString.replace("elementUuid", "id"));
                    } else if (path.matches("/v1/cases/metadata[?]ids=" + CASE_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), caseInfosAttributesAsString);
                    } else if (path.matches("/v1/network-modifications/metadata[?]ids=" + MODIFICATION_UUID)) {
                        return new MockResponse(200,
                                Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                                modificationInfosAttributesAsString);
                    } else if (path.matches("/v1/network-composite-modification/" + COMPOSITE_MODIFICATION_UUID + "/network-modifications")) {
                        return new MockResponse(200,
                                Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                                mapper.writeValueAsString(compositeModificationMetadata));
                    } else if (path.matches("/v1/network-composite-modification/.*") && "PUT".equals(request.getMethod())) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/studies/metadata[?]ids=" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), listOfPrivateStudyAttributesAsString.replace("elementUuid", "id"));
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_EXCEEDED + "/profile/max-cases")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "3");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/profile/max-cases")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "5");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 + "/profile/max-cases")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "5");
                    } else if (path.matches("/v1/users/" + USER_NOT_FOUND + "/profile/max-cases")) {
                        return new MockResponse(404);
                    } else if (path.matches("/v1/users/" + USER_UNEXPECTED_ERROR + "/profile/max-cases")) {
                        return new MockResponse(500);
                    } else if (path.matches("/v1/users/.*/profile/max-cases")) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_EXCEEDED + "/cases/count")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "4");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED + "/cases/count")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "2");
                    } else if (path.matches("/v1/users/" + USER_WITH_CASE_LIMIT_NOT_EXCEEDED_2 + "/cases/count")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "1");
                    } else if (path.matches("/v1/users/.*/cases/count")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "0");
                    } else if (path.matches("/v1/elements/" + ELEMENT_UUID)) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), invalidElementAsString);
                    } else if (path.matches("/v1/cases-alert-threshold")) {
                        return new MockResponse(200, Headers.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), "40");
                    }
                } else if ("DELETE".equals(request.getMethod())) {
                    if (path.matches("/v1/filters/" + FILTER_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/studies/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/contingency-lists/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/network-modifications\\?uuids=" + MODIFICATION_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + INVALID_ELEMENT_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + PRIVATE_STUDY_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + FILTER_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + CONTINGENCY_LIST_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + PARENT_DIRECTORY_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + PARAMETERS_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements/" + MODIFICATION_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/(cases|elements)/" + CASE_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/parameters/" + PARAMETERS_UUID)) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/elements\\?ids=([^,]+,){2,}[^,]+$")) {
                        return new MockResponse(200);
                    }
                    return new MockResponse(404);
                } else if ("HEAD".equals(request.getMethod())) {
                    if (path.matches("/v1/elements\\?forDeletion=true&ids=" + FORBIDDEN_STUDY_UUID)) {
                        return new MockResponse(403);
                    } else if (path.matches("/v1/elements\\?forDeletion=true&ids=" + NOT_FOUND_STUDY_UUID)) {
                        return new MockResponse(404);
                    } else if (path.matches("/v1/elements\\?forUpdate=true&ids=" + FORBIDDEN_ELEMENT_UUID) && USER_NOT_ALLOWED.equals(request.getHeaders().get("userId"))) {
                        return new MockResponse(403);
                    } else if (path.matches("/v1/elements\\?forDeletion=true&ids=.*") || path.matches("/v1/elements\\?forUpdate=true&ids=.*")) {
                        return new MockResponse(200);
                    } else if (path.matches("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elements/elementName/types/type")) {
                        return new MockResponse(200);
                    }
                }
                return new MockResponse(418);
            }
        };
        server.setDispatcher(dispatcher);
        server.start();
    }

    @Test
    void testCreateStudyFromExistingCase() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                .param("duplicateCase", "false")
                .header("userId", "userId")
                .param("caseFormat", "XIIDM")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateStudyFromExistingCaseError() throws Exception {
        mockMvc.perform(post("/v1/explore/studies/" + STUDY1 + "/cases/" + NON_EXISTING_CASE_UUID + "?description=desc&parentDirectoryUuid=" + PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                        .param("caseFormat", "XIIDM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCase() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, MediaType.TEXT_XML_VALUE, is);

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
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE_WITH_ERRORS, MediaType.TEXT_XML_VALUE, is);

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
                .content("\"Contingency list content\"")
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateScriptContingencyListError() throws Exception {
        mockMvc.perform(post("/v1/explore/script-contingency-lists/{listName}?&parentDirectoryUuid={parentDirectoryUuid}&description={description}}",
                "contingencyListScriptName", PARENT_DIRECTORY_WITH_ERROR_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Contingency list content\"")
        ).andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                FILTER_CONTINGENCY_LIST, PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Contingency list content\"")
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/identifier-contingency-lists/{listName}?parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "identifierContingencyListName", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Contingency list content\"")
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
        mockMvc.perform(post("/v1/explore/form-contingency-lists/{id}/replace-with-script",
                CONTINGENCY_LIST_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}&description={description}",
                "contingencyListScriptName", "", PARENT_DIRECTORY_UUID, null)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Filter content\"")
        ).andExpect(status().isOk());
    }

    @Test
    void testCreateParameters() throws Exception {
        mockMvc.perform(post("/v1/explore/parameters?name={name}&type={type}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                "paramName", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), "comment", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Parameters content\"")
        ).andExpect(status().isOk());
    }

    @Test
    void testUpdateParameters() throws Exception {
        mockMvc.perform(put("/v1/explore/parameters/{id}?name={name}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                PARAMETERS_UUID, "", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"new Parameters content\"")
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

    private void deleteElement(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}",
                        elementUUid).header("userId", USER1))
                .andExpect(status().isOk());
    }

    private void deleteElements(List<UUID> elementUuids, UUID parentUuid) throws Exception {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        mockMvc.perform(delete("/v1/explore/elements/{parentUuid}?ids=" + ids, parentUuid)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    private void deleteElementInvalidType(UUID elementUUid) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}", elementUUid)
                        .header("userId", USER1))
                .andExpect(status().is2xxSuccessful());
    }

    private void deleteElementNotAllowed(UUID elementUUid, int status) throws Exception {
        mockMvc.perform(delete("/v1/explore/elements/{elementUuid}",
                        elementUUid).header("userId", USER1))
                .andExpect(status().is(status));
    }

    private void deleteElementsNotAllowed(List<UUID> elementUuids, UUID parentUuid, int status) throws Exception {
        var ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        mockMvc.perform(delete("/v1/explore/elements/{parentUuid}?ids=" + ids, parentUuid)
                        .header("userId", USER1))
                .andExpect(status().is(status));
    }

    @Test
    void testDeleteElement() throws Exception {
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
    void testGetElementsMetadata() throws Exception {
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
    void testDuplicateCase() throws Exception {
        mockMvc.perform(post("/v1/explore/cases?duplicateFrom={caseUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        CASE_UUID, PARENT_DIRECTORY_UUID).header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateFilter() throws Exception {
        mockMvc.perform(post("/v1/explore/filters?duplicateFrom={filterUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                FILTER_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)).andExpect(status().isOk());
    }

    @Test
    void testDuplicateScriptContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={scriptContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                        CONTINGENCY_LIST_UUID, ContingencyListType.SCRIPT, PARENT_DIRECTORY_UUID)
                        .header("userId", USER1))
                .andExpect(status().isOk());
    }

    @Test
    void testDuplicateFormContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={formContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, ContingencyListType.FORM, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testDuplicateIdentifierContingencyList() throws Exception {
        mockMvc.perform(post("/v1/explore/contingency-lists?duplicateFrom={identifierContingencyListUuid}&type={contingencyListsType}&parentDirectoryUuid={parentDirectoryUuid}",
                CONTINGENCY_LIST_UUID, ContingencyListType.IDENTIFIERS, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testDuplicateStudy() throws Exception {
        mockMvc.perform(post("/v1/explore/studies?duplicateFrom={studyUuid}&parentDirectoryUuid={parentDirectoryUuid}",
                        PUBLIC_STUDY_UUID, PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testDuplicateParameters() throws Exception {
        mockMvc.perform(post("/v1/explore/parameters?duplicateFrom={parameterUuid}&type={type}&parentDirectoryUuid={parentDirectoryUuid}",
                        PARAMETERS_UUID, ParametersType.LOADFLOW_PARAMETERS, PARENT_DIRECTORY_UUID)
                .header("userId", USER1))
            .andExpect(status().isOk());
    }

    @Test
    void testCaseCreationErrorWithBadExtension() throws Exception {
        try (InputStream is = new FileInputStream(ResourceUtils.getFile("classpath:" + TEST_INCORRECT_FILE))) {
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_INCORRECT_FILE, MediaType.TEXT_XML_VALUE, is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY_ERROR_NAME, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER1)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void testChangeFilter(final MockWebServer server) throws Exception {
        final String filter = "{\"type\":\"CRITERIA\",\"equipmentFilterForm\":{\"equipmentType\":\"BATTERY\",\"name\":\"test bbs\",\"countries\":[\"BS\"],\"nominalVoltage\":{\"type\":\"LESS_THAN\",\"value1\":545430,\"value2\":null},\"freeProperties\":{\"region\":[\"north\"],\"totallyFree\":[\"6555\"],\"tso\":[\"ceps\"]}}}";
        final String name = "filter name";
        final String description = "new filter description";
        mockMvc.perform(put("/v1/explore/filters/{id}",
                FILTER_UUID)
                .contentType(APPLICATION_JSON)
                .content(filter)
                .param("name", name)
                .param("description", description)
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests(server, "/v1/filters/");
    }

    @Test
    void testModifyScriptContingencyList(final MockWebServer server) throws Exception {
        final String scriptContingency = "{\"script\":\"alert(\\\"script contingency\\\")\"}";
        final String name = "script name";
        final String description = "description";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(scriptContingency)
                .param("name", name)
                .param("description", description)
                .param("contingencyListType", ContingencyListType.SCRIPT.name())
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests(server, "/v1/script-contingency-lists");
    }

    @Test
    void testModifyFormContingencyList(final MockWebServer server) throws Exception {
        final String formContingency = "{\"equipmentType\":\"LINE\",\"name\":\"contingency EN update1\",\"countries1\":[\"AL\"],\"countries2\":[],\"nominalVoltage1\":{\"type\":\"EQUALITY\",\"value1\":45340,\"value2\":null},\"nominalVoltage2\":null,\"freeProperties1\":{},\"freeProperties2\":{}}";
        final String name = "form contingency name";
        final String description = "form contingency description";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(formContingency)
                .param("name", name)
                .param("description", description)
                .param("contingencyListType", ContingencyListType.FORM.name())
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests(server, "/v1/form-contingency-lists/");
    }

    @Test
    void testModifyIdentifierContingencyList(final MockWebServer server) throws Exception {
        final String identifierContingencyList = "{\"identifierContingencyList\":{\"type\":\"identifier\",\"version\":\"1.0\",\"identifiableType\":\"LINE\",\"identifiers\":[{\"type\":\"LIST\",\"identifierList\":[{\"type\":\"ID_BASED\",\"identifier\":\"34\"},{\"type\":\"ID_BASED\",\"identifier\":\"qs\"}]}]},\"type\":\"IDENTIFIERS\"}";
        final String name = "identifier contingencyList name";
        final String description = "identifier contingencyList description";
        mockMvc.perform(put("/v1/explore/contingency-lists/{id}",
                SCRIPT_ID_BASE_FORM_CONTINGENCY_LIST_UUID)
                .contentType(APPLICATION_JSON)
                .content(identifierContingencyList)
                .param("name", name)
                .param("contingencyListType", ContingencyListType.IDENTIFIERS.name())
                .param("description", description)
                .header("userId", USER1)
        ).andExpect(status().isOk());

        verifyFilterOrContingencyUpdateRequests(server, "/v1/identifier-contingency-lists/");
    }

    private void verifyFilterOrContingencyUpdateRequests(final MockWebServer server, String contingencyOrFilterPath) {
        var requests = TestUtils.getRequestsWithBodyDone(2, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains(contingencyOrFilterPath)), "elementAttributes updated");
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/elements")), "name updated");
    }

    @Test
    void testGetMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/elements/metadata?ids=" + CASE_UUID)
                .header("userId", USER1))
                .andExpect(status().isOk())
                .andReturn();
        String res = result.getResponse().getContentAsString();
        List<ElementAttributes> elementsMetadata = mapper.readValue(res, new TypeReference<>() { });
        String caseAttributesAsString = mapper.writeValueAsString(new ElementAttributes(CASE_UUID, "case", "CASE", USER1, 0L, null, caseSpecificMetadata));
        assertEquals(1, elementsMetadata.size());
        assertEquals(mapper.writeValueAsString(elementsMetadata.get(0)), caseAttributesAsString);
    }

    @Test
    void testCreateNetworkCompositeModifications() throws Exception {
        List<UUID> modificationUuids = Arrays.asList(MODIFICATION_UUID, UUID.randomUUID());
        mockMvc.perform(post("/v1/explore/composite-modifications?name={name}&description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                "nameModif", "descModif", PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(modificationUuids))
        ).andExpect(status().isOk());
    }

    @Test
    void testModifyCompositeModifications(final MockWebServer server) throws Exception {
        final String name = "script name";
        mockMvc.perform(
                put("/v1/explore/composite-modification/{id}", COMPOSITE_MODIFICATION_UUID)
                        .contentType(APPLICATION_JSON)
                        .param("name", name)
                        .header("userId", USER1)
        ).andExpect(status().isOk());
    }

    @Test
    void testGetModificationMetadata() throws Exception {
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
    void testGetCompositeModificationContent() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/composite-modification/" + COMPOSITE_MODIFICATION_UUID + "/network-modifications")
                .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        List<Map<String, Object>> metadata = mapper.readValue(response, new TypeReference<>() { });
        assertEquals(2, metadata.size());
    }

    @Test
    void testMaxCaseCreationExceeded() throws Exception {
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
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, MediaType.TEXT_XML_VALUE, is);

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
    void testMaxCaseCreationNotExceeded() throws Exception {
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
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, MediaType.TEXT_XML_VALUE, is);

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
    void testMaxCaseCreationProfileNotSet() throws Exception {
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
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, MediaType.TEXT_XML_VALUE, is);

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
    void testMaxCaseCreationWithRemoteException() throws Exception {
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
            MockMultipartFile mockFile = new MockMultipartFile("caseFile", TEST_FILE, MediaType.TEXT_XML_VALUE, is);

            mockMvc.perform(multipart("/v1/explore/cases/{caseName}?description={description}&parentDirectoryUuid={parentDirectoryUuid}",
                            STUDY1, "description", PARENT_DIRECTORY_UUID).file(mockFile)
                            .header("userId", USER_UNEXPECTED_ERROR)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testCaseAlertThreshold(final MockWebServer server) throws Exception {
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
    void testUpdateElement() throws Exception {
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
    void testMoveElementsDirectory() throws Exception {
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(STUDY1);
        mockMvc.perform(put("/v1/explore/elements?targetDirectoryUuid={parentDirectoryUuid}",
                PARENT_DIRECTORY_UUID)
                .header("userId", USER1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(List.of(ELEMENT_UUID, PUBLIC_STUDY_UUID)))
        ).andExpect(status().isOk());
    }

    @Test
    void testUpdateElementNotOk() throws Exception {
        ElementAttributes elementAttributes = new ElementAttributes();
        elementAttributes.setElementName(STUDY1);
        mockMvc.perform(put("/v1/explore/elements/{id}",
                FORBIDDEN_ELEMENT_UUID)
                .header("userId", USER_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(elementAttributes))
        ).andExpect(status().isForbidden());
    }

    @Test
    void testGetRootDirectories(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/directories/root-directories")
                        .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("v1/root-directories")));
    }

    @Test
    void testCreateRootDirectories(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/explore/directories/root-directories")
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GENERIC_STRING)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("v1/root-directories")));
    }

    @Test
    void testGetDirectoryElements(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/directories/{directoryUuid}/elements", PARENT_DIRECTORY_UUID)
                        .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/directories/" + PARENT_DIRECTORY_UUID + "/elements?elementTypes&recursive=false")));
    }

    @Test
    void testCreateDirectory(final MockWebServer server) throws Exception {
        String newDirectoryAttributesAsString = mapper.writeValueAsString(new ElementAttributes(ELEMENT_UUID, DIRECTORY1, "DIRECTORY", USER1, 0, null));
        MvcResult result = mockMvc.perform(post("/v1/explore/directories/{directoryUuid}/directories", PARENT_DIRECTORY_UUID2)
                        .header("userId", USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newDirectoryAttributesAsString)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(newDirectoryAttributesAsString, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elements?allowNewName=false")
                && r.getBody().equals(newDirectoryAttributesAsString)));
    }

    @Test
    void testGetElementPath(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/directories/elements/{elementUuid}/path", PARENT_DIRECTORY_UUID2)
                        .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/elements/" + PARENT_DIRECTORY_UUID2 + "/path")));
    }

    @Test
    void testElementExists(final MockWebServer server) throws Exception {
        mockMvc.perform(head("/v1/explore/directories/{directoryUuid}/elements/{elementName}/types/{type}",
                        PARENT_DIRECTORY_UUID2,
                        "elementName",
                        "type")
                        .header("userId", USER1)
                ).andExpect(status().isOk());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elements/elementName/types/type")));
    }

    @Test
    void testGetElementNameCandidate(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/directories/{directoryUuid}/elementName/newNameCandidate?type=type", PARENT_DIRECTORY_UUID2)
                        .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/directories/" + PARENT_DIRECTORY_UUID2 + "/elementName/newNameCandidate")));
    }

    @Test
    void testSearchElement(final MockWebServer server) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/explore/directories/elements/indexation-infos?userInput=userInput&directoryUuid=directoryUuid")
                        .header("userId", USER1)
                ).andExpect(status().isOk())
                .andReturn();
        assertEquals(GENERIC_STRING, result.getResponse().getContentAsString());

        var requests = TestUtils.getRequestsWithBodyDone(1, server);
        assertTrue(requests.stream().anyMatch(r -> r.getPath().contains("/v1/elements/indexation-infos")));
    }
}
