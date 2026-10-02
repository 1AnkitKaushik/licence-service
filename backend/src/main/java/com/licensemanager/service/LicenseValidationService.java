package com.licensemanager.service;

import com.licensemanager.dto.LicenseValidationRequest;
import com.licensemanager.dto.LicenseValidationResponse;
import com.licensemanager.model.License;
import com.licensemanager.model.License.AccessLevel;
import com.licensemanager.model.License.LicenseStatus;
import com.licensemanager.model.LicenseValidationLog;
import com.licensemanager.model.LicenseValidationLog.ValidationResult;
import com.licensemanager.model.LicenseValidationLog.ValidationType;
import com.licensemanager.repository.LicenseRepository;
import com.licensemanager.repository.LicenseValidationLogRepository;
import com.licensemanager.util.LicenseKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LicenseValidationService {

    private final LicenseRepository licenseRepository;
    private final LicenseValidationLogRepository validationLogRepository;
    private final LicenseFileService licenseFileService;

    @Value("${license.expiry-warning-days:14}")
    private int expiryWarningDays;

    public LicenseValidationService(LicenseRepository licenseRepository,
                                  LicenseValidationLogRepository validationLogRepository,
                                  LicenseFileService licenseFileService) {
        this.licenseRepository = licenseRepository;
        this.validationLogRepository = validationLogRepository;
        this.licenseFileService = licenseFileService;
    }

    public LicenseValidationResponse validateLicense(LicenseValidationRequest request, String ipAddress) {
        return validateLicenseInternal(request, ipAddress, ValidationType.ONLINE);
    }

    public LicenseValidationResponse validateLicenseOffline(LicenseValidationRequest request) {
        if (!LicenseKeyGenerator.isValidFormat(request.getLicenseKey())) {
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Invalid license key format")
                    .accessLevel(AccessLevel.NONE)
                    .build();
        }
        return validateLicenseInternal(request, "offline", ValidationType.OFFLINE);
    }

    public LicenseFileService.OfflineLicenseValidationResult validateLicenseFile(String fileContent) {
        return licenseFileService.validateLicenseFile(fileContent);
    }

    private LicenseValidationResponse validateLicenseInternal(LicenseValidationRequest request,
                                                              String ipAddress,
                                                              ValidationType validationType) {
        Optional<License> licenseOpt = licenseRepository.findByLicenseKey(request.getLicenseKey());

        // 1. Key Existence Check
        if (licenseOpt.isEmpty()) {
            logValidation(request.getLicenseKey(), null, validationType,
                    ValidationResult.INVALID, "License key not found", ipAddress, request.getMachineId());
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Invalid license key")
                    .licenseKey(request.getLicenseKey())
                    .accessLevel(AccessLevel.NONE)
                    .build();
        }

        License license = licenseOpt.get();

        // 2. Administrative Suspended / Revoked Checks
        if (license.getStatus() == LicenseStatus.SUSPENDED) {
            logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                    ValidationResult.SUSPENDED, "License is suspended", ipAddress, request.getMachineId());
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("License is suspended")
                    .licenseKey(license.getLicenseKey())
                    .status(license.getStatus())
                    .accessLevel(AccessLevel.NONE)
                    .build();
        }

        if (license.getStatus() == LicenseStatus.REVOKED) {
            logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                    ValidationResult.FAILED, "License has been revoked", ipAddress, request.getMachineId());
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("License has been revoked")
                    .licenseKey(license.getLicenseKey())
                    .status(license.getStatus())
                    .accessLevel(AccessLevel.NONE)
                    .build();
        }

        // 3. Lower Bound Guard: PENDING / Future Start Date
        if (license.getStatus() == LicenseStatus.PENDING || license.isNotYetActive()) {
            logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                    ValidationResult.INVALID, "License is not yet active", ipAddress, request.getMachineId());
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("License is not yet active. Valid from: " + license.getStartDate())
                    .licenseKey(license.getLicenseKey())
                    .status(license.getStatus())
                    .subscriptionType(license.getSubscriptionType())
                    .startDate(license.getStartDate())
                    .expiryDate(license.getExpiryDate())
                    .accessLevel(AccessLevel.NONE)
                    .premiumFeaturesEnabled(false)
                    .writeAccessEnabled(false)
                    .build();
        }

        // 4. Hard Expiration Check (Past Grace Period)
        if (license.isFullyExpired()) {
            if (license.getStatus() != LicenseStatus.EXPIRED) {
                license.setStatus(LicenseStatus.EXPIRED);
                license.setPremiumFeaturesEnabled(false);
                license.setWriteAccessEnabled(false);
                license.setUpdatedAt(LocalDateTime.now());
                licenseRepository.save(license);
            }

            logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                    ValidationResult.EXPIRED, "License and grace period have expired", ipAddress, request.getMachineId());
            return LicenseValidationResponse.builder()
                    .valid(false)
                    .message("License and grace period have expired. Please renew your subscription.")
                    .licenseKey(license.getLicenseKey())
                    .status(LicenseStatus.EXPIRED)
                    .subscriptionType(license.getSubscriptionType())
                    .startDate(license.getStartDate())
                    .expiryDate(license.getExpiryDate())
                    .graceEndDate(license.getGraceEndDate())
                    .accessLevel(AccessLevel.NONE)
                    .premiumFeaturesEnabled(false)
                    .writeAccessEnabled(false)
                    .build();
        }

        // 5. Grace Period Buffer (Degraded / Limited Access)
        if (license.isInGracePeriod()) {
            if (license.getStatus() != LicenseStatus.GRACE_PERIOD) {
                license.setStatus(LicenseStatus.GRACE_PERIOD);
                license.setWriteAccessEnabled(false);
                license.setUpdatedAt(LocalDateTime.now());
                licenseRepository.save(license);
            }

            int daysUntilGraceEnd = license.getDaysUntilGraceEnd();
            String warningMessage = String.format(
                    "GRACE PERIOD: License expired. You have %d days of limited access remaining. " +
                    "Write operations and premium features are disabled. Please renew immediately.",
                    daysUntilGraceEnd
            );

            logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                    ValidationResult.SUCCESS, "License in grace period - limited access", ipAddress, request.getMachineId());

            return LicenseValidationResponse.builder()
                    .valid(true)
                    .message("License in grace period - limited access")
                    .licenseKey(license.getLicenseKey())
                    .status(LicenseStatus.GRACE_PERIOD)
                    .subscriptionType(license.getSubscriptionType())
                    .startDate(license.getStartDate())
                    .expiryDate(license.getExpiryDate())
                    .graceEndDate(license.getGraceEndDate())
                    .gracePeriodDays(license.getGracePeriodDays())
                    .daysUntilExpiry(0)
                    .daysUntilGraceEnd(daysUntilGraceEnd)
                    .inGracePeriod(true)
                    .warningMessage(warningMessage)
                    .accessLevel(AccessLevel.LIMITED)
                    .premiumFeaturesEnabled(false)
                    .writeAccessEnabled(false)
                    .build();
        }

        // 6. Active & Valid (Full Access)
        logValidation(license.getLicenseKey(), license.getUserId(), validationType,
                ValidationResult.SUCCESS, "License is valid", ipAddress, request.getMachineId());

        int daysUntilExpiry = license.getDaysUntilExpiry();
        boolean expiringSoon = license.isExpiringSoon(expiryWarningDays);
        String warningMessage = expiringSoon
                ? String.format("Your license will expire in %d days. Please renew soon.", daysUntilExpiry)
                : null;

        return LicenseValidationResponse.builder()
                .valid(true)
                .message("License is valid")
                .licenseKey(license.getLicenseKey())
                .status(license.getStatus())
                .subscriptionType(license.getSubscriptionType())
                .startDate(license.getStartDate())
                .expiryDate(license.getExpiryDate())
                .graceEndDate(license.getGraceEndDate())
                .gracePeriodDays(license.getGracePeriodDays())
                .daysUntilExpiry(daysUntilExpiry)
                .daysUntilGraceEnd(daysUntilExpiry + license.getGracePeriodDays())
                .expiringSoon(expiringSoon)
                .inGracePeriod(false)
                .warningMessage(warningMessage)
                .accessLevel(AccessLevel.FULL)
                .premiumFeaturesEnabled(license.isPremiumFeaturesEnabled())
                .writeAccessEnabled(license.isWriteAccessEnabled())
                .build();
    }

    private void logValidation(String licenseKey, String userId, ValidationType type,
                              ValidationResult result, String message, String ipAddress, String machineId) {
        LicenseValidationLog log = new LicenseValidationLog();
        log.setLicenseKey(licenseKey);
        log.setUserId(userId);
        log.setValidationType(type);
        log.setResult(result);
        log.setMessage(message);
        log.setIpAddress(ipAddress);
        log.setMachineId(machineId);
        validationLogRepository.save(log);
    }

    public List<LicenseValidationLog> getValidationLogs() {
        return validationLogRepository.findTop100ByOrderByValidatedAtDesc();
    }

    public List<LicenseValidationLog> getValidationLogsByLicense(String licenseKey) {
        return validationLogRepository.findByLicenseKey(licenseKey);
    }
}