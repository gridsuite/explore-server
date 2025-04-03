package org.gridsuite.explore.server.dto;

public record PermissionResponse(boolean hasPermission, String permissionCheckResult) { }
