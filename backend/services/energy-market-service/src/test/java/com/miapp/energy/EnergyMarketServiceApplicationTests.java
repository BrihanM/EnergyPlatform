package com.miapp.energy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the infrastructure-only service starts without business data.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnergyMarketServiceApplicationTests {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
