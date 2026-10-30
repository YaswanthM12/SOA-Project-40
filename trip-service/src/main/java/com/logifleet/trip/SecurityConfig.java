package com.logifleet.trip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity h, JwtFilter f) throws Exception {
        return h
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/error").permitAll().anyRequest().authenticated())
                .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtFilter jwtFilter(@Value("${app.jwt.secret}") String s) {
        return new JwtFilter(s);
    }

    @Bean
    FilterRegistrationBean<JwtFilter> disableAutoRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    static class JwtFilter extends OncePerRequestFilter {
        final SecretKey key;

        JwtFilter(String s) {
            key = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        protected void doFilterInternal(HttpServletRequest q, HttpServletResponse p, FilterChain c)
                throws IOException, ServletException {
            String h = q.getHeader("Authorization");

            try {
                if (h == null || !h.startsWith("Bearer ")) {
                    throw new JwtException("Missing token");
                }

                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(h.substring(7))
                        .getPayload();

                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : Collections.emptyList();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                SecurityContextHolder.setContext(context);
                c.doFilter(q, p);
                return;
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                p.sendError(401, "Invalid or missing JWT");
                return;
            }
        }
    }
}
