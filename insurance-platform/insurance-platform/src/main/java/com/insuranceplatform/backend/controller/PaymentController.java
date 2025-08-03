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

    // This is the public callback URL Safaricom would call.
    // We will call it ourselves for simulation.
    @PostMapping("/mpesa-callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody MpesaCallbackRequest callback) {
        mpesaService.processMpesaCallback(callback);
        return ResponseEntity.ok().build();
    }
}