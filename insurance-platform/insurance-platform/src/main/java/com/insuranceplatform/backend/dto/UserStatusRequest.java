package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.UserStatus;
import lombok.Data;

@Data
public class UserStatusRequest {
    private UserStatus status;
}