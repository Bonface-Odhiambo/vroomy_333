package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.UserDto;
import com.insuranceplatform.backend.enums.UserRole; // CORRECTED: Using UserRole enum from your enums package
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for handling general user-related operations.
 * This service is primarily used by the AdminController to view user data securely.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves a list of all users, with an option to filter by role.
     * Converts the User entities to UserDto objects to prevent exposing sensitive data like passwords.
     *
     * @param roleStr An optional string representing the role to filter by (e.g., "AGENT"). Case-insensitive.
     * @return A list of UserDto objects.
     * @throws IllegalArgumentException if the provided role string is invalid.
     */
    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers(String roleStr) {
        List<User> users;

        if (roleStr == null || roleStr.isBlank()) {
            // If no role is specified, return all users
            users = userRepository.findAll();
        } else {
            try {
                // Convert the string to the UserRole enum
                UserRole role = UserRole.valueOf(roleStr.toUpperCase());
                // Fetch users from the repository using the corresponding method
                users = userRepository.findByRole(role);
            } catch (IllegalArgumentException e) {
                // This handles cases where an invalid role (e.g., "GUEST") is passed
                throw new IllegalArgumentException("Invalid role specified: " + roleStr);
            }
        }

        // Convert the list of User entities to a list of UserDto objects
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds a single user by their ID and converts them to a DTO for safe exposure via the API.
     *
     * @param userId The ID of the user to find.
     * @return The UserDto object.
     * @throws ResourceNotFoundException if no user is found with the given ID.
     */
    @Transactional(readOnly = true)
    public UserDto findUserDtoById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return convertToDto(user);
    }

    /**
     * Finds a single user entity by its ID. Used internally by other services that need the full entity.
     *
     * @param userId The ID of the user to find.
     * @return The full User entity.
     * @throws ResourceNotFoundException if no user is found with the given ID.
     */
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }


    /**
     * A private helper method to map a User entity to a UserDto.
     * This ensures consistent data mapping and hides sensitive information.
     *
     * @param user The User entity to convert.
     * @return A UserDto object.
     */
    private UserDto convertToDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),      // Convert UserRole enum to string
                user.getStatus().name()     // Convert UserStatus enum to string
        );
    }
}