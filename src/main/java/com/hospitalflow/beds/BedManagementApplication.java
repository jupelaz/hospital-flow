package com.hospitalflow.beds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BedManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(BedManagementApplication.class, args);
    }
}
