package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.SharedTransactionDto;
import com.insuranceplatform.backend.entity.Policy;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSharingService {

    private final PolicyRepository policyRepository;

    public List<SharedTransactionDto> getPaidPolicyTransactions() {
        List<Policy> paidPolicies = policyRepository.findAll(); // In a real app, you'd filter this further
        return paidPolicies.stream()
                .filter(policy -> policy.getStatus() == PolicyStatus.PAID)
                .map(policy -> new SharedTransactionDto(
                        policy.getId(),
                        policy.getProduct().getName(),
                        policy.getTotalAmount(),
                        policy.getPaidAt(),
                        policy.getAgent().getUser().getFullName(),
                        policy.getAgent().getSuperagent().getUser().getFullName()
                ))
                .collect(Collectors.toList());
    }
}