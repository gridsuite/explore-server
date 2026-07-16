/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import java.util.Map;

/**
 * Identities returned by user-identity-server, indexed by sub.
 *
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
public record UsersIdentities(Map<String, UserIdentity> data) {

    public record UserIdentity(String firstName, String lastName) {
        /**
         * @return the displayable name of the user, falling back to its sub when unknown or incomplete
         */
        public static String toLabel(UserIdentity identity, String sub) {
            if (identity == null) {
                return sub;
            }
            String firstName = identity.firstName();
            String lastName = identity.lastName();
            if (!isBlank(firstName) && !isBlank(lastName)) {
                return firstName + " " + lastName;
            }
            if (!isBlank(firstName)) {
                return firstName;
            }
            if (!isBlank(lastName)) {
                return lastName;
            }
            return sub;
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
