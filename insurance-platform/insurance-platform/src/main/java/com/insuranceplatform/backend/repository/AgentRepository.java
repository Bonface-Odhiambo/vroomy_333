package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.entity.Superagent; 
import com.insuranceplatform.backend.entity.User;
import java.util.List; 
import java.util.Optional; 

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findBySuperagent(Superagent superagent);
    Optional<Agent> findByUser(User user);
    long countBySuperagent(Superagent superagent);
}