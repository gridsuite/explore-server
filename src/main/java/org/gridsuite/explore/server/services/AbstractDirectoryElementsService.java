/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractDirectoryElementsService implements IDirectoryElementsService {
    protected static final String DELIMITER = "/";

    @Getter @Setter protected String serverBaseUri;
    @Getter(AccessLevel.PROTECTED) protected final RestTemplate restTemplate;

    protected static HttpHeaders getUserHeaders(String userId) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_USER_ID, userId);
        return headers;
    }

    protected static <T> HttpEntity<T> getHttpEntityWithHeaders(String userId, T content) {
        final HttpHeaders headers = getUserHeaders(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(content, headers);
    }

    protected static <T> HttpEntity<T> getHttpEntityWithContentHeaders(T content) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(content, headers);
    }
}
