package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.AuthResponse;
import com.insuranceplatform.backend.dto.RegisterRequest;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.entity.Wallet;
import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.repository.AgentRepository;
import com.insuranceplatform.backend.repository.SuperagentRepository;
import com.insuranceplatform.backend.repository.UserRepository;
import com.insuranceplatform.backend.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito for this test class
public class AuthServiceTest {

    // These are our "fake" dependencies
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SuperagentRepository superagentRepository;
    @Mock
    private AgentRepository agentRepository;


    // This is the class we are actually testing
    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Create a sample request object to use in our tests
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("testagent@example.com");
        registerRequest.setFullName("Test Agent");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("1234567890");
        registerRequest.setRole(UserRole.AGENT);
        registerRequest.setSuperagentId(1L);
    }

    @Test
    void whenRegisterUser_thenUserAndWalletAreSaved() {
        // --- 1. Arrange (Setup the test conditions) ---
        String fakeToken = "fake-jwt-token";
        String encodedPassword = "encodedPassword123";

        // Tell our mocks what to do when their methods are called
        when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the user that was passed in
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn(fakeToken);
        // We need to mock the findById for the superagent lookup
        when(superagentRepository.findById(1L)).thenReturn(java.util.Optional.of(new com.insuranceplatform.backend.entity.Superagent()));


        // --- 2. Act (Call the method we want to test) ---
        AuthResponse response = authService.register(registerRequest);


        // --- 3. Assert (Check if the results are correct) ---
        assertNotNull(response);
        assertEquals(fakeToken, response.getToken());

        // Verify that the save methods were called exactly once
        verify(userRepository, times(1)).save(any(User.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(agentRepository, times(1)).save(any(com.insuranceplatform.backend.entity.Agent.class));
    }
}