/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import com.powsybl.ws.commons.error.AbstractPowsyblWsException;
import com.powsybl.ws.commons.error.BusinessErrorCode;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class ExploreException extends AbstractPowsyblWsException {

    private final ExploreBusinessErrorCode errorCode;
    private final PowsyblWsProblemDetail remoteError;

    public ExploreException(ExploreBusinessErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ExploreException(ExploreBusinessErrorCode errorCode, String message, PowsyblWsProblemDetail remoteError) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.remoteError = remoteError;
    }

    public static ExploreException of(ExploreBusinessErrorCode errorCode, String message, Object... args) {
        return new ExploreException(errorCode, args.length == 0 ? message : String.format(message, args));
    }

    public Optional<ExploreBusinessErrorCode> getErrorCode() {
        return Optional.of(errorCode);
    }

    @Override
    public Optional<BusinessErrorCode> getBusinessErrorCode() {
        return Optional.ofNullable(errorCode);
    }

    public Optional<PowsyblWsProblemDetail> getRemoteError() {
        return Optional.ofNullable(remoteError);
    }

}
