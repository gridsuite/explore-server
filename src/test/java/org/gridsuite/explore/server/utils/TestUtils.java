/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.utils;

import com.powsybl.commons.exceptions.UncheckedInterruptedException;
import okhttp3.mockwebserver.MockWebServer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
public final class TestUtils {
    private TestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final long TIMEOUT = 100;

    public static Set<RequestWithBody> getRequestsWithBodyDone(int n, MockWebServer server) throws UncheckedInterruptedException {
        return IntStream.range(0, n).mapToObj(i -> {
            try {
                var request = server.takeRequest(TIMEOUT, TimeUnit.MILLISECONDS);
                if (request == null) {
                    throw new AssertionError("Expected " + n + " requests, got only " + i);
                }
                return new RequestWithBody(request.getPath(), request.getBody().readUtf8());
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }).collect(Collectors.toSet());
    }
}
