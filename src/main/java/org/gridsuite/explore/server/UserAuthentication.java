/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.explore.server;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class UserAuthentication implements Authentication {

    private final String principal;

    private final Collection<GrantedAuthority> authorities;

    private boolean authenticated = false;

    public UserAuthentication(String principal, Collection<? extends GrantedAuthority> authorities) {
        this.principal = principal;
        this.authorities = authorities == null
                ? AuthorityUtils.NO_AUTHORITIES
                : Collections.unmodifiableList(new ArrayList<>(authorities));
        setAuthenticated(true);
    }

    public String getUserId() {
        return principal;
    }

    public String getRoles() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining("|"));
    }

    // methods needed by interface

    @Override
    public String getName() {
        return principal;
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [");
        sb.append("Principal=").append(getPrincipal()).append(", ");
        sb.append("Authenticated=").append(isAuthenticated()).append(", ");
        sb.append("Granted Authorities=").append(this.authorities);
        sb.append("]");
        return sb.toString();
    }
}
