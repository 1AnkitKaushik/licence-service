package com.licenseservice.service;

import com.licenseservice.model.*;
import com.licenseservice.repository.FileRegistryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LicenseRegistryService {

    private final FileRegistryRepository repository;

    @Value("${registry.grace-period-days:15}")
    private int gracePeriodDays;

    public LicenseRegistryService(FileRegistryRepository repository) {
        this.repository = repository;
    }

    public void evaluateAllLicenses() {
        LocalDate today = LocalDate.now();
        List<ServiceRegistryEntry> entries = repository.findAll();

        System.out.println("🔄 Running hourly License Registry Evaluation at " + LocalDateTime.now());

        for (ServiceRegistryEntry entry : entries) {
            LocalDate expiry = entry.getLicenseExpiryDate();
            
            if (entry.getGracePeriodEndDate() == null && expiry != null) {
                entry.setGracePeriodEndDate(expiry.plusDays(gracePeriodDays));
            }

            if (expiry != null) {
                if (today.isBefore(expiry) || today.isEqual(expiry)) {
                    entry.setLicenseState(LicenseState.ACTIVE);
                    entry.setState(ServiceState.RUNNING);
                    entry.setAction(ActionRequired.NONE);
                } else if (today.isAfter(expiry) && (today.isBefore(entry.getGracePeriodEndDate()) || today.isEqual(entry.getGracePeriodEndDate()))) {
                    entry.setLicenseState(LicenseState.GRACE_PERIOD);
                    entry.setState(ServiceState.DEGRADED);
                    entry.setAction(ActionRequired.WARN);
                } else {
                    entry.setLicenseState(LicenseState.EXPIRED);
                    entry.setState(ServiceState.SHUTDOWN);
                    entry.setAction(ActionRequired.SHUTDOWN);
                }
            }

            entry.setLastCheckedAt(LocalDateTime.now());
            repository.save(entry);
        }

        System.out.println("✅ License Registry updated successfully.");
    }

    public List<K8sServiceActionResponse> getK8sActionList() {
        return repository.findAll().stream()
            .map(entry -> new K8sServiceActionResponse(
                entry.getServiceId(),
                entry.getServiceName(),
                entry.getLicenseState(),
                entry.getAction()
            ))
            .collect(Collectors.toList());
    }

    public void registerService(ServiceRegistryEntry entry) {
        if (entry.getGracePeriodEndDate() == null && entry.getLicenseExpiryDate() != null) {
            entry.setGracePeriodEndDate(entry.getLicenseExpiryDate().plusDays(gracePeriodDays));
        }
        repository.save(entry);
    }
}