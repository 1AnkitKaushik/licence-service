package com.licensemanager.controller;

import com.licensemanager.dto.LicenseValidationRequest;
import com.licensemanager.dto.LicenseValidationResponse;
import com.licensemanager.model.LicenseValidationLog;
import com.licensemanager.service.LicenseFileService;
import com.licensemanager.service.LicenseValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/license")
public class LicenseValidationController {

    private final LicenseValidationService validationService;

    public LicenseValidationController(LicenseValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<LicenseValidationResponse> validateLicense(
            @Valid @RequestBody LicenseValidationRequest request,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        LicenseValidationResponse response = validationService.validateLicense(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-offline")
    public ResponseEntity<LicenseValidationResponse> validateLicenseOffline(
            @Valid @RequestBody LicenseValidationRequest request) {

        LicenseValidationResponse response = validationService.validateLicenseOffline(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/file/validate")
    public ResponseEntity<LicenseFileService.OfflineLicenseValidationResult> validateLicenseFile(
            @RequestBody String fileContent) {

        LicenseFileService.OfflineLicenseValidationResult result = validationService.validateLicenseFile(fileContent);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LicenseValidationLog>> getValidationLogs() {
        return ResponseEntity.ok(validationService.getValidationLogs());
    }

    @GetMapping("/logs/{licenseKey}")
    public ResponseEntity<List<LicenseValidationLog>> getLogsByLicense(@PathVariable String licenseKey) {
        return ResponseEntity.ok(validationService.getValidationLogsByLicense(licenseKey));
    }
}