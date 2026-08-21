package com.licenseservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ServiceRegistryEntry {
    private String serviceId;
    private String serviceName;
    private String licenseKey;
    private String policy;
    private ServiceState state;
    private LicenseState licenseState;
    private ActionRequired action;
    private LocalDate licenseExpiryDate;
    private LocalDate gracePeriodEndDate;
    private LocalDateTime lastCheckedAt;

    public ServiceRegistryEntry() {}

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }

    public ServiceState getState() { return state; }
    public void setState(ServiceState state) { this.state = state; }

    public LicenseState getLicenseState() { return licenseState; }
    public void setLicenseState(LicenseState licenseState) { this.licenseState = licenseState; }

    public ActionRequired getAction() { return action; }
    public void setAction(ActionRequired action) { this.action = action; }

    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) { this.licenseExpiryDate = licenseExpiryDate; }

    public LocalDate getGracePeriodEndDate() { return gracePeriodEndDate; }
    public void setGracePeriodEndDate(LocalDate gracePeriodEndDate) { this.gracePeriodEndDate = gracePeriodEndDate; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
}
