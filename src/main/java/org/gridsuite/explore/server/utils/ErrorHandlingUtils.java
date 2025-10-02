/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.utils;

import com.powsybl.ws.commons.error.ErrorResponse;
import com.powsybl.ws.commons.error.ErrorResponseParser;
import org.gridsuite.explore.server.ExploreException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com
 */
public final class ErrorHandlingUtils {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String DIRECTORY_SERVICE_NAME = "directory-server";

    private ErrorHandlingUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ExploreException wrapRemoteError(String response, HttpStatusCode statusCode) {
        if (!"".equals(response)) {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, response);
        } else {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, "{\"message\": " + statusCode + "\"}");
        }
    }

    public static ExploreException mapRemoteException(HttpStatusCodeException exception) {
        ErrorResponse error = ErrorResponseParser.parse(exception.getResponseBodyAsByteArray())
            .orElseGet(() -> fallbackRemoteError(exception));
        return ExploreException.remote(error);
    }

    private static ErrorResponse fallbackRemoteError(HttpStatusCodeException exception) {
        HttpStatus status = resolveHttpStatus(exception);
        return new ErrorResponse(
            DIRECTORY_SERVICE_NAME,
            resolveStatusName(status.value()),
            extractMessage(exception),
            status.value(),
            Instant.now(),
            null,
            extractCorrelationId(exception)
        );
    }

    private static String resolveStatusName(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null ? resolved.name() : String.valueOf(status);
    }

    private static String extractMessage(HttpStatusCodeException exception) {
        return Optional.of(exception.getResponseBodyAsString())
            .filter(body -> !body.isBlank())
            .orElseGet(exception::getStatusText);
    }

    private static String extractCorrelationId(HttpStatusCodeException exception) {
        HttpHeaders headers = exception.getResponseHeaders();
        return headers != null ? headers.getFirst(CORRELATION_ID_HEADER) : null;
    }

    public static HttpStatus resolveHttpStatus(HttpStatusCodeException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null) {
            return status;
        }
        try {
            return HttpStatus.valueOf(exception.getStatusCode().value());
        } catch (IllegalArgumentException ignored) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
