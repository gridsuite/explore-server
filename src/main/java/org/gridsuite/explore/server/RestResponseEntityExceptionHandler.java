/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.AbstractBaseRestExceptionHandler;
import com.powsybl.ws.commons.error.ServerNameProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler
    extends AbstractBaseRestExceptionHandler<ExploreException, ExploreBusinessErrorCode> {

    public RestResponseEntityExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @NotNull
    @Override
    protected ExploreBusinessErrorCode getBusinessCode(ExploreException ex) {
        return ex.getBusinessErrorCode();
    }

    @Override
    protected HttpStatus mapStatus(ExploreBusinessErrorCode errorCode) {
        return switch (errorCode) {
            case EXPLORE_ELEMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EXPLORE_PERMISSION_DENIED, EXPLORE_MAX_ELEMENTS_EXCEEDED -> HttpStatus.FORBIDDEN;
            case EXPLORE_INCORRECT_CASE_FILE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case EXPLORE_CASE_COUNT_UNAVAILABLE -> HttpStatus.BAD_REQUEST;
            case EXPLORE_UNKNOWN_ELEMENT_TYPE, EXPLORE_IMPORT_CASE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
