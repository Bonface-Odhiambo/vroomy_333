package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.UserRole;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // We keep this for JSON deserialization
public class RegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private UserRole role;
    // Superagent-specific fields, optional
    private String iraNumber;
    private String kraPin;
    // Agent-specific fields, optional
    private Long superagentId;

    // This is the only constructor that takes all arguments now
    public RegisterRequest(String fullName, String email, String phone, String password, UserRole role, String iraNumber, String kraPin, Long superagentId) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.iraNumber = iraNumber;
        this.kraPin = kraPin;
        this.superagentId = superagentId;
    }
}