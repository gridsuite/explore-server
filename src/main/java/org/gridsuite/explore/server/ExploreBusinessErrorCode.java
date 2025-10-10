/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Business error codes emitted by the explore service.
 */
public enum ExploreBusinessErrorCode implements BusinessErrorCode {
    EXPLORE_PERMISSION_DENIED("explore.permissionDenied"),
    EXPLORE_ELEMENT_NOT_FOUND("explore.elementNotFound"),
    EXPLORE_UNKNOWN_ELEMENT_TYPE("explore.unknownElementType"),
    EXPLORE_IMPORT_CASE_FAILED("explore.importCaseFailed"),
    EXPLORE_INCORRECT_CASE_FILE("explore.incorrectCaseFile"),
    EXPLORE_MAX_ELEMENTS_EXCEEDED("explore.maxElementsExceeded"),
    EXPLORE_CASE_COUNT_UNAVAILABLE("explore.caseCountUnavailable"),
    EXPLORE_REMOTE_ERROR("explore.remoteError");

    private final String code;

    ExploreBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
