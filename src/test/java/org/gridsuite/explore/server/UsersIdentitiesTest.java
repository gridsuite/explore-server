/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.gridsuite.explore.server.dto.UsersIdentities;
import org.gridsuite.explore.server.dto.UsersIdentities.UserIdentity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Florent MILLOT {@literal <florent.millot_externe at rte-france.com>}
 */
class UsersIdentitiesTest {

    private static final String SUB = "user01";

    @Test
    void nullSubHasNoLabel() {
        assertNull(UsersIdentities.toLabel(null, Map.of(SUB, new UserIdentity("John", "Doe"))));
    }

    @Test
    void unknownSubFallsBackToTheSubItself() {
        assertEquals(SUB, UsersIdentities.toLabel(SUB, Map.of()));
    }

    @Test
    void fullNameIsBuiltFromBothParts() {
        assertEquals("John Doe", UsersIdentities.toLabel(SUB, Map.of(SUB, new UserIdentity("John", "Doe"))));
    }

    @Test
    void onlyFirstNameIsUsedWhenLastNameIsBlank() {
        assertEquals("John", UsersIdentities.toLabel(SUB, Map.of(SUB, new UserIdentity("John", "  "))));
    }

    @Test
    void onlyLastNameIsUsedWhenFirstNameIsBlank() {
        assertEquals("Doe", UsersIdentities.toLabel(SUB, Map.of(SUB, new UserIdentity(null, "Doe"))));
    }

    @Test
    void bothNamesBlankFallsBackToTheSubItself() {
        assertEquals(SUB, UsersIdentities.toLabel(SUB, Map.of(SUB, new UserIdentity("", null))));
    }
}
