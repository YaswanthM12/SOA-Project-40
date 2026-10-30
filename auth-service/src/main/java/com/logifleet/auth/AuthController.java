package com.logifleet.auth;

import java.util.Set;

import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    final UserRepository users;
    final PasswordEncoder encoder;
    final JwtService jwt;

    AuthController(UserRepository u, PasswordEncoder e, JwtService j) {
        users = u;
        encoder = e;
        jwt = j;
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@RequestBody AuthDtos.Credentials c) {
        if (c.username() == null || c.username().isBlank() || c.password() == null || c.password().isBlank()) {
            return ResponseEntity.badRequest().body("username and password required");
        }
        if (users.findByUsername(c.username()).isPresent()) {
            return ResponseEntity.status(409).body("username already exists");
        }

        String requestedRole = c.role() == null ? "DISPATCHER" : c.role().trim().toUpperCase();
        if (!Set.of("ADMIN", "DISPATCHER").contains(requestedRole)) {
            return ResponseEntity.badRequest().body("role must be ADMIN or DISPATCHER");
        }

        User u = new User();
        u.username = c.username();
        u.password = encoder.encode(c.password());
        u.role = requestedRole;
        users.save(u);
        return ResponseEntity.status(201).body(new AuthDtos.TokenResponse(jwt.create(u), u.username, u.role));
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody AuthDtos.Credentials c) {
        if (c.username() == null || c.username().isBlank() || c.password() == null || c.password().isBlank()) {
            return ResponseEntity.badRequest().body("username and password required");
        }

        return users.findByUsername(c.username())
                .filter(u -> encoder.matches(c.password(), u.password))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new AuthDtos.TokenResponse(jwt.create(u), u.username, u.role)))
                .orElseGet(() -> ResponseEntity.status(401).body("invalid credentials"));
    }
}
