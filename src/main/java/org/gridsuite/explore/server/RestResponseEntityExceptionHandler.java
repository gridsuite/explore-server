/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.gridsuite.explore.server.ExploreException.Type.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ExploreException.class})
    protected ResponseEntity<Object> handleException(RuntimeException exception) {
        ExploreException exploreException = (ExploreException) exception;
        switch (exploreException.getType()) {
            case STUDY_NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(STUDY_NOT_FOUND);
            case FILTER_NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(FILTER_NOT_FOUND);
            case CONTINGENCY_LIST_NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CONTINGENCY_LIST_NOT_FOUND);
            case NOT_ALLOWED:
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(NOT_ALLOWED);
            case REMOTE_ERROR:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
            case UNKNOWN_ELEMENT_TYPE:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UNKNOWN_ELEMENT_TYPE);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
