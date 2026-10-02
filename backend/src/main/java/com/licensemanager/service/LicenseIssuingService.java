package com.licensemanager.service;

import com.licensemanager.dto.LicenseRenewalRequest;
import com.licensemanager.dto.LicenseRequest;
import com.licensemanager.model.License;
import com.licensemanager.model.License.LicenseStatus;
import com.licensemanager.model.License.SubscriptionType;
import com.licensemanager.repository.LicenseRepository;
import com.licensemanager.util.LicenseKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LicenseIssuingService {

    private final LicenseRepository licenseRepository;
    private final LicenseEvaluationService licenseEvaluationService;

    @Value("${license.default-grace-period-days:7}")
    private int defaultGracePeriodDays;

    public LicenseIssuingService(LicenseRepository licenseRepository,
                                 LicenseEvaluationService licenseEvaluationService) {
        this.licenseRepository = licenseRepository;
        this.licenseEvaluationService = licenseEvaluationService;
    }

    /**
     * Issues and persists a new license entity.
     * Evaluates custom start dates and assigns PENDING or ACTIVE status.
     */
    public License generateLicense(LicenseRequest request) {
        String licenseKey;
        do {
            licenseKey = LicenseKeyGenerator.generateLicenseKey();
        } while (licenseRepository.existsByLicenseKey(licenseKey));

        License license = new License();
        license.setLicenseKey(licenseKey);
        license.setUsername(request.getUsername());
        license.setEmail(request.getEmail());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveStartDate = (request.getStartDate() != null)
                ? request.getStartDate()
                : now;
        license.setStartDate(effectiveStartDate);

        // Date Resolution: Custom explicit endDate vs standard recurring SubscriptionType
        LocalDateTime expiryDate;
        SubscriptionType resolvedType = request.getSubscriptionType();

        if (request.getEndDate() != null) {
            expiryDate = request.getEndDate();
            if (!expiryDate.isAfter(effectiveStartDate)) {
                throw new IllegalArgumentException("License endDate must be strictly after startDate");
            }
            if (resolvedType == null) {
                resolvedType = SubscriptionType.CUSTOM;
            }
        } else {
            if (resolvedType == null || resolvedType == SubscriptionType.CUSTOM) {
                throw new IllegalArgumentException("Must specify a valid SubscriptionType or an explicit endDate");
            }
            expiryDate = calculateExpiryDate(effectiveStartDate, resolvedType);
        }

        license.setSubscriptionType(resolvedType);
        license.setExpiryDate(expiryDate);

        // Lifecycle State Machine: PENDING if effectiveStartDate is in the future
        if (effectiveStartDate.isAfter(now)) {
            license.setStatus(LicenseStatus.PENDING);
            license.setPremiumFeaturesEnabled(false);
            license.setWriteAccessEnabled(false);
        } else {
            license.setStatus(LicenseStatus.ACTIVE);
            license.setPremiumFeaturesEnabled(true);
            license.setWriteAccessEnabled(true);
        }

        license.setMaxActivations(request.getMaxActivations() != null ? request.getMaxActivations() : 1);
        license.setGracePeriodDays(request.getGracePeriodDays() != null ? request.getGracePeriodDays() : defaultGracePeriodDays);
        license.setCreatedAt(now);
        license.setUpdatedAt(now);

        return licenseRepository.save(license);
    }

    /**
     * Extends a license validity while preserving the same key,
     * resetting alert counters, and immediately syncing service_registry.json.
     */
    public License renewLicense(LicenseRenewalRequest request) {
        License license = licenseRepository.findByLicenseKey(request.getLicenseKey())
                .orElseThrow(() -> new RuntimeException("License not found with key: " + request.getLicenseKey()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiryDate;
        SubscriptionType resolvedType = license.getSubscriptionType();

        if (request.getNewEndDate() != null) {
            newExpiryDate = request.getNewEndDate();
            if (!newExpiryDate.isAfter(now)) {
                throw new IllegalArgumentException("Renewal newEndDate must be in the future");
            }
            if (request.getSubscriptionType() != null) {
                resolvedType = request.getSubscriptionType();
            }
        } else {
            SubscriptionType typeToUse = (request.getSubscriptionType() != null)
                    ? request.getSubscriptionType()
                    : license.getSubscriptionType();

            if (typeToUse == SubscriptionType.CUSTOM) {
                throw new IllegalArgumentException("Cannot renew a CUSTOM tier without specifying newEndDate");
            }
            resolvedType = typeToUse;

            if (license.isFullyExpired() || license.isInGracePeriod() || license.getStatus() == LicenseStatus.EXPIRED) {
                newExpiryDate = calculateExpiryDate(now, typeToUse);
            } else {
                newExpiryDate = calculateExpiryDate(license.getExpiryDate(), typeToUse);
            }
        }

        license.setExpiryDate(newExpiryDate);
        license.setSubscriptionType(resolvedType);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setNotifiedExpiringSoon(false);
        license.setNotificationsSentDuringGrace(0);
        license.setLastNotificationSent(null);
        license.setPremiumFeaturesEnabled(true);
        license.setWriteAccessEnabled(true);
        license.setUpdatedAt(now);

        License savedLicense = licenseRepository.save(license);

        // Immediate Cluster Registry Sync for Kubernetes pods
        licenseEvaluationService.evaluateAndSyncToRegistry();

        return savedLicense;
    }

    public License suspendLicense(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new RuntimeException("License not found with key: " + licenseKey));

        license.setStatus(LicenseStatus.SUSPENDED);
        license.setUpdatedAt(LocalDateTime.now());
        License saved = licenseRepository.save(license);
        licenseEvaluationService.evaluateAndSyncToRegistry();
        return saved;
    }

    public License activateLicense(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new RuntimeException("License not found with key: " + licenseKey));

        if (!license.isFullyExpired()) {
            if (license.isInGracePeriod()) {
                license.setStatus(LicenseStatus.GRACE_PERIOD);
            } else if (!license.isExpired()) {
                license.setStatus(LicenseStatus.ACTIVE);
            }
            license.setUpdatedAt(LocalDateTime.now());
        }
        License saved = licenseRepository.save(license);
        licenseEvaluationService.evaluateAndSyncToRegistry();
        return saved;
    }

    public License revokeLicense(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new RuntimeException("License not found with key: " + licenseKey));

        license.setStatus(LicenseStatus.REVOKED);
        license.setUpdatedAt(LocalDateTime.now());
        License saved = licenseRepository.save(license);
        licenseEvaluationService.evaluateAndSyncToRegistry();
        return saved;
    }

    public void deleteLicense(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new RuntimeException("License not found with key: " + licenseKey));
        licenseRepository.delete(license);
        licenseEvaluationService.evaluateAndSyncToRegistry();
    }

    public Optional<License> getLicenseByKey(String licenseKey) {
        return licenseRepository.findByLicenseKey(licenseKey);
    }

    public List<License> getAllLicenses() {
        return licenseRepository.findAll();
    }

    private LocalDateTime calculateExpiryDate(LocalDateTime anchorDate, SubscriptionType type) {
        return switch (type) {
            case MONTHLY -> anchorDate.plusMonths(1);
            case QUARTERLY -> anchorDate.plusMonths(3);
            case YEARLY -> anchorDate.plusYears(1);
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM tier requires an explicit endDate");
        };
    }
}