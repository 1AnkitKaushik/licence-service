package com.licenseservice.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.licenseservice.model.ServiceRegistryEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FileRegistryRepository {

    private static final Logger log = LoggerFactory.getLogger(FileRegistryRepository.class);

    @Value("${registry.storage.file-path:data/registry/service_registry.json}")
    private String filePath;

    private final ObjectMapper objectMapper;
    private final Map<String, ServiceRegistryEntry> registryStore = new ConcurrentHashMap<>();

    public FileRegistryRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public synchronized void save(ServiceRegistryEntry entry) {
        registryStore.put(entry.getServiceId(), entry);
        saveToFile();
    }

    public synchronized void saveAll(Collection<ServiceRegistryEntry> entries) {
        registryStore.clear();
        for (ServiceRegistryEntry entry : entries) {
            registryStore.put(entry.getServiceId(), entry);
        }
        saveToFile();
    }

    public Optional<ServiceRegistryEntry> findById(String serviceId) {
        return Optional.ofNullable(registryStore.get(serviceId));
    }

    public List<ServiceRegistryEntry> findAll() {
        return new ArrayList<>(registryStore.values());
    }

    private void loadFromFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Registry file does not exist at {}. Creating directory structure...", filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            saveToFile();
            return;
        }

        try {
            List<ServiceRegistryEntry> entries = objectMapper.readValue(
                    file,
                    new TypeReference<List<ServiceRegistryEntry>>() {}
            );
            registryStore.clear();
            for (ServiceRegistryEntry entry : entries) {
                registryStore.put(entry.getServiceId(), entry);
            }
            log.info("Loaded {} service records from {}", registryStore.size(), filePath);
        } catch (IOException e) {
            log.error("Failed to load registry records from {}: {}", filePath, e.getMessage());
        }
    }

    private void saveToFile() {
        File file = new File(filePath);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, new ArrayList<>(registryStore.values()));
            log.debug("Saved {} records to {}", registryStore.size(), filePath);
        } catch (IOException e) {
            log.error("Failed to write service registry to {}: {}", filePath, e.getMessage());
        }
    }
}