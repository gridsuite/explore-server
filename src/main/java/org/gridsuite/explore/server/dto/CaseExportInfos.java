package org.gridsuite.explore.server.dto;

import java.util.UUID;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
public record CaseExportInfos(
        UUID uuid,
        String name
) {
}
