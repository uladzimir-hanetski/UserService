package org.example.userserv.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.userserv.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final SecurityUtil securityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String token = getToken(request);

        if (token != null && securityUtil.validateToken(token)) authenticate(token);

        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer "))
            return bearerToken.substring(7);

        return null;
    }

    private void authenticate(String token) {
        UUID userId = securityUtil.getUserIdFromToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
