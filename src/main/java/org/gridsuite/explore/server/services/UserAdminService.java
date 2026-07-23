/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.gridsuite.explore.server.dto.QuotaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import java.util.Objects;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class UserAdminService {

    private static final String USER_ADMIN_API_VERSION = "v1";
    private static final String USERS_QUOTA_URI = "/users/{sub}/quota";
    private static final String USERS_MAX_QUOTA_URI = USERS_QUOTA_URI + "/max";
    private static final String CASES_ALERT_THRESHOLD_URI = "/cases-alert-threshold";
    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    @Setter
    private String userAdminServerBaseUri;

    @Autowired
    public UserAdminService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.userAdminServerBaseUri = remoteServicesProperties.getServiceUri("user-admin-server");
        this.restTemplate = restTemplate;
    }

    public Map<QuotaType, Integer> getUserMaxQuota(String sub) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + USERS_MAX_QUOTA_URI)
                .buildAndExpand(sub).toUriString();
        return restTemplate.exchange(
                userAdminServerBaseUri + path,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<QuotaType, Integer>>() {
                }).getBody();
    }

    public Integer getUserMaxAllowedCases(String sub) {
        Map<QuotaType, Integer> userMaxQuotas = getUserMaxQuota(sub);

        return userMaxQuotas.getOrDefault(QuotaType.CASES, null);
    }

    public Integer getCasesAlertThreshold() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + CASES_ALERT_THRESHOLD_URI)
            .buildAndExpand().toUriString();
        return restTemplate.getForObject(userAdminServerBaseUri + path, Integer.class);

    }

    public ResponseEntity<String> getGroups() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + "/groups")
            .buildAndExpand()
            .toUriString();
        try {
            return restTemplate.exchange(userAdminServerBaseUri + path, HttpMethod.GET, null, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                .headers(Objects.requireNonNullElseGet(e.getResponseHeaders(), HttpHeaders::new))
                .body(e.getResponseBodyAsString());
        }
    }
}
