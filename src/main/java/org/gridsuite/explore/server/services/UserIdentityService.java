/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import lombok.Setter;
import org.gridsuite.explore.server.dto.UsersIdentities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Service
public class UserIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdentityService.class);

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
        return restTemplate.getForObject(userIdentityServerBaseUri + path, String.class);
    }

    /**
     * @return the identity of each sub, indexed by sub. Subs without a known identity are absent from the map,
     * and an unreachable server yields an empty map: callers fall back to the sub itself.
     */
    public Map<String, UsersIdentities.UserIdentity> getUsersIdentitiesMap(List<String> subs) {
        if (subs.isEmpty()) {
            return Map.of();
        }
        String path = UriComponentsBuilder.fromPath(DELIMITER + USER_IDENTITY_API_VERSION + USERS_IDENTITY_PATH)
            .buildAndExpand(String.join(",", subs)).toUriString();
        try {
            UsersIdentities usersIdentities = restTemplate.getForObject(userIdentityServerBaseUri + path, UsersIdentities.class);
            return usersIdentities == null || usersIdentities.data() == null ? Map.of() : usersIdentities.data();
        } catch (RestClientException e) {
            LOGGER.error("Failed to fetch users identities, falling back to subs", e);
            return Map.of();
        }
    }

}
