package com.licensemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.licensemanager", "com.licenseservice"})
@EnableScheduling
public class LicenseManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LicenseManagementApplication.class, args);
    }
}