/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.gridsuite.explore.server.utils.ErrorHandlingUtils.wrapRemoteError;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class UserAdminService {

    private static final String USER_ADMIN_API_VERSION = "v1";
    private static final String USERS_MAX_ALLOWED_CASES_URI = "/users/{sub}/profile/max-cases";
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

    public Integer getUserMaxAllowedCases(String sub) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + USERS_MAX_ALLOWED_CASES_URI)
                .buildAndExpand(sub).toUriString();
        try {
            return restTemplate.getForObject(userAdminServerBaseUri + path, Integer.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return null; // no profile == unlimited import
            }
            throw wrapRemoteError(e.getMessage(), e.getStatusCode());

        }
    }

    public Integer getCasesAlertThreshold() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + CASES_ALERT_THRESHOLD_URI)
            .buildAndExpand().toUriString();
        try {
            return restTemplate.getForObject(userAdminServerBaseUri + path, Integer.class);
        } catch (HttpStatusCodeException e) {
            throw wrapRemoteError(e.getMessage(), e.getStatusCode());
        }
    }
}
