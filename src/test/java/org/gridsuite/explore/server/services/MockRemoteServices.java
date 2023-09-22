package org.gridsuite.explore.server.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gridsuite.explore.server.dto.AccessRightsAttributes;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.utils.ParametersType;
import org.hamcrest.Matchers;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Host mocking classes of services/clients of this application to other services in GridSuite.<br/>
 * It is based on the Spring mock of {@link org.springframework.web.client.RestTemplate RestTemplate}.
 * @see org.gridsuite.explore.server.services
 * @see org.gridsuite.explore.server.TestConfig TestConfig
 * @see MockRestServiceServer
 * @see org.springframework.boot.web.client.RestTemplateBuilder RestTemplateBuilder
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"HideUtilityClassConstructor"})
public final class MockRemoteServices {
    /*
     * some notes:
     *  - @RestClientTest can't work because each @Service use one @RestTemplateBuilder (good for testing single component)
     *  - @AutoConfigureMockRestServiceServer dont work for same reason (support only one RestTemplateBuilder)
     *  - Multiple MockServer because need to check calls per server (don't distinct hostname)
     */

    public static final String TEST_FILE = "testCase.xiidm";
    public static final String TEST_FILE_WITH_ERRORS = "testCase_with_errors.xiidm";
    public static final String TEST_INCORRECT_FILE = "application-default.yml";

    public static final UUID CASE_UUID = UUID.randomUUID();
    public static final UUID NON_EXISTING_CASE_UUID = UUID.randomUUID();
    public static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();
    public static final UUID PARENT_DIRECTORY_WITH_ERROR_UUID = UUID.randomUUID();
    public static final UUID PRIVATE_STUDY_UUID = UUID.randomUUID();
    public static final UUID PUBLIC_STUDY_UUID = UUID.randomUUID();
    public static final UUID FILTER_UUID = UUID.randomUUID();
    public static final UUID FILTER_UUID_2 = UUID.randomUUID();
    public static final UUID CONTINGENCY_LIST_UUID = UUID.randomUUID();
    public static final UUID INVALID_ELEMENT_UUID = UUID.randomUUID();
    public static final UUID PARAMETERS_UUID = UUID.randomUUID();
    public static final String STUDY1 = "study1";
    public static final String USER1 = "user1";
    public static final String FILTER_CONTINGENCY_LIST = "filterContingencyList";
    public static final String FILTER_CONTINGENCY_LIST_2 = "filterContingencyList2";
    public static final String FILTER = "FILTER";

    public static final Map<String, Object> SPECIFIC_METADATA = Map.of("id", FILTER_UUID);
    public static final Map<String, Object> SPECIFIC_METADATA_2 = Map.of("equipmentType", "LINE", "id", FILTER_UUID_2);
    public static final Map<String, Object> CASE_SPECIFIC_METADATA = Map.of(
            "uuid", CASE_UUID,
            "name", TEST_FILE,
            "format", "XIIDM"
    );

    @AllArgsConstructor
    private abstract static class AbstractMockRestService {
        protected final MockRestServiceServer mockServer;
        protected final ObjectMapper mapper;

        @SneakyThrows(JsonProcessingException.class)
        protected final String jsonify(Object value) {
            return mapper.writeValueAsString(value);
        }

        protected void expectNoMoreCall(MockRestServiceServer mockServer) {
            mockServer.expect(ExpectedCount.never(), anything());
        }

        public void expectNoMoreCall() {
            this.expectNoMoreCall(mockServer);
        }

        public void expectDeleteAnything() {
            mockServer.expect(/*ExpectedCount.manyTimes(),*/ method(HttpMethod.DELETE));
        }
    }

    /**
     * Mock an HTTP server for <b>case-server</b>
     * @see CaseService
     * @see MockRestServiceServer
     */
    @SuppressWarnings({"deprecation"})
    public static class MockCaseService extends AbstractMockRestService {
        public MockCaseService(MockRestServiceServer mockServer, ObjectMapper mapper) {
            super(mockServer, mapper);
        }

        /**
         * import file with errors<br/>
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>POST</dd>
         *         <dd>uri = {@code /cases*}</dd>
         *         <dd>body = {@code contains(filename=$TEST_FILE_WITH_ERRORS)}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 409 Conflict}</dd>
         *         <dd>body = {@code "invalid file"}</dd>
         * </dl>
         */
        public void expectPostCasesTestFileWithErrors() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/cases")))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.containsString("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")))
                    .andRespond(withStatus(HttpStatus.CONFLICT).body("invalid file")); //TODO no content-type?
        }

