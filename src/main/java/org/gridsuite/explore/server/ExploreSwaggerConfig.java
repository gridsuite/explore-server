/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.explore.server;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Configuration
public class ExploreSwaggerConfig {

    private static final String USER_ID = "userId";
    private static final String ROLES = "roles";

    @Bean
    public OpenAPI createOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Explore API")
                        .description("This is the documentation of the Explore REST API")
                        .version(ExploreApi.API_VERSION))
                .components(new Components()
                        .addSecuritySchemes(
                                USER_ID,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(USER_ID)
                        )
                        .addSecuritySchemes(
                                ROLES,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(ROLES)
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(USER_ID)
                                .addList(ROLES)
                );
    }
}
