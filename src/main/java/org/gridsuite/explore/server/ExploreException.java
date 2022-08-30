/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import java.util.Objects;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
public class ExploreException extends RuntimeException {

    public enum Type {
        STUDY_NOT_FOUND,
        CONTINGENCY_LIST_NOT_FOUND,
        FILTER_NOT_FOUND,
        NOT_ALLOWED,
        UNKNOWN_ELEMENT_TYPE,
        REMOTE_ERROR,
        DELETE_CASE_FAILED,
        IMPORT_CASE_FAILED,
        DELETE_STUDY_FAILED,
        DELETE_FILTER_FAILED,
        DELETE_ACTION_SERVER_FAILED,
        INSERT_STUDY_FAILED,
        CREATE_ELEMENT_FAILED,
        REPLACE_FORM_CONTINGENCY_LIST_WITH_SCRIPT_FAILED,
        CREATE_CONTINGENCY_LIST_FAILED,
        CREATE_FILTER_FAILED,
        NOTIFICATION_DIRECTORY_CHANGED;
    }

    private final Type type;

    public ExploreException(Type type) {
        super(Objects.requireNonNull(type.name()));
        this.type = type;
    }

    public ExploreException(Type type, String message) {
        super(message);
        this.type = type;
    }

    Type getType() {
        return type;
    }
}