        /**
         * import file with errors
         * <dl>
         *     <dt>HTTP method</dt>
         *     <dt>HTTP request</dt>
         *         <dd>POST</dd>
         *         <dd>uri = {@code /cases*}</dd>
         *         <dd>body = {@code contains(filename=$TEST_INCORRECT_FILE)}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 409 Conflict}</dd>
         *         <dd>body = {@code "file with bad extension"}</dd>
         * </dl>
         */
        public void expectPostCasesTestIncorrectFile() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/cases")))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.containsString("filename=\"" + TEST_INCORRECT_FILE + "\"")))
                    .andRespond(withStatus(HttpStatus.CONFLICT).body("file with bad extension")); //TODO no content-type?
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>POST</dd>
         *         <dd>uri = {@code /cases*}</dd>
         *         <dd>body = {@code not_contains(filename=$TEST_FILE_WITH_ERRORS) & not_contains(filename=$TEST_INCORRECT_FILE)}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostCasesNoIncorrectOrErrorFile() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/cases")))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.not(Matchers.allOf(
                            Matchers.containsString("filename=\"" + TEST_FILE_WITH_ERRORS + "\""),
                            Matchers.containsString("filename=\"" + TEST_INCORRECT_FILE + "\"")
                    ))))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>GET</dd>
         *         <dd>uri = {@code /cases/metadata?ids=$CASE_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [$CASE_SPECIFIC_METADATA]}</dd>
         * </dl>
         */
        public void expectGetCasesMetadataIdsCaseUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/cases/metadata?ids=" + CASE_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(List.of(CASE_SPECIFIC_METADATA))));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /cases/$CASE_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteCasesCaseUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/cases/" + CASE_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }

    /**
     * Mock an HTTP server for <b>actions-server</b>
     * @see ContingencyListService
     * @see MockRestServiceServer
     */
    @SuppressWarnings({"deprecation"})
    public static class MockContingencyListService extends AbstractMockRestService {
        public MockContingencyListService(MockRestServiceServer mockServer, ObjectMapper mapper) {
            super(mockServer, mapper);
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /contingency-lists/metadata?ids=$CONTINGENCY_LIST_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetContingencyListsMetadataIdsContingencyListUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/contingency-lists/metadata?ids=" + CONTINGENCY_LIST_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(List.of(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0, null))).replace("elementUuid", "id")));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /script-contingency-lists?id=$PARENT_DIRECTORY_WITH_ERROR_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostScriptContingencyListsIdParentDirectoryWithErrorUuid() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo("/script-contingency-lists?id=" + PARENT_DIRECTORY_WITH_ERROR_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withServerError());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /script-contingency-lists*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostScriptContingencyLists() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/script-contingency-lists")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /form-contingency-lists*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostFormContingencyLists() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/form-contingency-lists")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /identifier-contingency-lists*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostIdentifierContingencyLists() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/identifier-contingency-lists")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /form-contingency-lists/*\/new-script/*}</dd>
         *     <dt> HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostFormContingencyListsNewScript() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.matchesPattern("^/form-contingency-lists/.+/new-script[/?].*")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /script-contingency-lists/*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPutScriptContingencyLists() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.startsWith("/script-contingency-lists/")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /form-contingency-lists/*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPutFormContingencyLists() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.startsWith("/form-contingency-lists/")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /identifier-contingency-lists/*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPutIdentifierContingencyLists() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.startsWith("/identifier-contingency-lists/")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /contingency-lists/$CONTINGENCY_LIST_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteContingencyListsContingencyListUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/contingency-lists/" + CONTINGENCY_LIST_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }

    /**
     * Mock an HTTP server for <b>directory-server</b>
     * @see DirectoryService
     * @see MockRestServiceServer
     */
    @SuppressWarnings({"deprecation"})
    public static class MockDirectoryService extends AbstractMockRestService {
        public MockDirectoryService(MockRestServiceServer mockServer, ObjectMapper mapper) {
            super(mockServer, mapper);
        }

        private final String privateStudyAttributesAsString = jsonify(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0, null));
        private final String formContingencyListAttributesAsString = jsonify(new ElementAttributes(CONTINGENCY_LIST_UUID, FILTER_CONTINGENCY_LIST, "CONTINGENCY_LIST", new AccessRightsAttributes(true), USER1, 0, null));
        private final String filterAttributesAsString = jsonify(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0, null));
        private final String caseElementAttributesAsString = jsonify(new ElementAttributes(CASE_UUID, "case", "CASE", new AccessRightsAttributes(true), USER1, 0L, null));

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /directories/$PARENT_DIRECTORY_UUID/elements}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $privateStudyAttributesAsString}</dd>
         * </dl>
         */
        public void expectPostDirectoriesParentDirectoryUuidElements() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo("/directories/" + PARENT_DIRECTORY_UUID + "/elements"))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(privateStudyAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /directories/$PARENT_DIRECTORY_WITH_ERROR_UUID/elements}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 500 Internal Server Error}</dd>
         * </dl>
         */
        public void expectPostDirectoriesParentDirectoryWithErrorUuidElements() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo("/directories/" + PARENT_DIRECTORY_WITH_ERROR_UUID + "/elements"))
                    .andExpect(header("userId", USER1))
                    .andRespond(withServerError());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$CONTINGENCY_LIST_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $formContingencyListAttributesAsString}</dd>
         * </dl>
         */
        public void expectGetElementsContingencyListUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + CONTINGENCY_LIST_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(formContingencyListAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>uri = {@code /elements/$CONTINGENCY_LIST_UUID/notification?type=UPDATE_DIRECTORY}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $formContingencyListAttributesAsString}</dd>
         * </dl>
         */
        public void expectHttpElementsContingencyListUuidNotificationTypeUpdateDirectory() {
            //TODO missing method
            mockServer.expect(requestTo("/elements/" + CONTINGENCY_LIST_UUID + "/notification?type=UPDATE_DIRECTORY"))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(formContingencyListAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$PARAMETERS_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetElementsParametersUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + PARAMETERS_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(new ElementAttributes(PARAMETERS_UUID, "voltageInitParametersName", ParametersType.VOLTAGE_INIT_PARAMETERS.name(), new AccessRightsAttributes(true), USER1, 0, null))));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$FILTER_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $filterAttributesAsString}</dd>
         * </dl>
         */
        public void expectGetElementsFilterUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + FILTER_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(filterAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>uri = {@code /elements/$FILTER_UUID/notification?type=UPDATE_DIRECTORY}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $filterAttributesAsString}</dd>
         * </dl>
         */
        public void expectHttpElementsFilterUuidNotificationTypeUpdateDirectory() {
            //TODO missing method
            mockServer.expect(requestTo("/elements/" + FILTER_UUID + "/notification?type=UPDATE_DIRECTORY"))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(filterAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$CASE_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $caseElementAttributesAsString}</dd>
         * </dl>
         */
        public void expectGetElementsCaseUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + CASE_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(caseElementAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$PRIVATE_STUDY_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code $privateStudyAttributesAsString}</dd>
         * </dl>
         */
        public void expectGetElementsPrivateStudyUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + PRIVATE_STUDY_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(privateStudyAttributesAsString));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$PUBLIC_STUDY_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetElementsPublicStudyUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + PUBLIC_STUDY_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(new ElementAttributes(PUBLIC_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(false), USER1, 0, null))));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements?ids=$FILTER_UUID,$FILTER_UUID_2&elementTypes=FILTER}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [$filterAttributesAsString,ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetElementsIdsFilterUuidFilterUuid2ElementtypesFilter() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements?ids=" + FILTER_UUID + "," + FILTER_UUID_2 + "&elementTypes=FILTER"))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body("[" + filterAttributesAsString + "," + jsonify(new ElementAttributes(FILTER_UUID_2, FILTER_CONTINGENCY_LIST_2, FILTER, new AccessRightsAttributes(true), USER1, 0, null)) + "]"));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements?ids=$CASE_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [$caseElementAttributesAsString]}</dd>
         * </dl>
         */
        public void expectGetElementsIdsCaseUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements?ids=" + CASE_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body("[" + caseElementAttributesAsString + "]"));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements?ids=$FILTER_UUID,$PRIVATE_STUDY_UUID,$CONTINGENCY_LIST_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [$filterAttributesAsString,$privateStudyAttributesAsString,$formContingencyListAttributesAsString]}</dd>
         * </dl>
         */
        public void expectGetElementsIdsFilterUuidPrivateStudyUuidContingencyListUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements?ids=" + FILTER_UUID + "," + PRIVATE_STUDY_UUID + "," + CONTINGENCY_LIST_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body("[" + filterAttributesAsString + "," + privateStudyAttributesAsString + "," + formContingencyListAttributesAsString + "]"));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /elements/}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         * </dl>
         */
        public void expectPutElements() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.startsWith("/elements/")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8)); //TODO missing body content
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$INVALID_ELEMENT_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code {...ElementAttributes(...)}}</dd>
         * </dl>
         */
        public void expectGetElementsInvalidElementUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + INVALID_ELEMENT_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(new ElementAttributes(INVALID_ELEMENT_UUID, "invalidElementName", "INVALID", new AccessRightsAttributes(false), USER1, 0, null))));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELGETETE}</dd>
         *         <dd>uri = {@code /directories/$PARENT_DIRECTORY_UUID/elements}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         * </dl>
         */
        public void expectGetDirectoriesParentDirectoryUuidElements() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/directories/" + PARENT_DIRECTORY_UUID + "/elements"))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8)); //TODO missing body content
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /elements/$PARENT_DIRECTORY_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code {...ElementAttributes(...)}}</dd>
         * </dl>
         */
        public void expectGetElementsParentDirectoryUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/elements/" + PARENT_DIRECTORY_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(new ElementAttributes(PARENT_DIRECTORY_UUID, "directory", "DIRECTORY", new AccessRightsAttributes(true), USER1, 0, null))));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$INVALID_ELEMENT_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsInvalidElementUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + INVALID_ELEMENT_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$PRIVATE_STUDY_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsPrivateStudyUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + PRIVATE_STUDY_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$FILTER_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsFilterUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + FILTER_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$CONTINGENCY_LIST_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsContingencyListUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + CONTINGENCY_LIST_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$PARENT_DIRECTORY_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsParentDirectoryUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + PARENT_DIRECTORY_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$CASE_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsCaseUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + CASE_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /elements/$PARAMETERS_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteElementsParametersUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/elements/" + PARAMETERS_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }

    /**
     * Mock an HTTP server for <b>filter-server</b>
     * @see FilterService
     * @see MockRestServiceServer
     */
    @SuppressWarnings({"deprecation"})
    public static class MockFilterService extends AbstractMockRestService {
        public MockFilterService(MockRestServiceServer mockServer, ObjectMapper mapper) {
            super(mockServer, mapper);
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /filters/metadata?ids=$FILTER_UUID,$FILTER_UUID_2}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [$SPECIFIC_METADATA),$SPECIFIC_METADATA_2]}</dd>
         * </dl>
         */
        public void expectGetFiltersMetadataIdsFilterUuidFilterUuid2() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/filters/metadata?ids=" + FILTER_UUID + "," + FILTER_UUID_2))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body("[" + jsonify(SPECIFIC_METADATA) + "," + jsonify(SPECIFIC_METADATA_2) + "]"));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /filters/*\/new-script*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostFiltersNewScript() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.matchesPattern("^/filters/.+/new-script.*")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /filters*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostFilters() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/filters")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /filters?id=*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostFiltersIdAny() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/filters?id=")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /filters/*\/replace-with-script}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPutFiltersReplaceWithScript() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.matchesPattern("^/filters/.+/replace-with-script")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code PUT}</dd>
         *         <dd>uri = {@code /filters/*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPutFilters() {
            mockServer.expect(method(HttpMethod.PUT))
                    .andExpect(requestTo(Matchers.startsWith("/filters/")))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /filters/metadata?ids=$FILTER_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetFiltersMetadataIdsFilterUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/filters/metadata?ids=" + FILTER_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(List.of(new ElementAttributes(FILTER_UUID, FILTER_CONTINGENCY_LIST, FILTER, new AccessRightsAttributes(true), USER1, 0, null))).replace("elementUuid", "id")));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /filters/$FILTER_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteFiltersFilterUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/filters/" + FILTER_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }

    /**
     * Mock an HTTP server for <b>study-server</b>
     * @see StudyService
     * @see MockRestServiceServer
     */
    @SuppressWarnings({"deprecation"})
    public static class MockStudyService extends AbstractMockRestService {
        public MockStudyService(MockRestServiceServer mockServer, ObjectMapper mapper) {
            super(mockServer, mapper);
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /studies/cases/$NON_EXISTING_CASE_UUID(/*)?}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 404 Not Found}</dd>
         * </dl>
         */
        public void expectPostStudiesCasesNonExistingCaseUuid() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/studies/cases/" + NON_EXISTING_CASE_UUID)))
                    .andExpect(header("userId", USER1))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));
        }

        /**
         * import file with errors
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /studies*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *         <dd>body = {@code contains(filename=$TEST_FILE_WITH_ERRORS)}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 409 Conflict}</dd>
         * </dl>
         */
        public void expectPostStudiesTestFileWithErrors() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/studies")))
                    .andExpect(header("userId", USER1))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.containsString("filename=\"" + TEST_FILE_WITH_ERRORS + "\"")))
                    .andRespond(withStatus(HttpStatus.CONFLICT));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code POST}</dd>
         *         <dd>uri = {@code /studies*}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *         <dd>body = {@code not_contains(filename=$TEST_FILE_WITH_ERRORS)}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectPostStudies() {
            mockServer.expect(method(HttpMethod.POST))
                    .andExpect(requestTo(Matchers.startsWith("/studies")))
                    .andExpect(header("userId", USER1))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.not(Matchers.containsString("filename=\"" + TEST_FILE_WITH_ERRORS + "\""))))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code GET}</dd>
         *         <dd>uri = {@code /studies/metadata?ids=$PRIVATE_STUDY_UUID}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         *         <dd>header: content-type = {@code application/json;charset=UTF-8}</dd>
         *         <dd>body = {@code [ElementAttributes(...)]}</dd>
         * </dl>
         */
        public void expectGetStudiesMetadataIdsPrivateStudyUuid() {
            mockServer.expect(method(HttpMethod.GET))
                    .andExpect(requestTo("/studies/metadata?ids=" + PRIVATE_STUDY_UUID))
                    .andRespond(withSuccess().contentType(APPLICATION_JSON_UTF8).body(jsonify(List.of(new ElementAttributes(PRIVATE_STUDY_UUID, STUDY1, "STUDY", new AccessRightsAttributes(true), USER1, 0, null))).replace("elementUuid", "id")));
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code /studies/$PRIVATE_STUDY_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteStudiesPrivateStudyUuid() {
            mockServer.expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("/studies/" + PRIVATE_STUDY_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }

    public static class MockParametersService extends AbstractMockRestService {
        private final Map<ParametersType, MockRestServiceServer> mockServers;

        public MockParametersService(Map<ParametersType, MockRestServiceServer> mockServers, ObjectMapper mapper) {
            super(null, mapper);
            this.mockServers = mockServers;
        }

        @Override
        public void expectNoMoreCall() {
            mockServers.values().forEach(this::expectNoMoreCall);
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>uri = {@code http://voltage_init_parameters/parameters/*}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectHttpVoltageInitAny() {
            //TODO missing method
            mockServers.get(ParametersType.VOLTAGE_INIT_PARAMETERS)
                    .expect(requestTo(Matchers.startsWith("http://voltage_init_parameters/v1/parameters")))
                    .andRespond(withSuccess());
        }

        /**
         * <dl>
         *     <dt>HTTP request</dt>
         *         <dd>method = {@code DELETE}</dd>
         *         <dd>uri = {@code http://voltage_init_parameters/parameters/$PARAMETERS_UUID}</dd>
         *         <dd>header: userId = {@code $USER1}</dd>
         *     <dt>HTTP response</dt>
         *         <dd>status = {@code 200 OK}</dd>
         * </dl>
         */
        public void expectDeleteVoltageInitParametersParametersUuid() {
            mockServers.get(ParametersType.VOLTAGE_INIT_PARAMETERS)
                    .expect(method(HttpMethod.DELETE))
                    .andExpect(requestTo("http://voltage_init_parameters/v1/parameters/" + PARAMETERS_UUID))
                    .andExpect(header("userId", USER1))
                    .andRespond(withSuccess());
        }
    }
}
