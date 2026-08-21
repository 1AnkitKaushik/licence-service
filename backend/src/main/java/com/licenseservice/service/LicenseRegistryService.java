package com.licenseservice.service;

import com.licensemanager.model.License;
import com.licensemanager.repository.LicenseRepository;
import com.licenseservice.dto.K8sServiceActionResponse;
import com.licenseservice.model.ActionRequired;
import com.licenseservice.model.LicenseState;
import com.licenseservice.model.ServiceRegistryEntry;
import com.licenseservice.model.ServiceState;
import com.licenseservice.repository.FileRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LicenseRegistryService {

    private static final Logger log = LoggerFactory.getLogger(LicenseRegistryService.class);

    private final LicenseRepository licenseRepository;
    private final FileRegistryRepository fileRegistryRepository;

    @Value("${registry.grace-period-days:15}")
    private int defaultGracePeriodDays;

    public LicenseRegistryService(LicenseRepository licenseRepository, FileRegistryRepository fileRegistryRepository) {
        this.licenseRepository = licenseRepository;
        this.fileRegistryRepository = fileRegistryRepository;
    }

    public List<ServiceRegistryEntry> syncAndEvaluateAll() {
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
            
            LocalDate expiry;
            if (lic.getExpiryDate() != null) {
                expiry = lic.getExpiryDate().toLocalDate();
            } else {
                expiry = today;
            }

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

        fileRegistryRepository.saveAll(entries);
        log.info("Successfully evaluated and synced {} service entries to registry file.", entries.size());
        return entries;
    }

    public List<K8sServiceActionResponse> getK8sServiceActions() {
        List<ServiceRegistryEntry> entries = fileRegistryRepository.findAll();
        return entries.stream()
                .map(e -> new K8sServiceActionResponse(
                        e.getServiceId(),
                        e.getServiceName(),
                        e.getLicenseState().name(),
                        e.getAction().name()
                ))
                .collect(Collectors.toList());
    }

    public List<K8sServiceActionResponse> getK8sActionRegistry() {
        return getK8sServiceActions();
    }

    public List<ServiceRegistryEntry> getAllRegistryEntries() {
        return fileRegistryRepository.findAll();
    }
}
