package com.insuranceplatform.backend.dto;

import lombok.Data;

@Data
public class StkPushRequest {
    private Long policyId;
    private String phoneNumber; // Phone to receive the STK push
}
