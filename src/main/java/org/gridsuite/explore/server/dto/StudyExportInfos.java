package org.gridsuite.explore.server.dto;

import java.util.List;
import java.util.UUID;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
public record StudyExportInfos(
        UUID studyUuid,
        List<RootNetworkExportInfos> rootNetworks,
        NodeTreeExportInfos nodeTree
) {
}
