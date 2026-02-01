package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.request.LoginRequestDTO;
import com.mystictarot.backend.dto.request.RegisterRequestDTO;
import com.mystictarot.backend.dto.request.SocialLoginRequestDTO;
import com.mystictarot.backend.dto.response.AuthResponseDTO;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
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

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponseDTO socialLogin(SocialLoginRequestDTO request) {
        // [Suy luận] Simplified implementation - in production, should validate OAuth2 token
        // with Google/Facebook APIs before creating/finding user
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
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
