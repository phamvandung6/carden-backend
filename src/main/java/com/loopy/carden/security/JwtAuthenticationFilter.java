package com.loopy.carden.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts HTTP requests and validates JWT tokens.
 * This filter runs once per request and validates the Bearer token in the Authorization header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip authentication for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            // If username is extracted and no authentication is set in SecurityContext
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validate token and set authentication
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Successfully authenticated user: {}", userEmail);
                } else {
                    log.debug("Invalid JWT token for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {} for IP: {}", 
                    e.getMessage(), getClientIpAddress(request));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the request path is a public endpoint that doesn't require authentication
     * @param request the HTTP request
     * @return true if it's a public endpoint
     */
	private boolean isPublicEndpoint(HttpServletRequest request) {
		String path = request.getServletPath();
		String method = request.getMethod();

		// Only allow unauthenticated access to specific auth endpoints (login/register/health)
		boolean isAuthLogin = path.equals("/v1/auth/login") && "POST".equals(method);
		boolean isAuthRegister = path.equals("/v1/auth/register") && "POST".equals(method);
		boolean isAuthHealth = path.equals("/v1/auth/health");

		return isAuthLogin
				|| isAuthRegister
				|| isAuthHealth
				|| path.startsWith("/public/")
				|| path.startsWith("/v1/public/")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui")
				|| path.equals("/actuator/health")
				|| path.equals("/actuator/info");
	}

    /**
     * Extracts the client IP address from the request
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
