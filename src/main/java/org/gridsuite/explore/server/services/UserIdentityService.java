/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.Setter;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Service
public class UserIdentityService {

    private static final String USER_IDENTITY_API_VERSION = "v1";
    private static final String USERS_IDENTITY_PATH = "/users/identities?subs={subs}";

    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;

    @Setter
    private String userAdminServerBaseUri;

    @Autowired
    public UserIdentityService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.userAdminServerBaseUri = remoteServicesProperties.getServiceUri("user-identity-server");
        this.restTemplate = restTemplate;
    }

    public String getUserIdentities(List<String> subs) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_IDENTITY_API_VERSION + USERS_IDENTITY_PATH)
                .buildAndExpand(String.join(",", subs)).toUriString();
        try {
            return restTemplate.getForObject(userAdminServerBaseUri + path, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
