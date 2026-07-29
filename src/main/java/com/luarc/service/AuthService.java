package com.luarc.service;

import com.luarc.dto.AuthDTO;
import com.luarc.entity.User;
import com.luarc.repository.UserRepository;
import com.luarc.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate token
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .message("User registered successfully")
                .build();
    }

    /**
     * Login user and return JWT token
     */
    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Generate token
            String token = jwtTokenProvider.generateTokenFromAuthentication(authentication);

            // Fetch user details
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User logged in successfully: {}", request.getEmail());

            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .message("Login successful")
                    .build();

        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password", e);
        }
    }
}
