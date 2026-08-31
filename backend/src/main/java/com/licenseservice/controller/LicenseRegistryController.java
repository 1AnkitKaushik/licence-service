package com.licenseservice.controller;

import com.licensemanager.service.LicenseEvaluationService;
import com.licenseservice.dto.K8sServiceActionResponse;
import com.licenseservice.model.ServiceRegistryEntry;
import com.licenseservice.service.LicenseRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/license/registry")
public class LicenseRegistryController {

    private static final Logger log = LoggerFactory.getLogger(LicenseRegistryController.class);

    private final LicenseRegistryService licenseRegistryService;
    private final LicenseEvaluationService licenseEvaluationService;

    public LicenseRegistryController(LicenseRegistryService licenseRegistryService, 
                                     LicenseEvaluationService licenseEvaluationService) {
        this.licenseRegistryService = licenseRegistryService;
        this.licenseEvaluationService = licenseEvaluationService;
    }

    @GetMapping
    public ResponseEntity<List<K8sServiceActionResponse>> getRegistryForKubernetes() {
        log.info("Kubernetes cluster controller requested service action registry.");
        return ResponseEntity.ok(licenseRegistryService.getK8sServiceActions());
    }

    @PostMapping("/sync")
    public ResponseEntity<List<ServiceRegistryEntry>> triggerSync() {
        log.info("Manual evaluation trigger requested via API endpoint.");
        return ResponseEntity.ok(licenseEvaluationService.evaluateAndSyncToRegistry());
    }
}