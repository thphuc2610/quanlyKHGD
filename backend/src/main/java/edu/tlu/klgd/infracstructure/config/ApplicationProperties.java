package edu.tlu.klgd.infracstructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = AppPropertyConstant.APP_PREFIX)
public record ApplicationProperties(
    Cors cors,
    Jwt jwt
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }
}
