/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.services;

import org.gridsuite.explore.server.dto.DirectoryElementType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Directory element types handled by an {@link IDirectoryElementsService} implementation.
 * Read by {@link ExploreService} at startup to build the element type -> service registry.
 * An annotation is used rather than an interface method so that the registry stays correct
 * when the service is replaced by a Mockito mock in tests.
 *
 * @author Mathieu DEHARBE <mathieu.deharbe at rte-france.com>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DirectoryElementTypes {

    /**
     * Element types handled by the annotated service.
     */
    DirectoryElementType[] value();
}
