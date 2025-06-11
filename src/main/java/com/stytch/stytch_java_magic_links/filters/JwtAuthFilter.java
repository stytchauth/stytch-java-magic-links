package com.stytch.stytch_java_magic_links.filters;

import com.stytch.java.consumer.models.sessions.Session;
import com.stytch.stytch_java_magic_links.services.StytchService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.stytch.stytch_java_magic_links.services.StytchService.STYTCH_SESSION_JWT_COOKIE_NAME;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private StytchService authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cookie> jwtCookie = Arrays.stream(cookies).filter((Cookie cookie) -> {
            return cookie.getName().equals(STYTCH_SESSION_JWT_COOKIE_NAME);
        }).findFirst();

        if (jwtCookie.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtCookie.get().getValue();
        Session session;
        try {
            session = authenticationService.authenticateJwt(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (session != null) {
            UsernamePasswordAuthenticationToken authenticationToken = getUsernamePasswordAuthenticationToken(session.getUserId());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }

    @NotNull
    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String username) {
        UserDetails userDetails = new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public String getPassword() {
                return "";
            }

            @Override
            public String getUsername() {
                return username;
            }
        };
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
