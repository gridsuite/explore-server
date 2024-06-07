
/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import static org.gridsuite.explore.server.ExploreException.Type.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = {ExploreException.class})
    protected ResponseEntity<Object> handleExploreException(ExploreException exception) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(exception.getMessage(), exception);
        }
        ExploreException exploreException = exception;
        switch (exploreException.getType()) {
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            case NOT_ALLOWED:
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(NOT_ALLOWED);
            case REMOTE_ERROR:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
            case INCORRECT_CASE_FILE:
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(exception.getMessage());
            case UNKNOWN_ELEMENT_TYPE:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UNKNOWN_ELEMENT_TYPE);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleAllException(Exception exception) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(exception.getMessage(), exception);
        }
        if (exception instanceof HttpStatusCodeException) {
            return ResponseEntity.status(((HttpStatusCodeException) exception).getStatusCode()).body(exception.getMessage());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }
}
