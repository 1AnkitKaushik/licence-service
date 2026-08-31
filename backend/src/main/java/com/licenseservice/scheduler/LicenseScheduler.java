package com.licenseservice.scheduler;

import com.licensemanager.service.LicenseEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LicenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(LicenseScheduler.class);

    private final LicenseEvaluationService licenseEvaluationService;

    public LicenseScheduler(LicenseEvaluationService licenseEvaluationService) {
        this.licenseEvaluationService = licenseEvaluationService;
    }

    @Scheduled(cron = "${registry.scheduler.cron:0 0 * * * *}")
    public void runScheduledSync() {
        log.info("Executing scheduled hourly license evaluation from MongoDB to Registry file...");
        licenseEvaluationService.evaluateAndSyncToRegistry();
    }
}