package org.gridsuite.explore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.explore.server.dto.ElementAttributes;
import org.gridsuite.explore.server.dto.PermissionType;
import org.gridsuite.explore.server.services.DirectoryService;
import org.gridsuite.explore.server.services.SingleLineDiagramService;
import org.gridsuite.explore.server.utils.WireMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SingleLineDiagramTest {

    @Autowired
    protected MockMvc mockMvc;

    private WireMockServer wireMockServer;

    protected WireMockUtils wireMockUtils;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @MockBean
    private DirectoryService directoryService;

    @Captor
    private ArgumentCaptor<ElementAttributes> elementAttributesCaptor;

    private static final String BASE_URL = "/v1/explore/diagram-config";
    private static final String USER_SINGLE_LINE_DIAGRAM_SERVER_BASE_URL = "/v1/network-area-diagram/config";
    private static final String USER1 = "user1";
    private static final UUID NAD_CONFIG_UUID = UUID.randomUUID();
    private static final UUID DUPLICATE_NAD_CONFIG_UUID = UUID.randomUUID();
    private static final UUID PARENT_DIRECTORY_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockUtils = new WireMockUtils(wireMockServer);
        wireMockServer.start();
        singleLineDiagramService.setSingleLineDiagramServerBaseUri(wireMockServer.baseUrl());
    }

    @Test
    void testCreateDiagramConfig() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(USER_SINGLE_LINE_DIAGRAM_SERVER_BASE_URL))
                .willReturn(WireMock.ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(mapper.writeValueAsString(NAD_CONFIG_UUID.toString()))
                )).getId();

        when(directoryService.hasPermission(List.of(PARENT_DIRECTORY_UUID), null, USER1, PermissionType.WRITE)).thenReturn(true);

        mockMvc.perform(post(BASE_URL)
                    .param("name", "diagram config name")
                    .param("type", "DIAGRAM_CONFIG")
                    .param("description", "the config description")
                    .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("{\"depth\": 1}")
                    .header("userId", USER1))
                    .andExpect(status().isOk())
                    .andReturn();

        verify(directoryService, times(1)).createElement(elementAttributesCaptor.capture(), eq(PARENT_DIRECTORY_UUID), eq(USER1));
        verify(directoryService, times(1)).hasPermission(List.of(PARENT_DIRECTORY_UUID), null, USER1, PermissionType.WRITE);
        assertEquals(NAD_CONFIG_UUID, elementAttributesCaptor.getValue().getElementUuid());
        wireMockUtils.verifyPostRequest(stubId, USER_SINGLE_LINE_DIAGRAM_SERVER_BASE_URL, Map.of(), false);
    }

    @Test
    void testDuplicateDiagramConfig() throws Exception {
        UUID stubId = wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(USER_SINGLE_LINE_DIAGRAM_SERVER_BASE_URL))
                .willReturn(WireMock.ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(mapper.writeValueAsString(DUPLICATE_NAD_CONFIG_UUID.toString()))
                )).getId();

        when(directoryService.hasPermission(List.of(PARENT_DIRECTORY_UUID), null, USER1, PermissionType.WRITE)).thenReturn(true);
        when(directoryService.hasPermission(List.of(NAD_CONFIG_UUID), null, USER1, PermissionType.READ)).thenReturn(true);

        mockMvc.perform(post(BASE_URL)
                    .param("duplicateFrom", NAD_CONFIG_UUID.toString())
                    .param("parentDirectoryUuid", PARENT_DIRECTORY_UUID.toString())
                    .header("userId", USER1))
                    .andExpect(status().isOk())
                    .andReturn();

        verify(directoryService, times(1)).duplicateElement(NAD_CONFIG_UUID, DUPLICATE_NAD_CONFIG_UUID, PARENT_DIRECTORY_UUID, USER1);
        verify(directoryService, times(1)).hasPermission(List.of(PARENT_DIRECTORY_UUID), null, USER1, PermissionType.WRITE);
        verify(directoryService, times(1)).hasPermission(List.of(NAD_CONFIG_UUID), null, USER1, PermissionType.READ);
        wireMockUtils.verifyPostRequest(stubId, USER_SINGLE_LINE_DIAGRAM_SERVER_BASE_URL, Map.of(), false);
    }
}
