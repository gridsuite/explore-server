/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ExploreException extends AbstractBusinessException {

    private final ExploreBusinessErrorCode errorCode;
    private final transient Map<String, Object> businessErrorValues;

    public ExploreException(ExploreBusinessErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public ExploreException(ExploreBusinessErrorCode errorCode, String message, Map<String, Object> businessErrorValues) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.businessErrorValues = businessErrorValues != null ? Map.copyOf(businessErrorValues) : Map.of();
    }

    public static ExploreException of(ExploreBusinessErrorCode errorCode, String message, Object... args) {
        return new ExploreException(errorCode, args.length == 0 ? message : String.format(message, args));
    }

    @NonNull
    @Override
    public ExploreBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    @NotNull
    @Override
    public Map<String, Object> getBusinessErrorValues() {
        return businessErrorValues;
    }

}
