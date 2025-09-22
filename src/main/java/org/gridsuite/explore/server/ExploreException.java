/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.ErrorResponse;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
public class ExploreException extends RuntimeException {

    public enum Type {
        NOT_FOUND,
        NOT_ALLOWED,
        UNKNOWN_ELEMENT_TYPE,
        REMOTE_ERROR,
        IMPORT_CASE_FAILED,
        INCORRECT_CASE_FILE,
        MAX_ELEMENTS_EXCEEDED,
    }

    private final Type type;
    private final ErrorResponse remoteError;

    public ExploreException(Type type, String message) {
        this(type, message, null);
    }

    public ExploreException(Type type, String message, ErrorResponse remoteError) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.remoteError = remoteError;
    }

    public static ExploreException of(Type type, String message, Object... args) {
        return new ExploreException(type, args.length == 0 ? message : String.format(message, args));
    }

    public static ExploreException remote(Type type, ErrorResponse remoteError) {
        return new ExploreException(type, Objects.requireNonNull(remoteError, "remoteError must not be null").message(), remoteError);
    }

    Type getType() {
        return type;
    }

    Optional<ErrorResponse> getRemoteError() {
        return Optional.ofNullable(remoteError);
    }
}
