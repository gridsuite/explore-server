
/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.ServletRequestBindingException;

import java.time.Instant;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    private static final String SERVICE_NAME = "explore-server";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @ExceptionHandler(ExploreException.class)
    protected ResponseEntity<ErrorResponse> handleExploreException(ExploreException exception, HttpServletRequest request) {
        HttpStatus status = resolveStatus(exception);
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, exception));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleAllException(Exception exception, HttpServletRequest request) {
        HttpStatus status = resolveStatus(exception);
        String message = exception.getMessage() != null ? exception.getMessage() : status.getReasonPhrase();
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, status.name(), message));
    }

    private ErrorResponse buildErrorResponse(HttpServletRequest request, HttpStatus status, ExploreException exception) {
        ErrorResponse remoteError = exception.getRemoteError().orElse(null);
        String errorCode = remoteError != null ? remoteError.errorCode() : exception.getType().name();
        String message = remoteError != null ? remoteError.message() : exception.getMessage();
        String service = remoteError != null ? remoteError.service() : SERVICE_NAME;
        return buildErrorResponse(request, status, service, errorCode, message);
    }

    private ErrorResponse buildErrorResponse(HttpServletRequest request, HttpStatus status, String errorCode, String message) {
        return buildErrorResponse(request, status, SERVICE_NAME, errorCode, message);
    }

    private ErrorResponse buildErrorResponse(HttpServletRequest request, HttpStatus status, String service, String errorCode, String message) {
        return new ErrorResponse(
            service,
            errorCode,
            message,
            status.value(),
            Instant.now(),
            request.getRequestURI(),
            request.getHeader(CORRELATION_ID_HEADER)
        );
    }

    private HttpStatus resolveStatus(ExploreException exception) {
        return exception.getRemoteError()
            .map(ErrorResponse::status)
            .map(HttpStatus::resolve)
            .orElseGet(() -> switch (exception.getType()) {
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case NOT_ALLOWED, MAX_ELEMENTS_EXCEEDED -> HttpStatus.FORBIDDEN;
                case REMOTE_ERROR -> HttpStatus.BAD_REQUEST;
                case INCORRECT_CASE_FILE -> HttpStatus.UNPROCESSABLE_ENTITY;
                case UNKNOWN_ELEMENT_TYPE, IMPORT_CASE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            });
    }

    private HttpStatus resolveStatus(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
            return HttpStatus.valueOf(httpStatusCodeException.getStatusCode().value());
        }
        if (exception instanceof ServletRequestBindingException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof NoResourceFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
