package com.licenseservice.service;

import com.licenseservice.dto.K8sServiceActionResponse;
import com.licenseservice.model.ServiceRegistryEntry;
import com.licenseservice.repository.FileRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LicenseRegistryService {

    private static final Logger log = LoggerFactory.getLogger(LicenseRegistryService.class);

    private final FileRegistryRepository fileRegistryRepository;

    public LicenseRegistryService(FileRegistryRepository fileRegistryRepository) {
        this.fileRegistryRepository = fileRegistryRepository;
    }

    /**
     * Updates the full registry table (called exclusively by LM).
     */
    public List<ServiceRegistryEntry> updateAll(List<ServiceRegistryEntry> entries) {
        fileRegistryRepository.saveAll(entries);
        log.info("LR: Successfully updated registry cache with {} entries.", entries.size());
        return entries;
    }

    /**
     * Upserts a single service entry in the registry.
     */
    public ServiceRegistryEntry saveOrUpdate(ServiceRegistryEntry entry) {
        List<ServiceRegistryEntry> entries = fileRegistryRepository.findAll();
        entries.removeIf(e -> e.getServiceId().equalsIgnoreCase(entry.getServiceId()));
        entries.add(entry);
        fileRegistryRepository.saveAll(entries);
        log.info("LR: Saved/Updated entry for serviceId: {}", entry.getServiceId());
        return entry;
    }

    /**
     * Retrieves actions formatted specifically for Kubernetes cluster controllers.
     */
    public List<K8sServiceActionResponse> getK8sServiceActions() {
        List<ServiceRegistryEntry> entries = fileRegistryRepository.findAll();
        return entries.stream()
                .map(e -> new K8sServiceActionResponse(
                        e.getServiceId(),
                        e.getServiceName(),
                        e.getLicenseState() != null ? e.getLicenseState().name() : "UNKNOWN",
                        e.getAction() != null ? e.getAction().name() : "NONE"
                ))
                .collect(Collectors.toList());
    }

    public List<ServiceRegistryEntry> getAllRegistryEntries() {
        return fileRegistryRepository.findAll();
    }

    public Optional<ServiceRegistryEntry> getEntryByServiceId(String serviceId) {
        return fileRegistryRepository.findAll().stream()
                .filter(e -> e.getServiceId().equalsIgnoreCase(serviceId))
                .findFirst();
    }
}