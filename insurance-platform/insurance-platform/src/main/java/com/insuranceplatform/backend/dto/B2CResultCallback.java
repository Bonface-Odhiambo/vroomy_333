package com.insuranceplatform.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// This represents the entire payload from Safaricom
public record B2CResultCallback(
    @JsonProperty("Result")
    B2CResult Result
) {
    // This represents the main 'Result' object
    public record B2CResult(
        @JsonProperty("ResultCode") int resultCode,
        @JsonProperty("ResultDesc") String resultDesc,
        @JsonProperty("OriginatorConversationID") String originatorConversationID,
        @JsonProperty("ConversationID") String conversationID,
        @JsonProperty("TransactionID") String transactionID,
        @JsonProperty("ResultParameters") ResultParameters resultParameters
    ) {}

    // This holds the detailed parameters of the result
    public record ResultParameters(
        @JsonProperty("ResultParameter")
        List<Map<String, Object>> resultParameter
    ) {}
}