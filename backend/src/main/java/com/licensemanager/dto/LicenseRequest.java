package com.licensemanager.dto;

import com.licensemanager.model.License.SubscriptionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private SubscriptionType subscriptionType;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer maxActivations;

    private Integer gracePeriodDays;
}