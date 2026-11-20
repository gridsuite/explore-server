/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
public class ExploreApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExploreApplication.class, args);
    }
}
