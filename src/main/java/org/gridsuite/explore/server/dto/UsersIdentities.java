/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Identities returned by user-identity-server, indexed by sub.
 *
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
public record UsersIdentities(Map<String, UserIdentity> data) {

    /**
     * @return the displayable name of the given sub, falling back to the sub itself when its identity is unknown
     * or incomplete. A null sub has no name.
     */
    public static String toLabel(String sub, Map<String, UserIdentity> identityBySub) {
        if (sub == null) {
            return null;
        }
        UserIdentity identity = identityBySub.get(sub);
        if (identity == null) {
            return sub;
        }
        String firstName = identity.firstName();
        String lastName = identity.lastName();
        if (StringUtils.isNoneBlank(firstName, lastName)) {
            return firstName + " " + lastName;
        }
        if (StringUtils.isNotBlank(firstName)) {
            return firstName;
        }
        if (StringUtils.isNotBlank(lastName)) {
            return lastName;
        }
        return sub;
    }

    public record UserIdentity(String firstName, String lastName) {
    }
}
