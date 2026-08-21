package com.licensemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.licensemanager", "com.licenseservice"})
@EnableMongoRepositories(basePackages = {"com.licensemanager.repository"})
public class LicenseManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseManagementApplication.class, args);
    }
}