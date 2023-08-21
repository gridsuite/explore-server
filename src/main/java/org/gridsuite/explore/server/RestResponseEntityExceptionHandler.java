/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import static org.gridsuite.explore.server.ExploreException.Type.NOT_ALLOWED;
import static org.gridsuite.explore.server.ExploreException.Type.UNKNOWN_ELEMENT_TYPE;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Slf4j
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ExploreException.class})
    protected ResponseEntity<Object> handleExploreException(ExploreException exception) {
        log.error("Error during explore", exception);
        switch (exception.getType()) {
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
        if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
            return ResponseEntity.status(httpStatusCodeException.getStatusCode()).body(exception.getMessage());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }
}
