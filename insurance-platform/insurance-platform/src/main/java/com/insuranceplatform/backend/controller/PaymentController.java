package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.MpesaCallbackRequest;
import com.insuranceplatform.backend.dto.StkPushRequest;
import com.insuranceplatform.backend.service.MpesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping; 
import org.springframework.web.bind.annotation.PathVariable;
import com.insuranceplatform.backend.dto.PaybillDetailsDto;
import com.insuranceplatform.backend.dto.B2CResultCallback;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final MpesaService mpesaService;

    // This endpoint would be called by the Agent's frontend
    @PostMapping("/stk-push")
    public ResponseEntity<String> stkPush(@RequestBody StkPushRequest request) {
        String response = mpesaService.initiateStkPush(request);
        return ResponseEntity.ok(response);
    }
     @GetMapping("/policy/{policyId}/paybill-details")
    public ResponseEntity<PaybillDetailsDto> getPaybillDetails(@PathVariable Long policyId) {
        PaybillDetailsDto details = mpesaService.getPaybillDetails(policyId);
        return ResponseEntity.ok(details);
    }
    @PostMapping("/b2c-result")
    public ResponseEntity<Void> b2cResultCallback(@RequestBody B2CResultCallback callback) {
        mpesaService.processB2CResultCallback(callback);
        return ResponseEntity.ok().build();
    }

    /**
     * Public callback URL for Safaricom to post if a B2C transaction times out.
     */
    @PostMapping("/b2c-timeout")
    public ResponseEntity<Void> b2cTimeoutCallback(@RequestBody Object timeoutPayload) {
        // Log the timeout payload and handle it (e.g., mark transaction as timed out)
        System.err.println("Received B2C Timeout: " + timeoutPayload.toString());
        return ResponseEntity.ok().build();
    }

    // This is the public callback URL Safaricom would call.
    // We will call it ourselves for simulation.
    @PostMapping("/mpesa-callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody MpesaCallbackRequest callback) {
        mpesaService.processMpesaCallback(callback);
        return ResponseEntity.ok().build();
    }
}