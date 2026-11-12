/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.utils;

import com.powsybl.commons.exceptions.UncheckedInterruptedException;
import mockwebserver3.MockWebServer;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.gridsuite.explore.server.services.NotificationService.HEADER_ELEMENT_UUID;
import static org.gridsuite.explore.server.services.NotificationService.HEADER_MODIFICATION_DATE;
import static org.gridsuite.explore.server.services.NotificationService.HEADER_MODIFIED_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author AJELLAL Ali <ali.ajellal at rte-france.com>
 */
public final class TestUtils {

    private static final long TIMEOUT = 100;
    private static final String ELEMENT_UPDATE_DESTINATION = "element.update";

    private TestUtils() {

    }

    public static void assertElementUpdatedSent(OutputDestination output, UUID elementUuid, String modifiedBy) {
        Message<byte[]> message = output.receive(TIMEOUT, ELEMENT_UPDATE_DESTINATION);
        assertNotNull(message);
        assertEquals(elementUuid, message.getHeaders().get(HEADER_ELEMENT_UUID));
        assertEquals(modifiedBy, message.getHeaders().get(HEADER_MODIFIED_BY));
        assertNotNull(message.getHeaders().get(HEADER_MODIFICATION_DATE));
    }

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
