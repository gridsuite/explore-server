/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.utils;

import org.gridsuite.explore.server.ExploreException;
import org.springframework.http.HttpStatusCode;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com
 */
public final class ExploreUtils {

    private ExploreUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ExploreException wrapRemoteError(String response, HttpStatusCode statusCode) {
        if (!"".equals(response)) {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, response);
        } else {
            throw new ExploreException(ExploreException.Type.REMOTE_ERROR, "{\"message\": " + statusCode + "\"}");
        }
    }

}
