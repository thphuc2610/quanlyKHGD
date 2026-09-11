package edu.tlu.klgd.infracstructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.common.ApiURL;
import edu.tlu.klgd.domain.common.MessageResponse;
import edu.tlu.klgd.infracstructure.config.ApplicationProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        ApplicationProperties applicationProperties,
        ObjectMapper objectMapper
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityConstant.HEALTH_ENDPOINT).permitAll()
                .requestMatchers(SecurityConstant.SWAGGER_UI_HTML).permitAll()
                .requestMatchers(SecurityConstant.SWAGGER_UI_ENDPOINTS).permitAll()
                .requestMatchers(SecurityConstant.OPENAPI_DOCS_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, SecurityConstant.ALL_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, ApiURL.AUTH_LOGIN).permitAll()
                .requestMatchers(HttpMethod.POST, ApiURL.AUTH_FORGOT_PASSWORD).permitAll()
                .requestMatchers(HttpMethod.POST, ApiURL.AUTH_RESET_PASSWORD).permitAll()
                .requestMatchers(ApiURL.allSubPaths(ApiURL.ADMIN)).hasRole(SecurityConstant.ADMIN_ROLE)
                .requestMatchers(ApiURL.allSubPaths(ApiURL.IMPORT_BATCH)).hasRole(SecurityConstant.ADMIN_ROLE)
                .requestMatchers(ApiURL.allSubPaths(ApiURL.REPORT)).hasRole(SecurityConstant.ADMIN_ROLE)
                .requestMatchers(ApiURL.AUTH_ME).authenticated()
                .requestMatchers(ApiURL.AUTH_CHANGE_PASSWORD).authenticated()
                .requestMatchers(ApiURL.allSubPaths(ApiURL.DASHBOARD)).hasRole(SecurityConstant.ADMIN_ROLE)
                .requestMatchers(ApiURL.allSubPaths(ApiURL.WORKLOAD)).authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, ApiMessage.UNAUTHORIZED, request.getServletPath()))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeError(response, HttpStatus.FORBIDDEN, ApiMessage.FORBIDDEN, request.getServletPath()))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(applicationProperties.cors().allowedOrigins());
        config.setAllowedMethods(SecurityConstant.CORS_ALLOWED_METHODS);
        config.setAllowedHeaders(SecurityConstant.CORS_ALLOWED_HEADERS);
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(SecurityConstant.ALL_ENDPOINTS, config);
        return source;
    }

    private void writeError(
        HttpServletResponse response,
        HttpStatus status,
        String message,
        String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new MessageResponse(status.value(), message, path));
    }
}
