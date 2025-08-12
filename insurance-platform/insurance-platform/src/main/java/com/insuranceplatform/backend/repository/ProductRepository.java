package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.entity.Superagent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySuperagent(Superagent superagent);
}