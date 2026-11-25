/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.DirectoryElementType;
import org.gridsuite.explore.server.services.cases.CaseService;
import org.gridsuite.explore.server.services.contingency.ContingencyListService;
import org.gridsuite.explore.server.services.diagram.SingleLineDiagramService;
import org.gridsuite.explore.server.services.directory.DirectoryRestService;
import org.gridsuite.explore.server.services.directory.DirectoryService;
import org.gridsuite.explore.server.services.dynamic.DynamicMappingService;
import org.gridsuite.explore.server.services.filter.FilterService;
import org.gridsuite.explore.server.services.modification.NetworkModificationService;
import org.gridsuite.explore.server.services.monitor.MonitorService;
import org.gridsuite.explore.server.services.parameters.ParametersService;
import org.gridsuite.explore.server.services.spreadsheet.SpreadsheetConfigCollectionService;
import org.gridsuite.explore.server.services.spreadsheet.SpreadsheetConfigService;
import org.gridsuite.explore.server.services.study.StudyService;
import org.gridsuite.explore.server.services.workspace.WorkspaceService;
import org.gridsuite.explore.server.utils.ParametersType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
class DirectoryElementTypesRegistryTest {

    private static final List<Class<? extends AbstractElementsService>> SERVICE_CLASSES = List.of(
            DirectoryService.class,
            StudyService.class,
            CaseService.class,
            ContingencyListService.class,
            FilterService.class,
            NetworkModificationService.class,
            SpreadsheetConfigService.class,
            SpreadsheetConfigCollectionService.class,
            SingleLineDiagramService.class,
            WorkspaceService.class,
            MonitorService.class,
            DynamicMappingService.class,
            ParametersService.class
    );

    private static List<AbstractElementsService> mockedServices() {
        return SERVICE_CLASSES.stream().<AbstractElementsService>map(c -> mock(c)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Test
    void parametersServiceHandlesExactlyTheParametersTypes() {
        Set<String> declaredTypes = Arrays.stream(ParametersService.class.getAnnotation(DirectoryElementTypes.class).value())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> parametersTypes = Arrays.stream(ParametersType.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(parametersTypes, declaredTypes);
    }

    @Test
    void registryResolvesEveryTypeToItsDeclaringServiceEvenWhenMocked() {
        List<AbstractElementsService> services = mockedServices();

        Map<DirectoryElementType, AbstractElementsService> registry = ExploreService.buildGenericServicesRegistry(services);

        assertEquals(Set.of(DirectoryElementType.values()), registry.keySet());
        assertInstanceOf(FilterService.class, registry.get(DirectoryElementType.FILTER));
        assertInstanceOf(DirectoryService.class, registry.get(DirectoryElementType.DIRECTORY));
        Arrays.stream(ParametersType.values()).forEach(parametersType ->
                assertInstanceOf(ParametersService.class, registry.get(DirectoryElementType.valueOf(parametersType.name()))));
    }

    @Test
    void registryFailsWhenATypeIsHandledByNoService() {
        List<AbstractElementsService> services = mockedServices();
        services.removeIf(WorkspaceService.class::isInstance);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ExploreService.buildGenericServicesRegistry(services));
        assertTrue(exception.getMessage().contains(DirectoryElementType.WORKSPACE.name()));
    }

    @Test
    void registryFailsWhenTwoServicesDeclareTheSameType() {
        List<AbstractElementsService> services = Stream.concat(mockedServices().stream(), Stream.of(mock(FilterService.class))).toList();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ExploreService.buildGenericServicesRegistry(services));
        assertTrue(exception.getMessage().contains(DirectoryElementType.FILTER.name()));
    }

    @Test
    void registryFailsWhenAServiceIsNotAnnotated() {
        AbstractElementsService notAnnotatedService = new AbstractElementsService(mock(DirectoryRestService.class)) {
            @Override
            public void delete(UUID id) {
                // not needed for this test
            }
        };
        List<AbstractElementsService> services = Stream.concat(mockedServices().stream(), Stream.of(notAnnotatedService)).toList();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ExploreService.buildGenericServicesRegistry(services));
        assertTrue(exception.getMessage().contains(DirectoryElementTypes.class.getSimpleName()));
    }
}
