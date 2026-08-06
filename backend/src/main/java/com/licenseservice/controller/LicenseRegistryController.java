package com.licenseservice.controller;

import com.licenseservice.model.K8sServiceActionResponse;
import com.licenseservice.model.ServiceRegistryEntry;
import com.licenseservice.service.LicenseRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/license")
public class LicenseRegistryController {

    private final LicenseRegistryService registryService;

    public LicenseRegistryController(LicenseRegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping("/registry")
    public ResponseEntity<List<K8sServiceActionResponse>> getServiceRegistryForK8s() {
        return ResponseEntity.ok(registryService.getK8sActionList());
    }

    @PostMapping("/registry/register")
    public ResponseEntity<String> registerService(@RequestBody ServiceRegistryEntry entry) {
        registryService.registerService(entry);
        return ResponseEntity.ok("Service " + entry.getServiceId() + " registered successfully.");
    }

    @PostMapping("/registry/evaluate")
    public ResponseEntity<String> triggerEvaluation() {
        registryService.evaluateAllLicenses();
        return ResponseEntity.ok("License evaluation triggered manually.");
    }
}