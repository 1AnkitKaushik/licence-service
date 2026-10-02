package com.licensemanager.dto;

import com.licensemanager.model.License.SubscriptionType;
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
public class LicenseRenewalRequest {

    @NotBlank(message = "License key is required")
    private String licenseKey;

    private SubscriptionType subscriptionType;

    private LocalDateTime newEndDate;
}