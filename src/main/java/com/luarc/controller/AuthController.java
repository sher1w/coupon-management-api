package com.luarc.controller;

import com.luarc.dto.AuthDTO;
import com.luarc.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        try {
            log.info("Register request received for email: {}", request.getEmail());
            AuthDTO.AuthResponse response = authService.register(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return new ResponseEntity<>(
                    new ErrorResponse(e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Registration error", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Registration failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        try {
            log.info("Login request received for email: {}", request.getEmail());
            AuthDTO.AuthResponse response = authService.login(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.warn("Login failed: {}", e.getMessage());
            return new ResponseEntity<>(
                    new ErrorResponse("Invalid email or password"),
                    HttpStatus.UNAUTHORIZED
            );
        } catch (Exception e) {
            log.error("Login error", e);
            return new ResponseEntity<>(
                    new ErrorResponse("Login failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Simple error response object
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
