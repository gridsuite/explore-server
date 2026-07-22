/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.services.CaseService;
import org.gridsuite.explore.server.services.ContingencyListService;
import org.gridsuite.explore.server.services.FilterService;
import org.gridsuite.explore.server.services.MonitorService;
import org.gridsuite.explore.server.services.NetworkConversionService;
import org.gridsuite.explore.server.services.SpreadsheetConfigCollectionService;
import org.gridsuite.explore.server.services.SpreadsheetConfigService;
import org.gridsuite.explore.server.services.StudyService;
import org.gridsuite.explore.server.services.UserAdminService;
import org.gridsuite.explore.server.services.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExplorePassThroughControllersTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();
    private static final String JSON = "{\"name\":\"value\"}";
    private static final String USER_ID = "userId";

    @Mock
    private ContingencyListService contingencyListService;
    @Mock
    private CaseService caseService;
    @Mock
    private FilterService filterService;
    @Mock
    private MonitorService monitorService;
    @Mock
    private NetworkConversionService networkConversionService;
    @Mock
    private SpreadsheetConfigService spreadsheetConfigService;
    @Mock
    private SpreadsheetConfigCollectionService spreadsheetConfigCollectionService;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private StudyService studyService;
    @Mock
    private UserAdminService userAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new ActionsController(contingencyListService),
            new CaseController(caseService),
            new FilterController(filterService),
            new MonitorController(monitorService),
            new NetworkConversionController(networkConversionService),
            new StudyConfigController(spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService),
            new StudyController(studyService),
            new UserAdminController(userAdminService)
        ).build();
    }

    @Test
    void getIdentifierContingencyList() throws Exception {
        when(contingencyListService.getIdentifierContingencyList(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/identifier-contingency-lists/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(contingencyListService).getIdentifierContingencyList(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getFilterBasedContingencyList() throws Exception {
        when(contingencyListService.getFilterBasedContingencyList(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/filters-contingency-lists/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(contingencyListService).getFilterBasedContingencyList(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void importCaseForwardsMultipartFileAndExpirationFlag() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "case.xiidm", MediaType.APPLICATION_XML_VALUE, "case".getBytes());
        when(caseService.importCaseWithoutDirectoryElementCreation(file, true)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ID));

        mockMvc.perform(multipart("/v1/explore/cases")
                .file(file)
                .param("withExpiration", "true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"" + ID + "\""));

        verify(caseService).importCaseWithoutDirectoryElementCreation(file, true);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void deleteCaseForwardsCaseUuidAndReturnsNoContent() throws Exception {
        when(caseService.deleteCase(ID)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/v1/explore/cases/{caseUuid}", ID))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(HttpHeaders.CONTENT_TYPE))
            .andExpect(content().string(""));

        verify(caseService).deleteCase(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void downloadCaseForwardsCaseUuidAndReturnsFileContent() throws Exception {
        when(caseService.downloadCase(ID)).thenReturn(ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=case.xiidm")
            .body(new ByteArrayResource("case".getBytes())));

        mockMvc.perform(get("/v1/explore/cases/{caseUuid}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=case.xiidm"))
            .andExpect(content().bytes("case".getBytes()));

        verify(caseService).downloadCase(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getCaseBaseNameForwardsCaseNameAndReturnsText() throws Exception {
        when(caseService.getBaseName("case.xiidm")).thenReturn(ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("case"));

        mockMvc.perform(get("/v1/explore/cases/caseBaseName")
                .param("caseName", "case.xiidm"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_PLAIN))
            .andExpect(content().string("case"));

        verify(caseService).getBaseName("case.xiidm");
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getFilterForwardsIdAndReturnsJson() throws Exception {
        when(filterService.getFilter(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/filters/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(filterService).getFilter(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getProcessConfigForwardsIdAndReturnsJson() throws Exception {
        when(monitorService.getProcessConfig(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/process-configs/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(monitorService).getProcessConfig(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getCaseImportParametersForwardsCaseUuidAndReturnsJson() throws Exception {
        when(networkConversionService.getCaseImportParameters(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/cases/{caseUuid}/import-parameters", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(networkConversionService).getCaseImportParameters(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void convertCaseForwardsPathQueryHeaderAndBody() throws Exception {
        when(networkConversionService.convertCase(ID, "CGMES", "network.zip", JSON, USER_ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(SECOND_ID));

        mockMvc.perform(post("/v1/explore/cases/{caseUuid}/convert/{format}", ID, "CGMES")
                .param("fileName", "network.zip")
                .header("userId", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"" + SECOND_ID + "\""));

        verify(networkConversionService).convertCase(ID, "CGMES", "network.zip", JSON, USER_ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void downloadFileForwardsExportUuidAndReturnsFileContent() throws Exception {
        when(networkConversionService.downloadFile(ID)).thenReturn(ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=network.zip")
            .body(new ByteArrayResource("network".getBytes())));

        mockMvc.perform(get("/v1/explore/download-file/{exportUuid}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=network.zip"))
            .andExpect(content().bytes("network".getBytes()));

        verify(networkConversionService).downloadFile(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getExportFormatsReturnsJsonFromNetworkConversionService() throws Exception {
        when(networkConversionService.getExportFormats()).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/export/formats"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(networkConversionService).getExportFormats();
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getSpreadsheetConfigForwardsIdAndReturnsJson() throws Exception {
        when(spreadsheetConfigService.getSpreadsheetConfig(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/spreadsheet-configs/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(spreadsheetConfigService).getSpreadsheetConfig(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getSpreadsheetConfigCollectionForwardsIdAndReturnsJson() throws Exception {
        when(spreadsheetConfigCollectionService.getSpreadsheetConfigCollection(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/spreadsheet-config-collections/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(spreadsheetConfigCollectionService).getSpreadsheetConfigCollection(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getWorkspaceForwardsIdAndReturnsJson() throws Exception {
        when(workspaceService.getWorkspace(ID)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/workspaces/{id}", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(workspaceService).getWorkspace(ID);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void evaluateFiltersOnFirstRootNetworkForwardsStudyUuidAndBody() throws Exception {
        when(studyService.evaluateFiltersOnFirstRootNetwork(ID, JSON)).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(post("/v1/explore/studies/{studyUuid}/filters/elements", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(studyService).evaluateFiltersOnFirstRootNetwork(ID, JSON);
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getServersInfosForwardsViewQueryParamAndReturnsJson() throws Exception {
        when(studyService.getServersInfos("short")).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/servers/about")
                .param("view", "short"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(JSON));

        verify(studyService).getServersInfos("short");
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }

    @Test
    void getGroupsReturnsJsonFromUserAdminService() throws Exception {
        when(userAdminService.getGroups()).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON));

        mockMvc.perform(get("/v1/explore/groups"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().json(JSON));

        verify(userAdminService).getGroups();
        verifyNoMoreInteractions(contingencyListService, caseService, filterService, monitorService, networkConversionService,
            spreadsheetConfigService, spreadsheetConfigCollectionService, workspaceService, studyService, userAdminService);
    }
}
