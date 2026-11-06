package com.miapp.energy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the energy market bounded context.
 *
 * <p>Business modules are intentionally added in later phases. This service
 * currently provides only infrastructure, migration ownership, and readiness
 * for authenticated Gateway traffic.</p>
 */
@SpringBootApplication
public class EnergyMarketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyMarketServiceApplication.class, args);
    }
}
