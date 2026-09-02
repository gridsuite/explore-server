/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.dto;

import java.util.UUID;

/**
 * One element to put inside a composite modification when creating / updating a composite modification element.
 *
 * @param modificationUuid the modification to store inside the composite. A selected shared modification is
 *                         resolved by the caller to the composite it points to, so it is stored as a plain copy.
 * @param description      description to apply to the stored copy, or {@code null} to keep the source's own
 *                         description. Carries a selected reference's description onto the resolved composite copy.
  @author Maissa Souissi <maissa.souissi at rte-france.com>
 */

public record CompositeModificationContentInfos(UUID modificationUuid, String description) { }
