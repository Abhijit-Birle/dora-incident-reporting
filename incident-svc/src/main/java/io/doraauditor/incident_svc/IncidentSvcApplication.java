package io.doraauditor.incident_svc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point for the DORA Incident Ingestion and Classification Service.
 */
@SpringBootApplication
public class IncidentSvcApplication {

    /**
     * Application bootstrapping method launching the Spring context.
     *
     * @param args command-line arguments passed during startup
     */
    public static void main(String[] args) {
        SpringApplication.run(IncidentSvcApplication.class, args);
    }

}
