package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperagentRepository extends JpaRepository<Superagent, Long> {
    Optional<Superagent> findByUser(User user);
}