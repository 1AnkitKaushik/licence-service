package com.licenseservice.scheduler;

import com.licenseservice.service.LicenseRegistryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LicenseScheduler {

    private final LicenseRegistryService registryService;

    public LicenseScheduler(LicenseRegistryService registryService) {
        this.registryService = registryService;
    }

    @Scheduled(cron = "${registry.scheduler.cron:0 0 * * * *}")
    public void runHourlyCheck() {
        registryService.evaluateAllLicenses();
    }
}