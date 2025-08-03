package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // Method to find a user by their email address
    Optional<User> findByEmail(String email);
    
    // We can add more finders later if needed, e.g., by phone
    Optional<User> findByPhone(String phone);
}