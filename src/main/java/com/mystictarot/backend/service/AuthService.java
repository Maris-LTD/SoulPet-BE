package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.LoginRequestDTO;
import com.mystictarot.backend.dto.request.RegisterRequestDTO;
import com.mystictarot.backend.dto.request.SocialLoginRequestDTO;
import com.mystictarot.backend.dto.response.AuthResponseDTO;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .plan(PlanType.FREE)
                .extraCredits(0)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());

        log.info("User registered successfully: userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return buildAuthResponse(savedUser, token);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email - {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        log.info("User logged in successfully: userId={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponseDTO socialLogin(SocialLoginRequestDTO request) {
        // [Suy luận] Simplified implementation - in production, should validate OAuth2 token
        // with Google/Facebook APIs before creating/finding user
        
        log.info("Social login attempt: provider={}, email={}", request.getProvider(), request.getEmail());
        
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            log.warn("Social login failed: Email is required");
            throw new IllegalArgumentException("Email is required for social login");
        }
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    log.info("Creating new user from social login: email={}", request.getEmail());
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .name(request.getName() != null ? request.getName() : "User")
                            .plan(PlanType.FREE)
                            .extraCredits(0)
                            .avatarUrl(request.getAvatarUrl())
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        log.info("Social login successful: userId={}, email={}, provider={}", user.getId(), user.getEmail(), request.getProvider());
        return buildAuthResponse(user, token);
    }

    private AuthResponseDTO buildAuthResponse(User user, String token) {
        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .plan(user.getPlan())
                .extraCredits(user.getExtraCredits())
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
