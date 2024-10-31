/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import static org.gridsuite.explore.server.utils.ExploreUtils.wrapRemoteError;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
    private String userIdentityServerBaseUri;

    @Autowired
    public UserIdentityService(RestTemplate restTemplate, RemoteServicesProperties remoteServicesProperties) {
        this.userIdentityServerBaseUri = remoteServicesProperties.getServiceUri("user-identity-server");
        this.restTemplate = restTemplate;
    }

    public String getUsersIdentities(List<String> subs) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_IDENTITY_API_VERSION + USERS_IDENTITY_PATH)
                .buildAndExpand(String.join(",", subs)).toUriString();
        try {
            return restTemplate.getForObject(userIdentityServerBaseUri + path, String.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return null; // no profile == unlimited import
            }
            throw wrapRemoteError(e.getMessage(), e.getStatusCode());
        }
    }

}
