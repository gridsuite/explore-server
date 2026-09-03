/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.gridsuite.explore.server.UserAuthentication;
import org.gridsuite.explore.server.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public class SecurityFilter extends OncePerRequestFilter {

    private static final String HEADER_ROLES = "roles";
    private static final String HEADER_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String rolesHeader = request.getHeader(HEADER_ROLES);

        if (userId != null && !userId.isEmpty()) {
            List<GrantedAuthority> authorities = Collections.emptyList();
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                authorities = Arrays.stream(rolesHeader.split("\\|"))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }

            SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(userId, authorities));
            /*
            Set<String> roles = Collections.emptySet();
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                roles = Arrays.stream(rolesHeader.split("\\|"))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .collect(Collectors.toSet());
            }
            userContext.setUserId(userId);
            userContext.setRoles(roles);
            */
        }

        filterChain.doFilter(request, response);
    }
}
