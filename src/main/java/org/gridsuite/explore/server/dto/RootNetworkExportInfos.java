package org.gridsuite.explore.server.dto;

import java.util.Map;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
public record RootNetworkExportInfos(
        String name,
        String tag,
        String caseFormat,
        CaseExportInfos caseInfos,
        Map<String, Object> importParameters
) {
}
