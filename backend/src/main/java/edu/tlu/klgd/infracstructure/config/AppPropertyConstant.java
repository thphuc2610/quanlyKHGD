package edu.tlu.klgd.infracstructure.config;

public final class AppPropertyConstant {
    private AppPropertyConstant() {
    }

    public static final String APP_PREFIX = "app";
    public static final String JWT_SECRET = "${app.jwt.secret}";
    public static final String JWT_EXPIRATION_MINUTES = "${app.jwt.expiration-minutes}";
}
