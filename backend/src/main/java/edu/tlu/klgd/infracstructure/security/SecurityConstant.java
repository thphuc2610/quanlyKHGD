package edu.tlu.klgd.infracstructure.security;

import java.util.List;

public final class SecurityConstant {
    private SecurityConstant() {
    }

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    public static final String HEALTH_ENDPOINT = "/actuator/health";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI_ENDPOINTS = "/swagger-ui/**";
    public static final String OPENAPI_DOCS_ENDPOINTS = "/v3/api-docs/**";
    public static final String ALL_ENDPOINTS = "/**";
    public static final String ALL_ORIGINS = "*";
    public static final long CORS_MAX_AGE_SECONDS = 3600;

    public static final List<String> CORS_ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    public static final List<String> CORS_ALLOWED_HEADERS = List.of(AUTHORIZATION_HEADER, "Content-Type");
}
