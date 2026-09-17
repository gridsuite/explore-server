/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Bean
    @SuppressWarnings("java:S4502") // CSRF is not applicable to this stateless REST API: authentication is not based on browser cookies
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is disabled because this application exposes a REST API
            // and does not use cookie-based authentication. Requests are authenticated
            // explicitly rather than relying on credentials automatically sent by the browser
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .addFilterAfter(new SecurityFilter(), SecurityContextHolderFilter.class);

        return http.build();
    }
}
