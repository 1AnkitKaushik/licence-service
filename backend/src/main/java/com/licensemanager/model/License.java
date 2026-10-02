package com.licensemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "licenses")
public class License {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String licenseKey;
    
    private String userId;
    
    private String username;
    
    private String email;
    
    private SubscriptionType subscriptionType;
    
    private LocalDateTime startDate;
    
    private LocalDateTime expiryDate;
    
    private LicenseStatus status = LicenseStatus.ACTIVE;
    
    private boolean notifiedExpiringSoon = false;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private String machineId;
    
    private int maxActivations = 1;
    
    private int currentActivations = 0;
    
    // Grace period configuration
    private int gracePeriodDays = 7;
    
    private LocalDateTime lastNotificationSent;
    
    private int notificationsSentDuringGrace = 0;
    
    // Feature flags for the license
    private boolean premiumFeaturesEnabled = true;
    
    private boolean writeAccessEnabled = true;
    
    public enum SubscriptionType {
        MONTHLY, 
        QUARTERLY, 
        YEARLY, 
        CUSTOM
    }
    
    public enum LicenseStatus {
        PENDING,
        ACTIVE, 
        GRACE_PERIOD,
        EXPIRED, 
        SUSPENDED, 
        REVOKED
    }
    
    public enum AccessLevel {
        FULL,           // All features enabled
        LIMITED,        // Read-only, core features only (during grace period)
        NONE            // No access (pending, suspended, revoked, or fully expired)
    }

    // Checks if the license start timestamp is in the future
    public boolean isNotYetActive() {
        return startDate != null && LocalDateTime.now().isBefore(startDate);
    }
    
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }
    
    public boolean isInGracePeriod() {
        if (!isExpired()) return false;
        LocalDateTime graceEndDate = getGraceEndDate();
        LocalDateTime now = LocalDateTime.now();
        return (now.isEqual(expiryDate) || now.isAfter(expiryDate)) && now.isBefore(graceEndDate);
    }
    
    public boolean isFullyExpired() {
        if (expiryDate == null) return false;
        LocalDateTime graceEndDate = getGraceEndDate();
        return LocalDateTime.now().isAfter(graceEndDate);
    }
    
    public LocalDateTime getGraceEndDate() {
        return expiryDate != null ? expiryDate.plusDays(gracePeriodDays) : null;
    }
    
    public boolean isExpiringSoon(int days) {
        if (expiryDate == null) return false;
        LocalDateTime warningDate = expiryDate.minusDays(days);
        return LocalDateTime.now().isAfter(warningDate) && !isExpired();
    }
    
    public int getDaysUntilExpiry() {
        if (expiryDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }
    
    public int getDaysUntilGraceEnd() {
        LocalDateTime graceEnd = getGraceEndDate();
        if (graceEnd == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), graceEnd);
    }
    
    public AccessLevel getAccessLevel() {
        // Block if suspended, revoked, pending, or set to start in the future
        if (status == LicenseStatus.SUSPENDED || status == LicenseStatus.REVOKED || status == LicenseStatus.PENDING) {
            return AccessLevel.NONE;
        }
        if (isNotYetActive()) {
            return AccessLevel.NONE;
        }
        if (isFullyExpired()) {
            return AccessLevel.NONE;
        }
        if (isInGracePeriod() || status == LicenseStatus.GRACE_PERIOD) {
            return AccessLevel.LIMITED;
        }
        if (status == LicenseStatus.ACTIVE && !isExpired()) {
            return AccessLevel.FULL;
        }
        return AccessLevel.NONE;
    }
}