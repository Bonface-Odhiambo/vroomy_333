package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.entity.Wallet;
import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.enums.UserStatus;
import com.insuranceplatform.backend.exception.InvalidIraNumberException;
import com.insuranceplatform.backend.exception.InvalidTokenException;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.exception.UserAlreadyExistsException;
import com.insuranceplatform.backend.repository.AgentRepository;
import com.insuranceplatform.backend.repository.SuperagentRepository;
import com.insuranceplatform.backend.repository.UserRepository;
import com.insuranceplatform.backend.repository.WalletRepository;
import com.insuranceplatform.backend.util.IraApiValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SuperagentRepository superagentRepository;
    private final AgentRepository agentRepository;
    private final WalletRepository walletRepository;
    private final IraApiValidator iraApiValidator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Error: Email is already in use!");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Error: Phone number is already in use!");
        }

        User.UserBuilder userBuilder = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole());

        // Set status based on role. Superagents require admin approval.
        if (request.getRole() == UserRole.SUPERAGENT) {
            // Ensure PENDING_APPROVAL exists in your UserStatus enum. If not, use INACTIVE.
            userBuilder.status(UserStatus.PENDING_APPROVAL);
        } else {
            userBuilder.status(UserStatus.ACTIVE);
        }

        User savedUser = userRepository.save(userBuilder.build());

        walletRepository.save(Wallet.builder().user(savedUser).build());

        if (request.getRole() == UserRole.SUPERAGENT) {
            if (!iraApiValidator.isIraNumberValid(request.getIraNumber())) {
                throw new InvalidIraNumberException("The provided IRA number is not valid or could not be verified.");
            }
            superagentRepository.save(Superagent.builder()
                    .user(savedUser)
                    .iraNumber(request.getIraNumber())
                    .kraPin(request.getKraPin())
                    .paybillNumber(request.getPaybillNumber())
                    .isVerified(false).build());
        } else if (request.getRole() == UserRole.AGENT) {
            Superagent managingSuperagent = superagentRepository.findById(request.getSuperagentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Managing Superagent not found with ID: " + request.getSuperagentId()));
            agentRepository.save(Agent.builder().user(savedUser).superagent(managingSuperagent).build());
        }

        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            // In a real application, you would call an EmailService here.
            System.out.println("Password Reset Token for " + user.getEmail() + ": " + token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token has expired.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found during token refresh"));

        if (jwtService.isTokenValid(refreshToken, user)) {
            String newAccessToken = jwtService.generateToken(user);
            return AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new InvalidTokenException("Invalid refresh token.");
        }
    }

    public void logout() {
        System.out.println("Logout initiated. Client should clear tokens.");
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in security context: " + username));
    }
}