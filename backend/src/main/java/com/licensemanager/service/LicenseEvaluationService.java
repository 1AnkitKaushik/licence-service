package com.licensemanager.service;

import com.licensemanager.model.License;
import com.licensemanager.repository.LicenseRepository;
import com.licenseservice.model.ActionRequired;
import com.licenseservice.model.LicenseState;
import com.licenseservice.model.ServiceRegistryEntry;
import com.licenseservice.model.ServiceState;
import com.licenseservice.service.LicenseRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LicenseEvaluationService {

    

    private final LicenseRepository licenseRepository;
    private final LicenseRegistryService licenseRegistryService;

    @Value("${registry.grace-period-days:15}")
    private int defaultGracePeriodDays;

    public LicenseEvaluationService(LicenseRepository licenseRepository, LicenseRegistryService licenseRegistryService) {
        this.licenseRepository = licenseRepository;
        this.licenseRegistryService = licenseRegistryService;
    }

    public List<ServiceRegistryEntry> evaluateAndSyncToRegistry() {
        LocalDate today = LocalDate.now();
        LocalDateTime nowTime = LocalDateTime.now();
        List<License> allLicenses = licenseRepository.findAll();
        List<ServiceRegistryEntry> entries = new ArrayList<>();

        for (License lic : allLicenses) {
            String serviceId = (lic.getUserId() != null && !lic.getUserId().isBlank()) 
                    ? lic.getUserId() 
                    : (lic.getId() != null ? lic.getId() : "svc-unknown");

            String serviceName = (lic.getUsername() != null && !lic.getUsername().isBlank()) 
                    ? lic.getUsername() 
                    : "UnknownService";

            String licenseKey = lic.getLicenseKey() != null ? lic.getLicenseKey() : "N/A";
            String policy = lic.getSubscriptionType() != null ? lic.getSubscriptionType().name() : "CUSTOM";
            
            LocalDate expiry = lic.getExpiryDate() != null ? lic.getExpiryDate().toLocalDate() : today;
            int graceDays = lic.getGracePeriodDays() > 0 ? lic.getGracePeriodDays() : defaultGracePeriodDays;
            LocalDate graceEnd = expiry.plusDays(graceDays);

            LicenseState licenseState;
            ActionRequired action;
            ServiceState serviceState;

            if (today.isBefore(expiry)) {
                licenseState = LicenseState.ACTIVE;
                action = ActionRequired.NONE;
                serviceState = ServiceState.RUNNING;
            } else if (!today.isAfter(graceEnd)) {
                licenseState = LicenseState.GRACE_PERIOD;
                action = ActionRequired.WARN;
                serviceState = ServiceState.RUNNING;
            } else {
                licenseState = LicenseState.EXPIRED;
                action = ActionRequired.SHUTDOWN;
                serviceState = ServiceState.SHUTDOWN;
            }

            ServiceRegistryEntry entry = new ServiceRegistryEntry();
            entry.setServiceId(serviceId);
            entry.setServiceName(serviceName);
            entry.setLicenseKey(licenseKey);
            entry.setPolicy(policy);
            entry.setState(serviceState);
            entry.setLicenseState(licenseState);
            entry.setAction(action);
            entry.setLicenseExpiryDate(expiry);
            entry.setGracePeriodEndDate(graceEnd);
            entry.setLastCheckedAt(nowTime);

            entries.add(entry);
        }

        return licenseRegistryService.updateAll(entries);
    }
}