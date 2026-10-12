/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.QuotaState;
import org.gridsuite.explore.server.dto.QuotaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class UserAdminService {

    private static final String USER_ADMIN_API_VERSION = "v1";
    private static final String USERS_QUOTA_URI = "/users/{sub}/quota";
    private static final String USERS_QUOTA_STATE_URI = USERS_QUOTA_URI + "/state";
    private static final String CASES_ALERT_THRESHOLD_URI = "/cases-alert-threshold";
    private static final String DELIMITER = "/";

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    private String userAdminServerBaseUri;

    @Autowired
    public UserAdminService(RestClient.Builder restClientBuilder, RemoteServicesProperties remoteServicesProperties) {
        this.restClientBuilder = restClientBuilder;
        this.userAdminServerBaseUri = remoteServicesProperties.getServiceUri("user-admin-server");
        this.restClient = restClientBuilder.baseUrl(userAdminServerBaseUri).build();
    }

    public void setUserAdminServerBaseUri(String userAdminServerBaseUri) {
        this.userAdminServerBaseUri = userAdminServerBaseUri;
        this.restClient = restClientBuilder
            .baseUrl(userAdminServerBaseUri)
            .build();
    }

    public Map<QuotaType, QuotaState> getUserQuotaState() {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + USERS_QUOTA_STATE_URI)
            .buildAndExpand(sub).toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });
    }

    public Integer getUserMaxAllowedCases() {
        Map<QuotaType, QuotaState> userMaxQuotas = getUserQuotaState();

        QuotaState stateCases = userMaxQuotas.getOrDefault(QuotaType.CASES, null);
        if (stateCases != null) {
            return stateCases.max();
        }
        return null;
    }

    public Integer getCasesAlertThreshold() {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_ADMIN_API_VERSION + CASES_ALERT_THRESHOLD_URI)
            .buildAndExpand().toUriString();

        return restClient.get()
            .uri(path)
            .retrieve()
            .body(Integer.class);
    }
}
