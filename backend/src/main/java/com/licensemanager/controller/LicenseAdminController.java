package com.licensemanager.controller;

import com.licensemanager.dto.LicenseRenewalRequest;
import com.licensemanager.dto.LicenseRequest;
import com.licensemanager.model.License;
import com.licensemanager.service.LicenseFileService;
import com.licensemanager.service.LicenseIssuingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/licenses")
public class LicenseAdminController {

    private final LicenseIssuingService issuingService;
    private final LicenseFileService fileService;

    public LicenseAdminController(LicenseIssuingService issuingService, LicenseFileService fileService) {
        this.issuingService = issuingService;
        this.fileService = fileService;
    }

    @PostMapping("/generate")
    public ResponseEntity<License> generateLicense(@Valid @RequestBody LicenseRequest request) {
        License license = issuingService.generateLicense(request);
        return ResponseEntity.ok(license);
    }

    @PostMapping("/renew")
    public ResponseEntity<License> renewLicense(@Valid @RequestBody LicenseRenewalRequest request) {
        License renewed = issuingService.renewLicense(request);
        return ResponseEntity.ok(renewed);
    }

    @PostMapping("/{licenseKey}/suspend")
    public ResponseEntity<License> suspendLicense(@PathVariable String licenseKey) {
        return ResponseEntity.ok(issuingService.suspendLicense(licenseKey));
    }

    @PostMapping("/{licenseKey}/activate")
    public ResponseEntity<License> activateLicense(@PathVariable String licenseKey) {
        return ResponseEntity.ok(issuingService.activateLicense(licenseKey));
    }

    @PostMapping("/{licenseKey}/revoke")
    public ResponseEntity<License> revokeLicense(@PathVariable String licenseKey) {
        return ResponseEntity.ok(issuingService.revokeLicense(licenseKey));
    }

    @DeleteMapping("/{licenseKey}")
    public ResponseEntity<Void> deleteLicense(@PathVariable String licenseKey) {
        issuingService.deleteLicense(licenseKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{licenseKey}")
    public ResponseEntity<License> getLicenseByKey(@PathVariable String licenseKey) {
        return issuingService.getLicenseByKey(licenseKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<License>> getAllLicenses() {
        return ResponseEntity.ok(issuingService.getAllLicenses());
    }

    @GetMapping("/{licenseKey}/export-file")
    public ResponseEntity<String> exportLicenseFile(@PathVariable String licenseKey) {
        License license = issuingService.getLicenseByKey(licenseKey)
                .orElseThrow(() -> new RuntimeException("License not found with key: " + licenseKey));

        String fileContent = fileService.generateLicenseFile(license);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"license.lic\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fileContent);
    }
}