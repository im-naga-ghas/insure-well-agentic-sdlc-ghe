package com.insurewell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * InsureWell Spring Boot Application
 * 
 * REST API for health insurance policy and claims management.
 * Listens on port 8080.
 */
@SpringBootApplication
@EnableScheduling
public class InsureWellApplication {

  public static void main(String[] args) {
    SpringApplication.run(InsureWellApplication.class, args);
  }

}
