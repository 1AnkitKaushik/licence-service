package com.licenseservice.repository;

import com.licenseservice.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FileRegistryRepository {

    private final String filePath;
    private final ObjectMapper objectMapper;
    private final Map<String, ServiceRegistryEntry> registryStore = new ConcurrentHashMap<>();

    public FileRegistryRepository(@Value("${registry.storage.file-path:data/registry/service_registry.json}") String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        initStorage();
    }

    private void initStorage() {
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            seedDefaultData();
            saveToFile();
        } else {
            loadFromFile();
        }
    }

    private void seedDefaultData() {
        List<ServiceRegistryEntry> defaults = List.of(
            new ServiceRegistryEntry("svc-001", "as1", "STD", ServiceState.RUNNING, LicenseState.ACTIVE, ActionRequired.NONE, LocalDate.now().plusDays(30)),
            new ServiceRegistryEntry("svc-002", "as2", "PRO", ServiceState.RUNNING, LicenseState.GRACE_PERIOD, ActionRequired.WARN, LocalDate.now().minusDays(5)),
            new ServiceRegistryEntry("svc-003", "IS1", "INFRA", ServiceState.SHUTDOWN, LicenseState.EXPIRED, ActionRequired.SHUTDOWN, LocalDate.now().minusDays(20))
        );
        defaults.forEach(e -> registryStore.put(e.getServiceId(), e));
    }

    public synchronized void saveToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), registryStore.values());
        } catch (IOException e) {
            System.err.println("❌ Failed to save registry to file: " + e.getMessage());
        }
    }

    public synchronized void loadFromFile() {
        try {
            File file = new File(filePath);
            List<ServiceRegistryEntry> entries = objectMapper.readValue(file, new TypeReference<>() {});
            registryStore.clear();
            entries.forEach(e -> registryStore.put(e.getServiceId(), e));
        } catch (IOException e) {
            System.err.println("❌ Failed to load registry from file: " + e.getMessage());
        }
    }

    public List<ServiceRegistryEntry> findAll() {
        return new ArrayList<>(registryStore.values());
    }

    public Optional<ServiceRegistryEntry> findById(String serviceId) {
        return Optional.ofNullable(registryStore.get(serviceId));
    }

    public void save(ServiceRegistryEntry entry) {
        registryStore.put(entry.getServiceId(), entry);
        saveToFile();
    }
}