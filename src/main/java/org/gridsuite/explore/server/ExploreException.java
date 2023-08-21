/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Data
@AllArgsConstructor
public class ExploreException extends RuntimeException {

    public enum Type {
        NOT_ALLOWED,
        UNKNOWN_ELEMENT_TYPE,
        REMOTE_ERROR,
        IMPORT_CASE_FAILED,
        INCORRECT_CASE_FILE
    }

    @NonNull private final Type type;

    public ExploreException(@NonNull Type type, String message) {
        super(message);
        this.type = type;
    }
}
