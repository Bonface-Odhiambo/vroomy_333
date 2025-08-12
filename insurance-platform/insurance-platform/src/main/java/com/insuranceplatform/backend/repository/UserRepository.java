package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.enums.UserRole; // CORRECTED: Import the UserRole enum
import com.insuranceplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Your existing methods (unchanged) ---
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // Method to find a user by their email address
    Optional<User> findByEmail(String email);

    // We can add more finders later if needed, e.g., by phone
    Optional<User> findByPhone(String phone);


    // --- NEW METHOD WITH CORRECTED TYPE ---

    /**
     * Finds all users that match a specific role.
     * This is required by the UserService to filter users for the admin dashboard.
     * Spring Data JPA will automatically generate the query based on the method name.
     *
     * @param role The UserRole enum (e.g., UserRole.AGENT, UserRole.SUPERAGENT) to search for.
     * @return A list of users matching the specified role.
     */
    // CORRECTED: The method parameter is now UserRole
    List<User> findByRole(UserRole role);
    Optional<User> findByPasswordResetToken(String token);
}