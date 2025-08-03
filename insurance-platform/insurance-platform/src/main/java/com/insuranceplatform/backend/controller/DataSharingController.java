package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.SharedTransactionDto;
import com.insuranceplatform.backend.service.DataSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/data-sharing")
@RequiredArgsConstructor
public class DataSharingController {

    private final DataSharingService dataSharingService;

    @GetMapping("/transactions")
    public ResponseEntity<List<SharedTransactionDto>> getTransactions() {
        return ResponseEntity.ok(dataSharingService.getPaidPolicyTransactions());
    }
}