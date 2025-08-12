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
import com.insuranceplatform.backend.exception.InvalidIraNumberException; // Import new exception
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.exception.UserAlreadyExistsException;
import com.insuranceplatform.backend.repository.AgentRepository;
import com.insuranceplatform.backend.repository.SuperagentRepository;
import com.insuranceplatform.backend.repository.UserRepository;
import com.insuranceplatform.backend.repository.WalletRepository;
import com.insuranceplatform.backend.util.IraApiValidator; // Import the validator
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SuperagentRepository superagentRepository;
    private final AgentRepository agentRepository;
    private final WalletRepository walletRepository;
    private final IraApiValidator iraApiValidator; // 1. INJECT THE VALIDATOR
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;



    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Existing validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Error: Email is already in use!");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Error: Phone number is already in use!");
        }

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        // Create a wallet for the new user
        Wallet wallet = Wallet.builder().user(savedUser).build();
        walletRepository.save(wallet);

        // Role-specific logic
        if (request.getRole() == UserRole.SUPERAGENT) {
            // --- THIS VALIDATION BLOCK IS NEW ---
            if (!iraApiValidator.isIraNumberValid(request.getIraNumber())) {
                throw new InvalidIraNumberException("The provided IRA number is not valid or could not be verified.");
            }
            // --- END OF VALIDATION BLOCK ---

            Superagent superagent = Superagent.builder()
                    .user(savedUser)
                    .iraNumber(request.getIraNumber())
                    .kraPin(request.getKraPin())
                    .paybillNumber(request.getPaybillNumber())
                    .isVerified(false)
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

    public User getCurrentUser() {
        // Get the principal object from Spring Security's context. This holds the user's identity.
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username;
        if (principal instanceof UserDetails) {
            // The standard case: the principal is a UserDetails object.
            username = ((UserDetails) principal).getUsername();
        } else {
            // A fallback case, though less common in a typical setup.
            username = principal.toString();
        }

        // Use the username (which is the user's email in our app) to fetch the full User entity.
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in security context: " + username));
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