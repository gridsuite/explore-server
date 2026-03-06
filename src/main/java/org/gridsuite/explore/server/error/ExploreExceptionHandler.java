/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@ControllerAdvice
public class ExploreExceptionHandler
    extends AbstractBusinessExceptionHandler<ExploreException, ExploreBusinessErrorCode> {

    public ExploreExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @NonNull
    @Override
    protected ExploreBusinessErrorCode getBusinessCode(ExploreException ex) {
        return ex.getBusinessErrorCode();
    }

    @Override
    protected HttpStatus mapStatus(ExploreBusinessErrorCode errorCode) {
        return switch (errorCode) {
            case EXPLORE_MAX_ELEMENTS_EXCEEDED -> HttpStatus.FORBIDDEN;
        };
    }

    @ExceptionHandler(ExploreException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleExploreException(
        ExploreException exception, HttpServletRequest request) {
        return super.handleDomainException(exception, request);
    }
}
