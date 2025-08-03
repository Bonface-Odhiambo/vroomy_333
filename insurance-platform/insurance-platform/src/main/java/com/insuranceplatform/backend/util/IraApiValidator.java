package com.insuranceplatform.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IraApiValidator {

    /**
     * Mocks the validation of an IRA number against an external API.
     * In a real application, this method would use RestTemplate or WebClient
     * to call the actual IRA API endpoint.
     *
     * For our simulation:
     * - Any IRA number starting with "VALID" will be considered valid.
     * - Any other IRA number will be considered invalid.
     *
     * @param iraNumber The IRA number to validate.
     * @return true if the number is valid, false otherwise.
     */
    public boolean isIraNumberValid(String iraNumber) {
        log.info("Simulating IRA API validation for number: {}", iraNumber);

        // TODO: Replace this mock logic with a real API call when going live.
        // Example of a real call might look like:
        // try {
        //     ResponseEntity<String> response = restTemplate.getForEntity("https://api.ira.go.ke/validate?number=" + iraNumber, String.class);
        //     return response.getStatusCode() == HttpStatus.OK;
        // } catch (HttpClientErrorException e) {
        //     return false;
        // }

        if (iraNumber != null && iraNumber.toUpperCase().startsWith("VALID")) {
            log.info("Mock validation PASSED for IRA number: {}", iraNumber);
            return true;
        } else {
            log.info("Mock validation FAILED for IRA number: {}", iraNumber);
            return false;
        }
    }
}