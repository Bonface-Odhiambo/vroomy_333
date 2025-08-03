package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.AuthRequest;
import com.insuranceplatform.backend.dto.AuthResponse;
import com.insuranceplatform.backend.dto.RegisterRequest;
import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.entity.Wallet;
import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.enums.UserStatus;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.AgentRepository;
import com.insuranceplatform.backend.repository.SuperagentRepository;
import com.insuranceplatform.backend.repository.UserRepository;
import com.insuranceplatform.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SuperagentRepository superagentRepository;
    private final AgentRepository agentRepository;
    private final WalletRepository walletRepository; // Added
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional // Ensures all database operations succeed or fail together
    public AuthResponse register(RegisterRequest request) {
        // TODO: Add validation here to check if email/phone already exists.
        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE) // Default to ACTIVE, can be changed
                .build();
        User savedUser = userRepository.save(user);

        // Create a wallet for the new user
        Wallet wallet = Wallet.builder().user(savedUser).build();
        walletRepository.save(wallet);

        // Role-specific logic
        if (request.getRole() == UserRole.SUPERAGENT) {
            // TODO: Add IRA API validation logic here
            Superagent superagent = Superagent.builder()
                    .user(savedUser)
                    .iraNumber(request.getIraNumber())
                    .kraPin(request.getKraPin())
                    .isVerified(false) // Superagents must be verified by an admin later
                    .build();
            superagentRepository.save(superagent);
        } else if (request.getRole() == UserRole.AGENT) {
            Superagent managingSuperagent = superagentRepository.findById(request.getSuperagentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Managing Superagent not found with ID: " + request.getSuperagentId()));

            Agent agent = Agent.builder()
                    .user(savedUser)
                    .superagent(managingSuperagent)
                    .build();
            agentRepository.save(agent);
        }

        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}