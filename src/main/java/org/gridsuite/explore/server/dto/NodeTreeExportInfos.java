package org.gridsuite.explore.server.dto;

import java.util.List;
import java.util.UUID;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
public record NodeTreeExportInfos(
        UUID id,
        String name,
        String type,
        UUID modificationGroupUuid,
        String buildStatus,
        List<NodeTreeExportInfos> children
) {
}
