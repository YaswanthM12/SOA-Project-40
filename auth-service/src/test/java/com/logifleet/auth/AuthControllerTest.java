package com.logifleet.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthControllerTest {

    @Test
    void jwtFilter_shouldAllowPublicAuthEndpointsWithoutToken() throws Exception {
        SecurityConfig.JwtFilter filter = new SecurityConfig.JwtFilter("logifleet-jwt-secret-key-1234567890");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/auth/register");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void register_shouldHashPasswordAndReturnToken() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("admin")).thenReturn(Optional.empty());

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService("logifleet-jwt-secret-key-1234567890");
        AuthController controller = new AuthController(users, encoder, jwtService);

        ResponseEntity<?> response = controller.register(new AuthDtos.Credentials("admin", "Admin@123", "ADMIN"));

        assertEquals(201, response.getStatusCode().value());
        AuthDtos.TokenResponse body = (AuthDtos.TokenResponse) response.getBody();
        assertNotNull(body);
        assertEquals("admin", body.username());
        assertEquals("ADMIN", body.role());
        assertTrue(body.token().length() > 20);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(encoder.matches("Admin@123", saved.password));
    }

    @Test
    void login_shouldAcceptValidCredentials() {
        UserRepository users = mock(UserRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User stored = new User();
        stored.username = "admin";
        stored.password = encoder.encode("Admin@123");
        stored.role = "ADMIN";
        when(users.findByUsername("admin")).thenReturn(Optional.of(stored));

        AuthController controller = new AuthController(users, encoder, new JwtService("logifleet-jwt-secret-key-1234567890"));

        ResponseEntity<?> response = controller.login(new AuthDtos.Credentials("admin", "Admin@123", "ADMIN"));

        assertEquals(200, response.getStatusCode().value());
        AuthDtos.TokenResponse body = (AuthDtos.TokenResponse) response.getBody();
        assertNotNull(body);
        assertEquals("ADMIN", body.role());
    }
}
