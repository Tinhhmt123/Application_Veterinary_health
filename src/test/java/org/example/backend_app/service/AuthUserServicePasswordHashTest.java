package org.example.backend_app.service;

import org.example.backend_app.entity.User;
import org.example.backend_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthUserServicePasswordHashTest {

    private UserRepository userRepository;
    private AuthUserService authUserService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authUserService = new AuthUserService(userRepository, cloudinaryService, jdbcTemplate, mailSenderProvider);
    }

    @Test
    void registerHashesPasswordWithBCrypt() {
        when(userRepository.existsByEmailIgnoreCase("user@test.com")).thenReturn(false);

        User req = new User();
        req.setFullName("Test User");
        req.setEmail("user@test.com");
        req.setPassword("secret123");

        User saved = authUserService.register(req);

        assertNotEquals("secret123", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2a$") || saved.getPassword().startsWith("$2b$"));
        assertTrue(encoder.matches("secret123", saved.getPassword()));
    }

    @Test
    void loginMatchesBCryptHashAndRejectsWrongPassword() {
        User dbUser = new User();
        dbUser.setEmail("user@test.com");
        dbUser.setPassword(encoder.encode("secret123"));
        dbUser.setIsActive(true);
        dbUser.setIsVerified(true);

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(dbUser));

        User okReq = new User();
        okReq.setEmail("user@test.com");
        okReq.setPassword("secret123");
        User loggedIn = authUserService.login(okReq);
        assertEquals("user@test.com", loggedIn.getEmail());

        User badReq = new User();
        badReq.setEmail("user@test.com");
        badReq.setPassword("wrongpass");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authUserService.login(badReq));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void loginUpgradesLegacyPlainTextPasswordToBCrypt() {
        User legacyUser = new User();
        legacyUser.setEmail("tinh@gmail.com");
        legacyUser.setPassword("123456");
        legacyUser.setIsActive(true);
        legacyUser.setIsVerified(true);

        when(userRepository.findByEmailIgnoreCase("tinh@gmail.com")).thenReturn(Optional.of(legacyUser));

        User req = new User();
        req.setEmail("tinh@gmail.com");
        req.setPassword("123456");

        User loggedIn = authUserService.login(req);

        assertNotEquals("123456", loggedIn.getPassword());
        assertTrue(encoder.matches("123456", loggedIn.getPassword()));
        verify(userRepository, atLeastOnce()).save(legacyUser);
    }

    @Test
    void resetPasswordHashesNewPasswordWithBCrypt() {
        User dbUser = new User();
        dbUser.setEmail("user@test.com");
        dbUser.setIsActive(true);
        dbUser.setIsVerified(true);
        dbUser.setOtpCode("654321");
        dbUser.setOtpExpiry(LocalDateTime.now().plusMinutes(3));

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(dbUser));

        authUserService.resetPassword(Map.of(
                "email", "user@test.com",
                "otp_code", "654321",
                "new_password", "newpass999"
        ));

        assertNotEquals("newpass999", dbUser.getPassword());
        assertTrue(encoder.matches("newpass999", dbUser.getPassword()));
    }

    @Test
    void loginRejectsGoogleAccountWithNullPasswordGracefully() {
        User googleUser = new User();
        googleUser.setEmail("google@gmail.com");
        googleUser.setPassword(null);
        googleUser.setIsActive(true);
        googleUser.setIsVerified(true);

        when(userRepository.findByEmailIgnoreCase("google@gmail.com")).thenReturn(Optional.of(googleUser));

        User req = new User();
        req.setEmail("google@gmail.com");
        req.setPassword("anything");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authUserService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
