package edu.tlu.klgd.infracstructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader(SecurityConstant.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstant.BEARER_PREFIX)) {
            String token = header.substring(SecurityConstant.BEARER_PREFIX_LENGTH);
            jwtService.parse(token).ifPresent(principal -> {
                var authorities = principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority(SecurityConstant.ROLE_PREFIX + role))
                    .toList();
                var authentication = new UsernamePasswordAuthenticationToken(
                    principal.username(),
                    null,
                    authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
