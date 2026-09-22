package com.logifleet.trip;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(
            HttpSecurity h,
            JwtFilter f
    ) throws Exception {

        return h
                .csrf(c -> c.disable())
                .authorizeHttpRequests(
                        a -> a.anyRequest().authenticated()
                )
                .addFilterBefore(
                        f,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    JwtFilter jwtFilter(
            @Value("${app.jwt.secret}") String s
    ) {
        return new JwtFilter(s);
    }

    static class JwtFilter extends OncePerRequestFilter {

        final SecretKey key;

        JwtFilter(String s) {
            key = Keys.hmacShaKeyFor(
                    s.getBytes(StandardCharsets.UTF_8)
            );
        }

        protected void doFilterInternal(
                HttpServletRequest q,
                HttpServletResponse p,
                FilterChain c
        ) throws IOException, ServletException {

            try {
                String h = q.getHeader("Authorization");

                if (h == null || !h.startsWith("Bearer ")) {
                    throw new JwtException("Missing");
                }

                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(h.substring(7));

                c.doFilter(q, p);

            } catch (Exception e) {
                p.sendError(
                        401,
                        "Invalid or missing JWT"
                );
            }
        }
    }
}