/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.gridsuite.explore.server.ExploreApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.gridsuite.explore.server.ExploreConstants.HEADER_ROLES;
import static org.gridsuite.explore.server.ExploreConstants.HEADER_USER_ID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Configuration
public class ExploreSwaggerConfig {

    @Bean
    public OpenAPI createOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Explore API")
                        .description("This is the documentation of the Explore REST API")
                        .version(ExploreApi.API_VERSION))
                .components(new Components()
                        .addSecuritySchemes(
                                HEADER_USER_ID,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(HEADER_USER_ID)
                        )
                        .addSecuritySchemes(
                                HEADER_ROLES,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(HEADER_ROLES)
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(HEADER_USER_ID)
                                .addList(HEADER_ROLES)
                );
    }
}
